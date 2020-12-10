package cn.demomaster.qdalive;

import android.app.Activity;
import android.app.Dialog;

import cn.demomaster.huan.quickdeveloplibrary.base.dialog.DialogActivityHelper;
import cn.demomaster.huan.quickdeveloplibrary.base.dialog.QdDialogActivity;
import cn.demomaster.huan.quickdeveloplibrary.helper.QDActivityManager;
import cn.demomaster.huan.quickdeveloplibrary.helper.QdThreadHelper;
import cn.demomaster.huan.quickdeveloplibrary.widget.dialog.OnClickActionListener;
import cn.demomaster.huan.quickdeveloplibrary.widget.dialog.QDDialog;
import cn.demomaster.qdalive.activity.DialogActivity;
import cn.demomaster.qdalive.media.MediaProjectionUtil;
import core.MqttConnectionListener;
import core.model.Msg;

public abstract class MqttActionListener implements MqttConnectionListener {
    Activity mActivity;

    public MqttActionListener() {

    }

    public MqttActionListener(Activity activity) {
        this.mActivity = activity;
    }

    public void onRequestControl(Msg msg) {
        if (mActivity != null) {
            QdThreadHelper.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!QDActivityManager.getInstance().isRunningOnForeground(mActivity)) {
                        QdDialogActivity.Builder builder = (QdDialogActivity.Builder) new QdDialogActivity.Builder(mActivity)
                                .setTitle("远程控制")
                                .setMessage(msg.getClientId() + "正在请求控制您的设备，是否允许？")
                                .setBackgroundRadius(20);
                        builder.addButtonAction("确定", new QdDialogActivity.OnClickListener() {
                            @Override
                            public void onClick(Activity activity, Object tag) {
                                activity.finish();
                                MyService.mqttClient.response(msg.getClientId(), null, msg.getMsgId(), "yes", (byte) msg.getType(), null);
                                MediaProjectionUtil.getInstance().startMediaIntent(mActivity);
                                MyService.setTargetDevice(msg.getClientId());
                            }
                        }).addButtonAction("拒绝", new QdDialogActivity.OnClickListener() {
                                    @Override
                                    public void onClick(Activity activity, Object tag) {
                                        activity.finish();
                                        MyService.mqttClient.response(msg.getClientId(), null, msg.getMsgId(), "no", (byte) msg.getType(), null);
                                    }
                                });
                        DialogActivityHelper.showDialog(mActivity, DialogActivity.class, builder);
                    } else {
                        QDDialog.Builder builder = new QDDialog.Builder(mActivity)
                                .setTitle("远程控制")
                                .setMessage(msg.getClientId() + "正在请求控制您的设备，是否允许？")
                                .setBackgroundRadius(20)
                                .addAction("确定", new OnClickActionListener() {
                                    @Override
                                    public void onClick(Dialog dialog, Object tag) {
                                        dialog.dismiss();
                                        MyService.mqttClient.response(msg.getClientId(), null, msg.getMsgId(), "yes", (byte) msg.getType(), null);

                                        MediaProjectionUtil.getInstance().startMediaIntent(mActivity);
                                        MyService.setTargetDevice(msg.getClientId());
                                    }
                                })
                                .addAction("拒绝", new OnClickActionListener() {
                                    @Override
                                    public void onClick(Dialog dialog, Object tag) {
                                        dialog.dismiss();
                                        MyService.mqttClient.response(msg.getClientId(), null, msg.getMsgId(), "no", (byte) msg.getType(), null);
                                    }
                                });
                        builder.create().show();
                    }
                }
            });
        }
    }

    public void onRequestControled(String clientId) {
        if (mActivity != null) {
            //MyService.mqttClient.response(msg.getClientId(),null,msg.getMsgId(),"yes",(byte) msg.getType(),null);
        }
    }
}