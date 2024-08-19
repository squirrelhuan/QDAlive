package cn.demomaster.qdalive.util;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.accessibility.AccessibilityEvent;

import com.alibaba.fastjson.JSON;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import cn.demomaster.huan.quickdeveloplibrary.helper.QdThreadHelper;
import cn.demomaster.huan.quickdeveloplibrary.service.AccessibilityHelper;
import cn.demomaster.huan.quickdeveloplibrary.service.QDAccessibilityService;
import cn.demomaster.huan.quickdeveloplibrary.util.DisplayUtil;
import cn.demomaster.huan.quickdeveloplibrary.util.terminal.ADBHelper;
import cn.demomaster.huan.quickdeveloplibrary.util.terminal.ProcessResult;
import cn.demomaster.huan.quickdeveloplibrary.util.terminal.QDRuntimeHelper;
import cn.demomaster.huan.quickdeveloplibrary.view.floatview.ServiceHelper;
import cn.demomaster.qdalive.MyService;
import cn.demomaster.qdalive.model.ActionModel;
import cn.demomaster.qdalive.model.ActionTypeEmun;
import cn.demomaster.qdalive.model.TouchPoint;
import cn.demomaster.qdlogger_library.QDLogger;
import core.mqtt.OnMessageReceiveListener;

import static android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK;
import static android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME;
import static android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS;

