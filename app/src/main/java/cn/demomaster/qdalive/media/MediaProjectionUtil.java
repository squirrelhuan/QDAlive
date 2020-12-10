package cn.demomaster.qdalive.media;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;

import cn.demomaster.huan.quickdeveloplibrary.helper.toast.QdToast;
import cn.demomaster.qdalive.util.ScreenCaputre;

public class MediaProjectionUtil {

    private static MediaProjectionUtil instance;

    public static MediaProjectionUtil getInstance() {
        if (instance == null) {
            instance = new MediaProjectionUtil();
        }
        return instance;
    }

    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;
    private MediaProjectionUtil() {

    }

    public final int REQUEST_MEDIA_PROJECTION = 141315;
    private static Context mContext;
    public void startMediaIntent(Activity context) {
        if(hasOpen()){
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mContext = context.getApplicationContext();
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            mWindowManager.getDefaultDisplay().getMetrics(metrics);
            mScreenDensity = metrics.densityDpi;
            mScreenWidth = metrics.widthPixels;
            mScreenHeight = metrics.heightPixels;
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent intent = mediaProjectionManager.createScreenCaptureIntent();
            context.startActivityForResult(intent, REQUEST_MEDIA_PROJECTION);
        } else {
            QdToast.show("对不起，只支持Android 5.0以上设备");
        }
    }

    public boolean hasOpen(){
        return (mMediaProjection!=null);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private MediaProjectionManager getMediaProjectionManager() {
        return (MediaProjectionManager) mContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    ScreenRecord.ScreenCaputreListener screenCaputreListener;
    public void setScreenCaputreListener(ScreenRecord.ScreenCaputreListener screenCaputreListener) {
        this.screenCaputreListener = screenCaputreListener;
        if(mScreenRecord!=null) {
            mScreenRecord.setScreenCaputreListener(screenCaputreListener);
        }
    }
    ScreenRecord mScreenRecord;
    private MediaProjection mMediaProjection;
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            switch (requestCode) {
                case REQUEST_MEDIA_PROJECTION:
                    mMediaProjection = getMediaProjectionManager().getMediaProjection(Activity.RESULT_OK, data);

                    mScreenRecord = new ScreenRecord(mScreenWidth, mScreenHeight, mMediaProjection);
                    mScreenRecord.setScreenCaputreListener(screenCaputreListener);
                    mScreenRecord.start();
                    break;
            }
        }
    }
}
