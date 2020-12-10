package cn.demomaster.qdalive;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.alibaba.fastjson.JSON;

import cn.demomaster.huan.quickdeveloplibrary.helper.toast.QdToast;
import cn.demomaster.huan.quickdeveloplibrary.service.QDAccessibilityService;
import cn.demomaster.huan.quickdeveloplibrary.util.QDAndroidDeviceUtil;
import cn.demomaster.qdalive.media.MediaProjectionUtil;
import cn.demomaster.qdalive.media.ScreenRecord;
import cn.demomaster.qdalive.model.ActionModel;
import cn.demomaster.qdalive.model.ActionTypeEmun;
import cn.demomaster.qdalive.util.CaptureUtil;
import cn.demomaster.qdalive.util.GestureHelper;
import cn.demomaster.qdlogger_library.QDLogger;
import core.MqttClient;
import core.MqttConnectionListener;
import core.MqttException;
import core.model.ConnectInfo;
import core.model.Msg;

public class MyService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static MqttClient mqttClient;
    static String[] devices;
    public static String serverIp;
    static MqttActionListener mListener;
    public static void setListener(MqttActionListener listener) {
        mListener = listener;
    }

    public static Handler handler;

    public void startScreenShot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CaptureUtil.getInstance().startScreenShot();
        }
    }

    public void reConnect() {
        if (mqttClient != null) {
            mqttClient.setServerIp(serverIp);
            mqttClient.reConnect();
        }
    }

    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        QDLogger.i("service onStartCommand");
        int flags1 = Service.START_STICKY;
        return super.onStartCommand(intent, flags1, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        QDLogger.i("service onCreate");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
        GestureHelper.context = getApplicationContext();
        handler = new Handler() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 456:
                        Intent intent = (Intent) msg.obj;
                        //setmResultData(intent);
                        break;
                    case 789:
                        //Activity activity = (Activity) msg.obj;
                        startScreenShot();
                        break;
                    case 790:
                        //Activity activity = (Activity) msg.obj;
                        reConnect();
                        break;
                    default:
                        break;
                }
            }
        };

        //QdToast.show("Service onCreate");
        String id = QDAndroidDeviceUtil.getUniqueID(this);
        //String ip = "192.168.199.107";
        //ip = "192.168.0.106";
        final int port = 10101;
        if (mqttClient == null) {
            mqttClient = new MqttClient(serverIp, port);
            ConnectInfo connectInfo = new ConnectInfo();
            connectInfo.setClientId("" + id);
            connectInfo.setUserName("u2");
            connectInfo.setPassWord("p2");
            connectInfo.setKeepAliveTime(30000);
            mqttClient.setConnectOption(connectInfo);
            mqttClient.connect(serverIp, port, new MqttConnectionListener() {
                @Override
                public void onMqttConnect() {
                    QdToast.show("连接成功");
                    //System.out.println("Mqtt连接成功");
                    if (mListener != null) {
                        mListener.onMqttConnect();
                    }
                }

                @Override
                public void onReceived(byte[] bytes) {
                    if (mListener != null) {
                        mListener.onReceived(bytes);
                    }
                }

                @Override
                public void onRequest(Msg msgObj) {
                    try {
                        //QDLogger.i("onRequest type="+msgObj.getType());
                        if (msgObj != null && msgObj.getType() == 1) {
                            ActionModel actionModel = JSON.parseObject(new String(msgObj.getData()), ActionModel.class);
                            //QDLogger.i("actionModel.getActionType()="+actionModel.getActionType());
                            if (actionModel.getActionType() == ActionTypeEmun.control.value()) {
                                //对方发送远程控制请求
                                if (mListener != null) {
                                    mListener.onRequestControl(msgObj);
                                    return;
                                }
                            }else if (actionModel.getActionType() == ActionTypeEmun.controled.value()) {
                                //对方发送远程被控制请求
                                if (mListener != null) {
                                    mListener.onRequestControled(msgObj.getClientId());
                                    return;
                                }
                               // mqttClient.response(msgObj.getClientId(),null,msgObj.getMsgId(),"yes",(byte) msgObj.getType(),null);
                            }else if (actionModel.getActionType() == ActionTypeEmun.home.value()) {
                                //对方发送远程被控制请求
                                Message message = Message.obtain();
                                message.what = actionModel.getActionType();
                                GestureHelper.handler.sendMessage(message);
                                return;
                            }else if (actionModel.getActionType() == ActionTypeEmun.task.value()) {
                                //对方发送远程被控制请求
                                Message message = Message.obtain();
                                message.what = actionModel.getActionType();
                                GestureHelper.handler.sendMessage(message);
                                return;
                            }else if (actionModel.getActionType() == ActionTypeEmun.back.value()) {
                                //对方发送远程被控制  返回 请求
                                Message message = Message.obtain();
                                message.what = actionModel.getActionType();
                                GestureHelper.handler.sendMessage(message);
                                return;
                            } else if (actionModel.getActionType() == ActionTypeEmun.move.value()) {
                                Message message = Message.obtain();
                                message.what = actionModel.getActionType();
                                message.obj = actionModel;
                                GestureHelper.handler.sendMessage(message);
                                return;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (mListener != null) {
                        mListener.onRequest(msgObj);
                    }
                    // mqttClient.response(msgObj.getHeader().getSn(),msgObj.getHeader().getMsgId(),"我明天有事，来不了".getBytes(),null);
                }

                @Override
                public void onDisConnect(MqttException e) {
                    QdToast.show("连接断开");
                    e.printStackTrace();
                    mqttClient.connect();
                    if (mListener != null) {
                        mListener.onDisConnect(e);
                    }
                }
            });
        }

       /* CaptureUtil.getInstance().setScreenCaputreListener(new ScreenCaputre.ScreenCaputreListener() {
            @Override
            public void onImageData(byte[] buf) {
                try {
                    QDLogger.i("ScreenCaputre  "+buf);
                    mqttClient.request(MyService.selectDevice,System.currentTimeMillis()+"",buf,(byte) 2,null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });*/
    }

    private static String targetDevice = null;
    public static void setTargetDevice(String targetDevice) {
        MyService.targetDevice = targetDevice;
        if (!TextUtils.isEmpty(targetDevice)) {
            MediaProjectionUtil.getInstance().setScreenCaputreListener(new ScreenRecord.ScreenCaputreListener() {
                @Override
                public void onImageData(byte[] buf) {
                    try {
                        //QDLogger.i("ScreenCaputre  " + buf);
                        MyService.mqttClient.request(targetDevice, System.currentTimeMillis() + "", buf, (byte) 2, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            MediaProjectionUtil.getInstance().setScreenCaputreListener(null);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // 通知渠道的id
        String id = "my_channel_01";
        // 用户可以看到的通知渠道的名字.
        CharSequence name = "channel_name";//channel_name
//         用户可以看到的通知渠道的描述
        String description = "using";//channel_description
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
//         配置通知渠道的属性
        mChannel.setDescription(description);
//         设置通知出现时的闪灯（如果 android 设备支持的话）
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.RED);
//         设置通知出现时的震动（如果 android 设备支持的话）
        mChannel.enableVibration(true);
        mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
//         最后在notificationmanager中创建该通知渠道 //
        mNotificationManager.createNotificationChannel(mChannel);

        // 为该通知设置一个id
        int notifyID = 1;
        // 通知渠道的id
        String CHANNEL_ID = "my_channel_01";
        // Create a notification and set the notification channel.
        Notification notification = new Notification.Builder(this)
                .setContentTitle("正在录屏").setContentText("录屏服务已开启")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setChannelId(CHANNEL_ID)
                .build();
        startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        QDLogger.e("onDestroy:"+this.getClass().getName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CaptureUtil.getInstance().stopVirtual();
        }
    }
}
