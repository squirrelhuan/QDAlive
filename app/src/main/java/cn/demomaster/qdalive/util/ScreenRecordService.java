package cn.demomaster.qdalive.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import cn.demomaster.huan.quickdeveloplibrary.util.QDFileUtil;

public class ScreenRecordService extends Thread {

    private static final String TAG = "ScreenRecordService";

    private int mWidth;
    private int mHeight;
    private int mBitRate;
    private int mDpi;
    private String mDstPath;
    private MediaProjection mMediaProjection;
    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced
    // Video Coding
    private static final int FRAME_RATE = 30; // 30 fps
    private static final int IFRAME_INTERVAL = 10; // 10 seconds between
    // I-frames
    private static final int TIMEOUT_US = 10000;

    private MediaCodec mediaCodec;
    private MediaMuxer mediaMuxer;
    private boolean mMuxerStarted = false;
    private int mVideoTrackIndex = -1;
    private AtomicBoolean mQuit = new AtomicBoolean(false);
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private VirtualDisplay mVirtualDisplay;

    public ScreenRecordService(int width, int height, int bitrate, int dpi, MediaProjection mp, String dstPath) {
        super(TAG);
        mWidth = width;
        mHeight = height;
        mBitRate = bitrate;
        mDpi = dpi;
        mMediaProjection = mp;
        mDstPath = dstPath;
    }

    /**
     * stop task
     */
    public final void quit() {
        mQuit.set(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void run() {
        try {
            try {
                prepareEncoder();
                mediaMuxer = new MediaMuxer(mDstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG + "-display", mWidth, mHeight, mDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mSurface, null, null);
            Log.d(TAG, "created virtual display: " + mVirtualDisplay);
            recordVirtualDisplay();
        } finally {
            release();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void recordVirtualDisplay() {
        while (!mQuit.get()) {
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
            //Log.i(TAG, "dequeue output buffer index=" + index);
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 后续输出格式变化
                Log.i(TAG, "后续输出格式变化 index=" + outputBufferIndex);
                resetOutputFormat();
            } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // 请求超时
//          Log.d(TAG, "retrieving buffers time out!");
                try {
                    // wait 10ms
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            } else if (outputBufferIndex >= 0) {
                // 有效输出
                if (!mMuxerStarted) {
                    throw new IllegalStateException("MediaMuxer dose not call addTrack(format) ");
                }
                encodeToVideoTrack(outputBufferIndex);

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            }
        }
    }

    /**
     * 硬解码获取实时帧数据并写入mp4文件
     *
     * @param outputBufferIndex
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void encodeToVideoTrack(int outputBufferIndex) {
        Log.d(TAG, "encodeToVideoTrack=" + outputBufferIndex);
        // 获取到的实时帧视频数据
        ByteBuffer encodedData = mediaCodec.getOutputBuffer(outputBufferIndex);

        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // The codec config data was pulled out and fed to the muxer
            // when we got
            // the INFO_OUTPUT_FORMAT_CHANGED status.
            // Ignore it.
            Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
            mBufferInfo.size = 0;
        }
        if (mBufferInfo.size == 0) {
            Log.d(TAG, "info.size == 0, drop it.");
            encodedData = null;
        } else {
//      Log.d(TAG, "got buffer, info: size=" + mBufferInfo.size + ", presentationTimeUs="
//          + mBufferInfo.presentationTimeUs + ", offset=" + mBufferInfo.offset);
        }
        if (encodedData != null) {
            mediaMuxer.writeSampleData(mVideoTrackIndex, encodedData, mBufferInfo);

           /* if (mBufferInfo.size != 0) {
                encodedData.position(mBufferInfo.offset);
                encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                try {
                    byte[] bytes = new byte[encodedData.remaining()];
                    encodedData.get(bytes);

                    File file = new File(Environment.getExternalStorageDirectory(),"test.h264");
                    QDFileUtil.writeFile(file,bytes,true);
                    Log.d(TAG, "bytes=" + Arrays.asList(bytes));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }*/


        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void resetOutputFormat() {
        // should happen before receiving buffers, and should only happen
        // once
        if (mMuxerStarted) {
            throw new IllegalStateException("output format already changed!");
        }
        MediaFormat newFormat = mediaCodec.getOutputFormat();
        mVideoTrackIndex = mediaMuxer.addTrack(newFormat);
        mediaMuxer.start();
        mMuxerStarted = true;
        Log.i(TAG, "started media muxer, videoIndex=" + mVideoTrackIndex);
    }

    Surface mSurface;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void prepareEncoder() throws IOException {

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

        Log.d(TAG, "created video format: " + format);
        mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mSurface = mediaCodec.createInputSurface();
        Log.d(TAG, "created input surface: " + mSurface);
        mediaCodec.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void release() {
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
        }
        if (mediaMuxer != null) {
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
        }
    }

}
