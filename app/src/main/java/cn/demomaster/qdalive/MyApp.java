package cn.demomaster.qdalive;

import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;

import java.io.File;

import cn.demomaster.huan.quickdeveloplibrary.QDApplication;
import cn.demomaster.huan.quickdeveloplibrary.constant.AppConfig;
import cn.demomaster.huan.quickdeveloplibrary.helper.NotificationHelper;
import cn.demomaster.qdalive.util.GestureHelper;
import cn.demomaster.qdlogger_library.QDLogger;
import cn.demomaster.qdlogger_library.config.ConfigBuilder;

public class MyApp extends QDApplication {

    public Bitmap bitmap;
    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ConfigBuilder configBuilder = new ConfigBuilder(this);
        String savePath = (String) AppConfig.getInstance().getConfigMap(getApplicationContext()).get("LogFilePath");
        configBuilder.setSaveInternalSoragePath("/qdalive/log/");
        configBuilder.setSaveExternalStorageBeforeAndroidQ(true);
        configBuilder.setSaveExternalStoragePath(new File(Environment.getExternalStorageDirectory(),savePath));
        QDLogger.init(this,configBuilder.build());
        GestureHelper.init(getApplicationContext());
       //NotificationHelper.getInstance().init(this);
    }
}
