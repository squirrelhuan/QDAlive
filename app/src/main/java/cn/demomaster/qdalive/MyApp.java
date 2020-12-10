package cn.demomaster.qdalive;

import android.graphics.Bitmap;
import android.os.Handler;

import cn.demomaster.huan.quickdeveloplibrary.QDApplication;
import cn.demomaster.huan.quickdeveloplibrary.helper.NotificationHelper;
import cn.demomaster.qdalive.util.GestureHelper;
import cn.demomaster.qdlogger_library.QDLogger;

public class MyApp extends QDApplication {

    public Bitmap bitmap;
    public Handler handler = new Handler();
    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        QDLogger.init(this,"/qdalive/log/");
        GestureHelper.init();
        NotificationHelper.getInstance().init(this);
    }
}
