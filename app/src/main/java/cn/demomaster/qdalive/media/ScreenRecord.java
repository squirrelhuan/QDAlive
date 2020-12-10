package cn.demomaster.qdalive.media;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static android.media.MediaCodec.CONFIGURE_FLAG_ENCODE;
import static android.media.MediaFormat.KEY_BIT_RATE;
import static android.media.MediaFormat.KEY_FRAME_RATE;
import static android.media.MediaFormat.KEY_I_FRAME_INTERVAL;

/**
 *
 * 录屏输出h264字节流
 * Created by LQD ON 2018/5/29 0029
 */
public class ScreenRecord {

    private static final String TAG = ScreenRecord.class.getSimpleName();

    public interface ScreenCaputreListener {
        void onImageData(byte[] buf);
    }
    private ScreenCaputreListener screenCaputreListener;

    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;

    private int width;
    private int height;

    public ScreenRecord(int width, int height, MediaProjection mMediaProjection) {
        this.width = 720;
        this.height = 1280;
        this.mMediaProjection = mMediaProjection;

    }

    public void setScreenCaputreListener(ScreenCaputreListener screenCaputreListener) {
        this.screenCaputreListener = screenCaputreListener;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void start() {
        try {
            prepareVideoEncoder();
            startVideoEncode();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void stop() {
        videoEncoderLoop = false;
        if (null != vEncoder) {
            vEncoder.stop();
        }
        if(mVirtualDisplay != null) mVirtualDisplay.release();
        if(mMediaProjection!= null) mMediaProjection.stop();

    }
    public static final int NAL_SLICE = 1;
    public static final int NAL_SLICE_DPA = 2;
    public static final int NAL_SLICE_DPB = 3;
    public static final int NAL_SLICE_DPC = 4;
    public static final int NAL_SLICE_IDR = 5;
    public static final int NAL_SEI = 6;
    public static final int NAL_SPS = 7;
    public static final int NAL_PPS = 8;
    public static final int NAL_AUD = 9;
    public static final int NAL_FILLER = 12;

    private MediaCodec.BufferInfo vBufferInfo = new MediaCodec.BufferInfo();
    private MediaCodec vEncoder;
    private Thread videoEncoderThread;

    private boolean videoEncoderLoop;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void prepareVideoEncoder() throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(KEY_BIT_RATE, width * height);
        format.setInteger(KEY_FRAME_RATE, 15);//帧率
        format.setInteger(KEY_I_FRAME_INTERVAL, 1);
        MediaCodec vencoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        vencoder.configure(format, null, null, CONFIGURE_FLAG_ENCODE);
        Surface surface = vencoder.createInputSurface();
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("-display", width, height, 1,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null);
        vEncoder = vencoder;
    }

    public void startVideoEncode() {
        if (vEncoder == null) {
            throw new RuntimeException("请初始化视频编码器");
        }
        if (videoEncoderLoop) {
            throw new RuntimeException("必须先停止");
        }
        videoEncoderThread = new Thread() {
            @Override
            public void run() {
                vEncoder.start();
                while (videoEncoderLoop && !Thread.interrupted()) {
                    try {
                        ByteBuffer[] outputBuffers = vEncoder.getOutputBuffers();
                        int outputBufferId = vEncoder.dequeueOutputBuffer(vBufferInfo, 0);
                        if (outputBufferId >= 0) {
                            ByteBuffer bb = outputBuffers[outputBufferId];
                            onEncodedAvcFrame(bb, vBufferInfo);
                            vEncoder.releaseOutputBuffer(outputBufferId, false);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        };
        videoEncoderLoop = true;
        videoEncoderThread.start();
    }

    private byte[] sps_pps_buf;
    private void onEncodedAvcFrame(ByteBuffer bb, final MediaCodec.BufferInfo vBufferInfo) {
        int offset = 4;
        //判断帧的类型
        if (bb.get(2) == 0x01) {
            offset = 3;
        }
        int type = bb.get(offset) & 0x1f;
        if (type == NAL_SPS) {
            //[0, 0, 0, 1, 103, 66, -64, 13, -38, 5, -126, 90, 1, -31, 16, -115, 64, 0, 0, 0, 1, 104, -50, 6, -30]
            //打印发现这里将 SPS帧和 PPS帧合在了一起发送
            // SPS为 [4，len-8]
            // PPS为后4个字节

            sps_pps_buf = new byte[vBufferInfo.size];
            bb.get(sps_pps_buf);
            //TODO
            /*
            final byte[] pps = new byte[4];
            final byte[] sps = new byte[vBufferInfo.size - 12];
            bb.getInt();// 抛弃 0,0,0,1
            bb.get(sps, 0, sps.length);
            bb.getInt();
            bb.get(pps, 0, pps.length);
            Log.d(TAG, "解析得到 sps:" + Arrays.toString(sps) + ",PPS=" + Arrays.toString(pps));
            */
        } else if (type == NAL_SLICE  /* || type == NAL_SLICE_IDR */) {
            final byte[] bytes = new byte[vBufferInfo.size];
            bb.get(bytes);
            if (null != screenCaputreListener) {
                screenCaputreListener.onImageData(bytes);
            }
           // Log.v(TAG, "视频数据  " + Arrays.toString(bytes));

        } else if (type == NAL_SLICE_IDR) {
            // I帧，前面添加sps和pps
            final byte[] bytes = new byte[vBufferInfo.size];
            bb.get(bytes);

            byte[] newBuf = new byte[sps_pps_buf.length + bytes.length];
            System.arraycopy(sps_pps_buf, 0, newBuf, 0, sps_pps_buf.length);
            System.arraycopy(bytes, 0, newBuf, sps_pps_buf.length, bytes.length);
            if (null != screenCaputreListener) {
                screenCaputreListener.onImageData(newBuf);
            }
            Log.v(TAG, "sps pps  " + Arrays.toString(sps_pps_buf));
           // Log.v(TAG, "视频数据  " + Arrays.toString(newBuf));
        }
    }
}