public class GestureHelper {
    static QDAccessibilityService mQDAccessibilityService;
    private static int code = 12345;
    public static void init(Context context) {

        //执行全局动作，api16以上可用
        /*performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);//切换到分屏
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);//打开快速设置，暂时不知道这个有什么用
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);//打开通知栏
        performGlobalAction(GLOBAL_ACTION_BACK);//模拟back键
        performGlobalAction(GLOBAL_ACTION_HOME);//模拟home键
        performGlobalAction(GLOBAL_ACTION_RECENTS);//模拟最近任务键
        performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);//打开电源键长按对话框
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);//锁屏
        performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);//截图*/
        if (ServiceHelper.serverIsRunning(context.getApplicationContext(), QDAccessibilityService.class.getName())) {
            mQDAccessibilityService = QDAccessibilityService.instance;
        }
        /*AccessibilityHelper.registerAccessibilityEventListener(code, new QDAccessibilityService.OnAccessibilityListener() {
            @Override
            public void onServiceConnected(QDAccessibilityService qdAccessibilityService) {
                mQDAccessibilityService = qdAccessibilityService;
            }

            @Override
            public void onAccessibilityEvent(AccessibilityService accessibilityService, AccessibilityEvent event) {

            }

            @Override
            public void onServiceDestroy() {
                mQDAccessibilityService = null;
            }
        });*/

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                QDLogger.i("msg.what=" + msg.what);
                switch (msg.what) {
                    case 5://ActionTypeEmun.home
                        goHome();
                        break;
                    case 6://ActionTypeEmun.back.value()
                        task();
                        break;
                    case 7://ActionTypeEmun.back.value()
                        back();
                        break;
                    case 0:
                    case 1:
                    case 2://ActionTypeEmun.move.value():
                        ActionModel actionModel = (ActionModel) msg.obj;
                        doMove(actionModel);
                        break;
                    default:
                        break;
                }
            }
        };
    }

    public static void doMove(ActionModel actionModel) {//仿滑动
        QDLogger.println("仿滑动 android.os.Build.VERSION.SDK_INT = " + android.os.Build.VERSION.SDK_INT);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            if (mQDAccessibilityService == null) {
                QDLogger.i("mQDAccessibilityService = null");
                return;
            }
            if (actionModel.getActionType() == ActionTypeEmun.move.value()) {
                String str = actionModel.getData();
                List<TouchPoint> pointFList = JSON.parseArray(str, TouchPoint.class);
                if (pointFList == null || pointFList.size() < 2) {
                    return;
                }
                Path path = new Path();
                long duration = pointFList.get(pointFList.size() - 1).getTime() - pointFList.get(0).getTime();
                for (int i = 0; i < pointFList.size(); i++) {
                    float x = pointFList.get(i).getX() * DisplayUtil.getScreenWidth(mQDAccessibilityService);
                    float y = pointFList.get(i).getY() * DisplayUtil.getScreenHeight(mQDAccessibilityService);
                    QDLogger.i("" + x + "," + y);
                    if (i == 0) {
                        path.moveTo(x, y);//滑动起点
                    } else {
                        path.lineTo(x, y);//滑动终点
                    }
                }
                GestureDescription.Builder builder = new GestureDescription.Builder();
                GestureDescription description = null;
                try {
                    /* if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        description = builder.addStroke(new GestureDescription.StrokeDescription(path, 100L, 100L,true)).build();
                    }else {*/
                    builder.addStroke(new GestureDescription.StrokeDescription(path, 0L, duration));
                    //builder.addStroke(new GestureDescription.StrokeDescription(path, 0L, duration));
                    description = builder.build();
                    //}
                    //100L 第一个是开始的时间，第二个是持续时间
                    mQDAccessibilityService.dispatchGesture(description, new AccessibilityService.GestureResultCallback() {
                        @Override
                        public void onCompleted(GestureDescription gestureDescription) {
                            super.onCompleted(gestureDescription);
                            QDLogger.i("滑动结束");
                        }
                    }, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (actionModel.getActionType() == ActionTypeEmun.move.value()) {
                String str = actionModel.getData();
                List<TouchPoint> pointFList = JSON.parseArray(str, TouchPoint.class);
                if (pointFList == null || pointFList.size() < 2) {
                    return;
                }
                long duration = pointFList.get(pointFList.size() - 1).getTime() - pointFList.get(0).getTime();

              /*  List<PointF> pointFList1 = new ArrayList<>();
                for (int i = 0; i < pointFList.size(); i++) {
                    float x = pointFList.get(i).getX() * DisplayUtil.getScreenWidth(mQDAccessibilityService);
                    float y = pointFList.get(i).getY() * DisplayUtil.getScreenHeight(mQDAccessibilityService);
                    QDLogger.i("" + x + "," + y);
                    pointFList1.add(new PointF(x, y));
                }*/
                String swapStr="input swipe";
                swapStr+=" "+(int)(pointFList.get(0).getX()* DisplayUtil.getScreenWidth(context));
                swapStr+=" "+(int)(pointFList.get(0).getY()* DisplayUtil.getScreenHeight(context));
                swapStr+=" "+(int)(pointFList.get(pointFList.size() - 1).getX()* DisplayUtil.getScreenWidth(context));
                swapStr+=" "+(int)(pointFList.get(pointFList.size() - 1).getY()* DisplayUtil.getScreenHeight(context));
                try {
                    QDRuntimeHelper.executeSu(swapStr, new QDRuntimeHelper.OnReceiveListener() {
                        @Override
                        public void onReceive(ProcessResult result) {
                            if (result.getCode() == 0) {
                                QDLogger.i("执行完成："+result.getResult());
                            } else {
                                QDLogger.e("执行异常："+result.getError());
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }



    //运行在主线程的Handler：使用Android默认的UI线程中的Looper
    public static Handler handler;
    public static Context context;

    public static void goHome() {
        if (mQDAccessibilityService == null) {
            return;
        }
        mQDAccessibilityService.performGlobalAction(GLOBAL_ACTION_HOME);//模拟home键
    }

    public static void back() {
        if (mQDAccessibilityService == null) {
            return;
        }
        mQDAccessibilityService.performGlobalAction(GLOBAL_ACTION_BACK);//模拟back键
    }

    public static void task() {
        if (mQDAccessibilityService == null) {
            return;
        }
        mQDAccessibilityService.performGlobalAction(GLOBAL_ACTION_RECENTS);//模拟最近任务键
    }


    GestureDetector.OnGestureListener onGestureListener;

    public static void requestTask(String clientID) {
        ActionModel actionModel = new ActionModel();
        actionModel.setActionType(ActionTypeEmun.task.value());
        String str = JSON.toJSONString(actionModel);
        MyService.mqttClient.request(clientID, str.getBytes(), new OnMessageReceiveListener() {
            @Override
            public void onReceived(byte[] bytes) {
            }
        });
    }

    public static void requestHome(String clientID) {
        ActionModel actionModel = new ActionModel();
        actionModel.setActionType(ActionTypeEmun.home.value());
        String str = JSON.toJSONString(actionModel);
        MyService.mqttClient.request(clientID, str.getBytes(), new OnMessageReceiveListener() {
            @Override
            public void onReceived(byte[] bytes) {

            }
        });
    }

    public static void requestBack(String clientID) {
        ActionModel actionModel = new ActionModel();
        actionModel.setActionType(ActionTypeEmun.back.value());
        String str = JSON.toJSONString(actionModel);
        MyService.mqttClient.request(clientID, str.getBytes(), new OnMessageReceiveListener() {
            @Override
            public void onReceived(byte[] bytes) {

            }
        });
    }

    public static void requestMove(String clientID, List<TouchPoint> pointFList1) {
        ActionModel actionModel = new ActionModel();
        actionModel.setActionType(ActionTypeEmun.move.value());
        String str2 = JSON.toJSONString(pointFList1);
        actionModel.setData(str2);
        String str = JSON.toJSONString(actionModel);
        MyService.mqttClient.request(clientID, str.getBytes(), new OnMessageReceiveListener() {
            @Override
            public void onReceived(byte[] bytes) {

            }
        });
    }

}
