package cn.demomaster.qdalive.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Base64;

import cn.demomaster.huan.quickdeveloplibrary.helper.QdThreadHelper;
import cn.demomaster.huan.quickdeveloplibrary.helper.toast.QdToast;
import cn.demomaster.huan.quickdeveloplibrary.util.QDBitmapUtil;
import cn.demomaster.qdalive.MyApp;
import cn.demomaster.qdalive.MyService;
import cn.demomaster.qdlogger_library.QDLogger;
import core.mqtt.OnMessageReceiveListener;

public class CaptureUtil {
    private static CaptureUtil instance;

    public static CaptureUtil getInstance() {
        if(instance==null){
            instance = new CaptureUtil();
        }
        return instance;
    }

    private CaptureUtil() {
    }

    Context mContext;
    public void init(Context context){
        mContext = context;
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 2);
        }
    }
    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;

    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;
    private MediaProjection mMediaProjection;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void startScreenShot() {
        virtualDisplay();
        startCapture();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private MediaProjectionManager getMediaProjectionManager() {
        return (MediaProjectionManager) mContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void stopVirtual() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
        if(mediaRecord!=null)
        mediaRecord.release();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void virtualDisplay() {
        if(mMediaProjection==null){
            return;
        }
        if(mVirtualDisplay==null) {
            mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                    mScreenWidth, mScreenHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mImageReader.getSurface(), null, null);
        }
    }

    long t1 ;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startCapture() {
        Image image = mImageReader.acquireNextImage();
        if (image == null) {
            QDLogger.i("startCapture...");
            startScreenShot();
        } else {
            t1=System.currentTimeMillis();
            SaveTask mSaveTask = new SaveTask();
            mSaveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, image);

            /*long t1 = System.currentTimeMillis();
            int width = image.getWidth();
            int height = image.getHeight();
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            Bitmap bitmap = Bitmap.createBitmap(width , height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            //bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);

            *//*byte[] imageBytes= new byte[buffer.remaining()];
            buffer.get(imageBytes);
            final Bitmap bitmap= BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.length);
*//*
            ((MyApp) mContext.getApplicationContext()).setBitmap(bitmap);
            sendBroadCast2(bitmap);
            image.close();
            long t2 = System.currentTimeMillis()-t1;
            QDLogger.i("用时："+t2);*/
        }
    }
    MediaRecordService mediaRecord;
    ScreenCaputre screenCaputre;
    ScreenCaputre.ScreenCaputreListener screenCaputreListener;

    public void setScreenCaputreListener(ScreenCaputre.ScreenCaputreListener screenCaputreListener) {
        this.screenCaputreListener = screenCaputreListener;
        if(screenCaputre!=null)
        screenCaputre.setScreenCaputreListener(screenCaputreListener);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onActivityResult(Intent data) {
        mMediaProjection = getMediaProjectionManager().getMediaProjection(Activity.RESULT_OK, data);
        File file = new File(Environment.getExternalStorageDirectory(),"qdalive.mp4");  //录屏生成mp4文件
        if(mMediaProjection!=null){
            /*mediaRecord = new MediaRecordService(mScreenWidth, mScreenHeight, 6000000, 1,
                    mMediaProjection, file.getAbsolutePath());
            mediaRecord.start();*/
           /* file = new File(Environment.getExternalStorageDirectory(),"qdalive2.mp4");  //录屏生成文件
            ScreenRecordService screenRecordService = new ScreenRecordService(mScreenWidth,mScreenHeight,6000000,1,mMediaProjection,file.getAbsolutePath());
            screenRecordService.start();*/

            screenCaputre = new ScreenCaputre(mScreenWidth, mScreenHeight, mMediaProjection);
            screenCaputre.setScreenCaputreListener(screenCaputreListener);
            screenCaputre.start();
        }
    }

    public void release() {
        if(mMediaProjection!=null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mMediaProjection.stop();
            }
        }
    }

    public class SaveTask extends AsyncTask<Image, Void, Bitmap> {

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        protected Bitmap doInBackground(Image... params) {
            if (params == null || params.length < 1 || params[0] == null) {
                return null;
            }
            QDLogger.i("用时1："+(System.currentTimeMillis()-t1));
            Image image = params[0];
            QDLogger.i("用时2："+(System.currentTimeMillis()-t1));

            int width = image.getWidth();
            int height = image.getHeight();
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            /*//每个像素的间距
            int pixelStride = planes[0].getPixelStride();
            //总的间距
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;
            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);*/
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
            bitmap.copyPixelsFromBuffer(buffer);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);

            Log.d("cgq", "bitmap 处理前大小："+QDBitmapUtil.getBitmapSize(bitmap));
            bitmap = QDBitmapUtil.zoomImage(bitmap,bitmap.getWidth()/2,bitmap.getHeight()/2);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);
            byte[] data = baos.toByteArray();
            Log.d("cgq", "bitmap 处理后大小："+data.length);

            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

            Log.d("cgq", "bitmap 处理后大小："+QDBitmapUtil.getBitmapSize(bitmap));
            long tt1 = System.currentTimeMillis();
            //updateBitmap(bitmap);
            //bitmap = getBitmap(bitmap);
            Log.d("cgq", "bitmap 处理time："+(System.currentTimeMillis()-tt1)+",size"+QDBitmapUtil.getBitmapSize(bitmap));
            ((MyApp) mContext.getApplicationContext()).setBitmap(bitmap);
            sendBroadCast2(bitmap);
            image.close();
            QDLogger.i("用时3："+(System.currentTimeMillis()-t1));

            /*File fileImage = null;
            if (bitmap != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        fileImage = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_SCREENSHOTS),FileUtil.getScreenShotsName(mContext));
                    }else {
                        fileImage = new File(FileUtil.getScreenShotsName(mContext));
                    }
                    if (!fileImage.exists()) {
                        QDFileUtil.createFile(fileImage);
                    }
                    FileOutputStream out = new FileOutputStream(fileImage);
                    if (out != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        out.flush();
                        out.close();
                       *//* Intent media = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        Uri contentUri = Uri.fromFile(fileImage);
                        media.setData(contentUri);
                        mContext.sendBroadcast(media);*//*
                       sendBroadCast(fileImage.getAbsolutePath());

                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    fileImage = null;
                } catch (IOException e) {
                    e.printStackTrace();
                    fileImage = null;
                }
            }

            if (fileImage != null) {
                return bitmap;
            }*/
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            //预览图片
            if (bitmap != null) {
                Log.e("ryze", "获取图片成功");
            }

        }
    }

    private void updateBitmap(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 10, baos);
        byte[] data = baos.toByteArray();
        String fileBuf = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            fileBuf = Base64.getEncoder().encodeToString(data);
        }
        //fileBuf = android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT);
        QDLogger.i("fileBuf="+fileBuf);
     /*TODO   MyService.mqttClient.uploadFile(MyService.selectDevice,System.currentTimeMillis()+"", fileBuf, new OnMessageReceiveListener() {
            @Override
            public void onReceived(byte[] bytes) {
                QDLogger.i("文件发送完成");
                *//*((MyApp)mContext.getApplicationContext()).handler.removeCallbacks(runnable);
                ((MyApp)mContext.getApplicationContext()).handler.post(runnable);*//*
            }
        });

        ((MyApp)mContext.getApplicationContext()).handler.postDelayed(runnable,3000);*/
    }

    Runnable runnable =new Runnable() {
        @Override
        public void run() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Log.d("cgq", "startScreenShot");
                startScreenShot();
            }
        }
    };

    private void sendBroadCast(String filePath) {
        Intent intent = new Intent("cn.demomaster.qdalive.receiver.MyBroadcastReceiver");
        /**注意：7.0的教程里没有这一句，高版本要加上，否则接收不到广播*/
        if(Build.VERSION.SDK_INT >= 26) {
           // intent.setComponent(new ComponentName("cn.demomaster.qdalive.receiver",
             //       "cn.demomaster.qdalive.receiver.MyBroadcastReceiver"));
        }
        Bundle bundle = new Bundle();
        bundle.putString("screenshoot",filePath);
        intent.putExtras(bundle);
        mContext.sendBroadcast(intent);
        Log.d("cgq", "sendBroadCast!!");
    }
    private void sendBroadCast2(Bitmap bitmap) {
        Intent intent = new Intent("cn.demomaster.qdalive.receiver.MyBroadcastReceiver");
        /**注意：7.0的教程里没有这一句，高版本要加上，否则接收不到广播*/
        if(Build.VERSION.SDK_INT >= 26) {
            // intent.setComponent(new ComponentName("cn.demomaster.qdalive.receiver",
            //       "cn.demomaster.qdalive.receiver.MyBroadcastReceiver"));
        }
        Bundle bundle = new Bundle();
        intent.putExtras(bundle);
        mContext.sendBroadcast(intent);

        int size = QDBitmapUtil.getBitmapSize(bitmap);
        Log.d("cgq", "bitmap 压缩后大小："+size);
        //Log.d("cgq", "bitmap 压缩后大小："+size);

       /* int bytes = bitmap.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes);
        bitmap.copyPixelsToBuffer(buffer); //Move the byte data to the buffer
        byte[] data = buffer.array(); //Get the bytes array of the bitmap*/

    }

    Bitmap bitmap_last;
    public Bitmap getBitmap(Bitmap bitmap){
        if(bitmap_last==null){
            bitmap_last = bitmap;
            return bitmap;
        }
        int width = bitmap_last.getWidth();
        int height = bitmap_last.getHeight();
        Bitmap bitmap_new = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_4444);
        for (int i = 0; i < width; ++i) {
            for (int j = 0; j < height; ++j) {
                if (bitmap_last.getPixel(i, j) != bitmap.getPixel(i, j)){
                    int color = bitmap.getPixel(i, j);
                    bitmap_new.setPixel(i,j,color);
                }else {
                    int color = Color.TRANSPARENT;
                    bitmap_new.setPixel(i,j,color);
                }
            }
        }
        return bitmap_new;
    }
}
