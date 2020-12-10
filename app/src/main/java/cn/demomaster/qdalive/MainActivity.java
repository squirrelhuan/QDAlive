package cn.demomaster.qdalive;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.demomaster.huan.quickdeveloplibrary.base.activity.QDActivity;
import cn.demomaster.huan.quickdeveloplibrary.helper.PermissionManager;
import cn.demomaster.huan.quickdeveloplibrary.helper.QDSharedPreferences;
import cn.demomaster.huan.quickdeveloplibrary.helper.QdThreadHelper;
import cn.demomaster.huan.quickdeveloplibrary.helper.toast.QdToast;
import cn.demomaster.huan.quickdeveloplibrary.service.AccessibilityHelper;
import cn.demomaster.huan.quickdeveloplibrary.service.QDAccessibilityService;
import cn.demomaster.huan.quickdeveloplibrary.util.QDAndroidDeviceUtil;
import cn.demomaster.huan.quickdeveloplibrary.widget.dialog.QDSheetDialog;
import cn.demomaster.qdalive.model.ActionModel;
import cn.demomaster.qdalive.model.ActionTypeEmun;
import cn.demomaster.qdalive.model.FileModel;
import cn.demomaster.qdalive.receiver.MyBroadcastReceiver;
import cn.demomaster.qdalive.util.CaptureUtil;
import cn.demomaster.qdalive.media.MediaProjectionUtil;
import cn.demomaster.qdalive.util.GestureHelper;
import cn.demomaster.qdalive.view.ControlPanelView;
import cn.demomaster.qdlogger_library.QDLogger;
import cn.demomaster.quicksticker_annotations.BindView;
import cn.demomaster.quicksticker_annotations.QuickStickerBinder;
import core.MqttException;
import core.model.Msg;
import core.mqtt.OnMessageReceiveListener;
import core.util.ByteUtil;

import static cn.demomaster.huan.quickdeveloplibrary.util.DisplayUtil.getStatusBarHeight;

public class MainActivity extends QDActivity {
    // MqttClient mqttClient;
    @BindView(R.id.btn_connect)
    Button btn_connect;
    @BindView(R.id.btn_devices)
    Button btn_devices;
    Button btn_control;
    TextView tv_sn;
    TextView tv_console;
    TextView btn_clear;
    TextView btn_accessibility;
    TextView btn_stop;
    TextView btn_shadow;
    RadioGroup rg_device;
    String clientID;
    Button btn_capture;
    ControlPanelView iv_screen;
    MyBroadcastReceiver myBroadcastReceiver;

    @BindView(R.id.tv_ip)
    TextView tv_ip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        QuickStickerBinder.getInstance().bind(this);
        clientID = QDAndroidDeviceUtil.getUniqueID(this);

        if(mqttActionListener==null){
            mqttActionListener = new MqttActionListener(mContext) {
                @Override
                public void onMqttConnect() {
                    btn_connect.setText("已连接");
                    btn_connect.setEnabled(false);
                    updateDevices();
                }

                @Override
                public void onReceived(byte[] bytes) {
                    System.out.println("onReceived size:" + bytes.length + "," + new String(bytes));
                    Msg msgObj;
                    try {
                        msgObj = JSON.parseObject(new String(bytes), Msg.class);
                        if (msgObj != null) {
                            String channelId = msgObj.getChannelId();
                            int contentType = msgObj.getType();
                            int index = msgObj.getOffset();
                            QDLogger.i("fileinfo:" + contentType + "," + channelId + "," + index);
                        }
                        QDLogger.i("json 解析success");
                    } catch (Exception e) {
                        QDLogger.i("json 解析失敗");
                        //e.printStackTrace();
                    }

                    QdThreadHelper.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv_console.append("\n" + new String(bytes));
                        }
                    });
                }

                @Override
                public void onRequest(Msg msgObj) {
                    // QDLogger.i("onRequest json 解析 ");
                    if (msgObj.getData() != null) {
                        String channelId = msgObj.getChannelId();
                        int contentType = msgObj.getType();
                        int offset = msgObj.getOffset();
                        int len = msgObj.getTotalSize();
                        //QDLogger.i("类型:" + contentType + ",channelId=" + channelId + ",文件：" + offset + "/" + len + ",本次传输：" + msgObj.getDataSize() + ",有效数据：" + msgObj.getData().length);
                        if (contentType == 1) {
                            MyService.mqttClient.response(msgObj.getClientId(), msgObj.getChannelId(), msgObj.getMsgId(), ("收到file i=" + offset), (byte) 1, null);
                            if (bitmapStrMap.containsKey(channelId)) {
                                FileModel fileModel = bitmapStrMap.get(channelId);
                                byte[] b1 = ByteUtil.byteMerger(fileModel.getData(), msgObj.getData());
                                fileModel.setData(b1);
                                bitmapStrMap.put(channelId, fileModel);

                                QDLogger.i("Size=" + fileModel.getData().length + "/" + msgObj.getTotalSize());
                                if (len == fileModel.getData().length) {
                                    QDLogger.i("isFull=" + msgObj.getTotalSize());
                                    bitmapStrMap.clear();
                                    refreshImage(fileModel);
                                }

                            } else {
                                QDLogger.i("onRequest2 bitmapStrMap=" + bitmapStrMap.size());
                                if (bitmapStrMap.size() > 0) {
                                    for (Map.Entry entry : bitmapStrMap.entrySet()) {
                                        FileModel fileModel = (FileModel) entry.getValue();
                                        //QDLogger.i("CGQ","bate64=" + fileModel.getData());
                                        QDLogger.i("Size2=" + fileModel.getData().length + "/" + msgObj.getTotalSize());
                                        if (len == fileModel.getData().length) {
                                            QDLogger.i("isFull2=" + msgObj.getTotalSize());
                                            refreshImage(fileModel);
                                        }
                                    }
                                }
                                bitmapStrMap.clear();

                                FileModel fileModel = new FileModel();
                                fileModel.setData(msgObj.getData());
                                fileModel.setFileSize(msgObj.getTotalSize());
                                bitmapStrMap.put(channelId, fileModel);
                            }
                        }
                    } else {
                        QDLogger.i("json 解析success");
                    }
                    //MyService.mqttClient.response(msgObj.getHeader().getSn(),msgObj.getHeader().getMsgId(),"我明天有事，来不了".getBytes(),null);
                }

                @Override
                public void onDisConnect(MqttException e) {
                    // Log.i("已断开");
                    e.printStackTrace();
                    QdThreadHelper.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btn_connect.setText("连接");
                            btn_connect.setEnabled(true);
                        }
                    });
                }

            };
        }

        MyService.setListener(mqttActionListener);
        Intent intent = new Intent(MainActivity.this, MyService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
            startService(intent);
        } else {
            startService(intent);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("cn.demomaster.qdalive.receiver.MyBroadcastReceiver");
        myBroadcastReceiver = new MyBroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
               /* Bundle bundle = intent.getExtras();
                String path = bundle.getString("screenshoot");
                File imgFile = new File(path);
                Uri contentUri = Uri.fromFile(imgFile);
                iv_screen.setImageURI(contentUri);*/
                iv_screen.setBitmap(((MyApp) getApplication()).bitmap);
            }
        };
        //下面这一句：NetworkChangeReceiver 就会收到所有值为android.net.conn.CONNECTIVITY_CHANGE 的广播
        registerReceiver(myBroadcastReceiver, intentFilter);

        btn_stop = findViewById(R.id.btn_stop);
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
               /* CaptureUtil.getInstance().stopVirtual();
                CaptureUtil.getInstance().release();*/
               MyService.setTargetDevice(null);
            }
        });
        iv_screen = findViewById(R.id.iv_screen);

        btn_shadow = findViewById(R.id.btn_shadow);
        btn_shadow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectDevice();
            }
        });
        btn_capture = findViewById(R.id.btn_capture);
        btn_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.postDelayed(runnable, 0);
            }
        });
        btn_control = findViewById(R.id.btn_control);
        btn_control.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*if (MyService.selectDevice == null || MyService.selectDevice == clientID) {
                    QdToast.show("请选择要控制的其他设备");
                    return;
                }*/

                showDeviceList();
            }
        });
        rg_device = findViewById(R.id.rg_device);
        rg_device.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                //MyService.selectDevice = MyService.devices[i];
                //tv_console.append("\n选择："+device);
            }
        });
        btn_clear = findViewById(R.id.btn_clear);
        tv_console = findViewById(R.id.tv_console);
        tv_sn = findViewById(R.id.tv_sn);
        btn_accessibility = findViewById(R.id.btn_accessibility);
        btn_accessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //确保无障碍服务已经开启
                if (!AccessibilityHelper.isEnable(MainActivity.this,QDAccessibilityService.class)) {
                    QDAccessibilityService.startSettintActivity(MainActivity.this);
                }
            }
        });
        btn_devices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateDevices();
            }
        });
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message message = Message.obtain();
                message.what = 790;
                //message.obj = mContext;
                if (MyService.handler != null) {
                    MyService.handler.sendMessage(message);
                }else {

                }
            }
        });
        /*btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyService.mqttClient.request(MyService.selectDevice, "明天去游泳吧".getBytes(), new OnMessageReceiveListener() {
                    @Override
                    public void onReceived(byte[] bytes) {
                        QdThreadHelper.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv_console.setText("回复：" + new String(bytes));
                            }
                        });
                    }
                });
            }
        });*/
        btn_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tv_console.setText("");
            }
        });

        System.out.println("clientID=" + clientID);
        tv_sn.setText("本机：" + clientID);


        String ip = QDSharedPreferences.getInstance().getString("ServerIp", ips[0]);
        tv_ip.setText(ip);
        MyService.serverIp = ip;
        tv_ip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showIpDialog();
            }
        });

//, Manifest.permission.SYSTEM_ALERT_WINDOW
        PermissionManager.getInstance().chekPermission(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, new PermissionManager.PermissionListener() {
            @Override
            public void onPassed() {

            }

            @Override
            public void onRefused() {

            }
        });
        requestPermissions();

        //CaptureUtil.getInstance().init(this);
        /*CaptureUtil.getInstance().setScreenCaputreListener(new ScreenCaputre.ScreenCaputreListener() {
            @Override
            public void onImageData(byte[] buf) {
                MyService.mqttClient.uploadFile(MyService.selectDevice,System.currentTimeMillis()+"",System.currentTimeMillis(), buf, new OnMessageReceiveListener() {
                    @Override
                    public void onReceived(byte[] bytes) {
                        QdToast.show("文件发送完成");

                    }
                });
            }
        });*/

        /*Spinner spinner = (Spinner) findViewById(R.id.spinner1);
        String[] mItems = new String[]{"192.168.199.107","192.168.0.106"};
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, mItems);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {

            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });*/
    }

    /**
     * 刷新设备列表
     */
    private void updateDevices() {
        if (MyService.mqttClient == null) {
            return;
        }
        MyService.mqttClient.request("devlist".getBytes(), new OnMessageReceiveListener() {
            @Override
            public void onReceived(byte[] data) {
                if (data != null)
                    QdThreadHelper.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv_console.append("\n" + new String(data));
                            rg_device.removeAllViews();
                            MyService.devices = new String(data).split(",");
                            for (int i = 0; i < MyService.devices.length; i++) {
                                String device = MyService.devices[i];
                                RadioButton radioButton = new RadioButton(MainActivity.this);
                                radioButton.setText(device);
                                radioButton.setId(i);
                                rg_device.addView(radioButton);
                            }
                        }
                    });
            }
        });
    }

    /**
     * 选择要投屏的设备
     */
    private void selectDevice() {
        if(MyService.devices==null){
            return;
        }
        new QDSheetDialog.MenuBuilder(mContext).setData(MyService.devices).setOnDialogActionListener(new QDSheetDialog.OnDialogActionListener() {
            @Override
            public void onItemClick(QDSheetDialog dialog, int position, List<String> data) {
                dialog.dismiss();
                MediaProjectionUtil.getInstance().startMediaIntent(mContext);
                MyService.setTargetDevice(data.get(position));
            }
        }).create().show();
    }

    /**
     * 选择要远程控制的设备
     */
    private void showDeviceList() {
        if(MyService.devices==null){
            return;
        }
        new QDSheetDialog.MenuBuilder(mContext).setData(MyService.devices).setOnDialogActionListener(new QDSheetDialog.OnDialogActionListener() {
            @Override
            public void onItemClick(QDSheetDialog dialog, int position, List<String> data) {
                 dialog.dismiss();
                //请求控制对方
                    ActionModel actionModel = new ActionModel();
                    actionModel.setActionType(ActionTypeEmun.control.value());
                    String str  =JSON.toJSONString(actionModel);
                    MyService. mqttClient.request(data.get(position), str.getBytes(), new OnMessageReceiveListener() {
                        @Override
                        public void onReceived(byte[] bytes) {
                            String r = new String(bytes);
                            if(r.equals("yes")){
                                QdThreadHelper.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Bundle bundle = new Bundle();
                                        bundle.putString("clientID",data.get(position));
                                        startActivity(ControlActivity.class,bundle);
                                    }
                                });
                            }else {
                                QdToast.show("对方已拒绝");
                            }
                        }
                    });
            }
        }).create().show();
    }

    String[] ips = new String[]{"192.168.199.107", "192.168.0.106"};
    /**
     * 显示serverIp选择框
     */
    private void showIpDialog() {
        new QDSheetDialog.MenuBuilder(mContext).setData(ips).setOnDialogActionListener(new QDSheetDialog.OnDialogActionListener() {
            @Override
            public void onItemClick(QDSheetDialog dialog, int position, List<String> data) {
                dialog.dismiss();
                QDSharedPreferences.getInstance().putString("ServerIp", data.get(position));
                MyService.serverIp = data.get(position);
                tv_ip.setText(MyService.serverIp);
            }
        }).create().show();
    }

    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Message message = Message.obtain();
                message.what = 789;
                //message.obj = mContext;
                QDLogger.i("发送handler消息");
                if (MyService.handler != null) {
                    MyService.handler.sendMessage(message);
                }
                handler.removeCallbacks(runnable);
                // handler.postDelayed(runnable,30000);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        MyService.setListener(mqttActionListener);
    }

    @SuppressLint("WrongConstant")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        MediaProjectionUtil.getInstance().onActivityResult(requestCode, resultCode, data);
    }

    Map<String, FileModel> bitmapStrMap = new HashMap<>();
    MqttActionListener mqttActionListener;
    private void refreshImage(FileModel fileModel) {
        QDLogger.i("刷新图像");
        try {
            Bitmap bitmap = base64ToBitmap(new String(fileModel.getData()));
            QdThreadHelper.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    QDLogger.i("setImageBitmap");
                    iv_screen.setBitmap(bitmap);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * base64转为bitmap
     *
     * @param base64Data
     * @return
     */
    public static Bitmap base64ToBitmap(String base64Data) {
        try {
            QDLogger.i("base64Data=" + base64Data);
            byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            //此处做动态权限申请
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 135);
        }
    }

    public static String QDAccessibilityServiceName = QDAccessibilityService.class.getCanonicalName();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        QDLogger.e("onDestroy:"+this.getClass().getName());
        QuickStickerBinder.getInstance().unBind(this);
        unregisterReceiver(myBroadcastReceiver);
        CaptureUtil.getInstance().release();
        MyService.setListener(null);
    }

}