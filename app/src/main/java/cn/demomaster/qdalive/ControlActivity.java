package cn.demomaster.qdalive;

import android.graphics.PointF;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import cn.demomaster.huan.quickdeveloplibrary.base.activity.QDActivity;
import cn.demomaster.huan.quickdeveloplibrary.base.tool.actionbar.ACTIONBAR_TYPE;
import cn.demomaster.huan.quickdeveloplibrary.helper.QdThreadHelper;
import cn.demomaster.qdalive.model.ActionModel;
import cn.demomaster.qdalive.model.ActionTypeEmun;
import cn.demomaster.qdalive.model.TouchPoint;
import cn.demomaster.qdalive.util.GestureHelper;
import cn.demomaster.qdalive.view.MySurfaceView;
import cn.demomaster.qdlogger_library.QDLogger;
import cn.demomaster.quicksticker_annotations.BindView;
import cn.demomaster.quicksticker_annotations.QuickStickerBinder;
import core.MqttConnectionListener;
import core.MqttException;
import core.model.Msg;
import core.mqtt.OnMessageReceiveListener;

public class ControlActivity extends QDActivity {


    private final static String MIME_TYPE = "video/avc";
    private final static int VIDEO_WIDTH = 720;
    private final static int VIDEO_HEIGHT = 1280;

    MySurfaceView mSurfaceView;

    @BindView(R.id.tv_lable)
    TextView tv_lable;
    @BindView(R.id.iv_back)
    ImageView iv_back;
    @BindView(R.id.iv_home)
    ImageView iv_home;
    @BindView(R.id.iv_task)
    ImageView iv_task;

    private MediaCodec mCodec;

    long firstDownTime =0;
    TouchPoint firstTouchedPoint;
    //ControlPanelView controlPanelView;
    List<TouchPoint> pointFList =null;
    String clientID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contol);
        QuickStickerBinder.getInstance().bind(this);
        getActionBarTool().setActionBarType(ACTIONBAR_TYPE.NO_ACTION_BAR_NO_STATUS);

        Bundle bundle = getIntent().getExtras();
        if(bundle!=null){
            if(bundle.containsKey("clientID")) {
                clientID = bundle.getString("clientID","");
                tv_lable.setText("远程设备："+clientID+"");
            }
        }

        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GestureHelper.requestBack(clientID);
            }
        });
        iv_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GestureHelper.requestHome(clientID);
            }
        });
        iv_task.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               GestureHelper.requestTask(clientID);
            }
        });

        mSurfaceView = (MySurfaceView) findViewById(R.id.surfaceView1);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        mSurfaceView.getHolder().setFixedSize(dm.widthPixels, dm.heightPixels);
        mSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                initDecoder();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });
        GestureDetector.OnGestureListener onGestureListener = new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                QDLogger.i("onDown "+e.getX()+","+e.getY());
                firstDownTime = System.currentTimeMillis();
                firstTouchedPoint =new TouchPoint(e.getX()/ mSurfaceView.getWidth(),e.getY()/ mSurfaceView.getHeight(),firstDownTime);
                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {
                QDLogger.i("onShowPress "+e.getX()+","+e.getY());
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                QDLogger.i("onSingleTapUp "+e.getX()+","+e.getY());
                TouchPoint pointF2 =new TouchPoint(e.getX()/ mSurfaceView.getWidth(),e.getY()/ mSurfaceView.getHeight(),System.currentTimeMillis());
                List<TouchPoint> touchPoints = new ArrayList<>();
                touchPoints.add(firstTouchedPoint);
                touchPoints.add(pointF2);
                GestureHelper.requestMove(clientID,touchPoints);
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                QDLogger.i("onScroll ("+e1.getX()+","+e1.getY()+"),("+e2.getX()+","+e2.getY()+")");
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                QDLogger.i("onLongPress "+e.getX()+","+e.getY());
                TouchPoint pointF2 =new TouchPoint(e.getX()/ mSurfaceView.getWidth(),e.getY()/ mSurfaceView.getHeight(),System.currentTimeMillis());
                List<TouchPoint> touchPoints = new ArrayList<>();
                touchPoints.add(firstTouchedPoint);
                touchPoints.add(pointF2);
                GestureHelper.requestMove(clientID,touchPoints);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                QDLogger.i("onFling ("+e1.getX()+","+e1.getY()+"),("+e2.getX()+","+e2.getY()+")");
                TouchPoint pointF =new TouchPoint(e1.getX()/ mSurfaceView.getWidth(),e1.getY()/ mSurfaceView.getHeight(),firstDownTime);
                TouchPoint pointF2 =new TouchPoint(e2.getX()/ mSurfaceView.getWidth(),e2.getY()/ mSurfaceView.getHeight(),System.currentTimeMillis());
                List<TouchPoint> touchPoints = new ArrayList<>();
                touchPoints.add(pointF);
                touchPoints.add(pointF2);
                GestureHelper.requestMove(clientID,touchPoints);
                return false;
            }
        };

        GestureDetector detector = new GestureDetector(this,onGestureListener);
        mSurfaceView.setGestureDetector(detector);
       // controlPanelView = findViewById(R.id.controlPanelView);
        /*mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                String str = "("+motionEvent.getX()+","+motionEvent.getY()+")";
                Log.i("tag",str);
                TouchPoint pointF =new TouchPoint(motionEvent.getX()/ mSurfaceView.getWidth(),motionEvent.getY()/ mSurfaceView.getHeight(),System.currentTimeMillis());
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        pointFList = new ArrayList<>();
                        pointFList.add(pointF);
                        break;
                    case MotionEvent.ACTION_UP:
                        //moveAction(lastPoint,new PointF(motionEvent.getX(),motionEvent.getY()));
                        pointFList.add(pointF);
                       *//* List<PointF> pointFList1 = new ArrayList<>();
                        pointFList1.addAll(pointFList);*//*
                        moveAction(pointFList);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        *//*pointFList.add(pointF);
                        moveAction(pointFList);
                        pointFList = new ArrayList<>();
                        pointFList.add(pointF);*//*
                       // pointFList.add(new PointF(motionEvent.getX()/ controlPanelView.getWidth(),motionEvent.getY()/ controlPanelView.getHeight()));

                        //moveAction(lastPoint,new PointF(motionEvent.getX()/ controlPanelView.getWidth(),motionEvent.getY()/ controlPanelView.getHeight()));
                        break;
                }
                //QDLogger.i(str);
                return true;
            }
        });*/


        if(mqttActionListener==null){
            mqttActionListener = new MqttActionListener() {
                @Override
                public void onMqttConnect() {
                    //System.out.println("Mqtt连接成功");
                    //char[] chars = bytes.toString().toCharArray();
                }

                @Override
                public void onReceived(byte[] bytes) {
                    if(visiable) {
                        System.out.println("onReceived size:" + bytes.length + "," + new String(bytes));
                        Msg msgObj;
                        try {
                            msgObj = JSON.parseObject(new String(bytes), Msg.class);
                            if (msgObj != null) {
                                String channelId = msgObj.getChannelId();
                                int contentType = msgObj.getType();
                                int index = msgObj.getOffset();
                                QDLogger.println("fileinfo:" + contentType + "," + channelId + "," + index);
                                if (contentType == 2) {
                                    QdThreadHelper.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            onFrame(bytes, 0, bytes.length);
                                        }
                                    });
                                }
                            }
                        } catch (Exception e) {
                            QDLogger.i("json 解析失敗");
                            //e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onRequest(Msg msgObj) {
                    //QDLogger.i("onRequest json 解析 ");
                    if(visiable) {
                        if (msgObj.getData() != null) {
                            String channelId = msgObj.getChannelId();
                            int contentType = msgObj.getType();
                            int offset = msgObj.getOffset();
                            int len = msgObj.getTotalSize();
                            QDLogger.i("CGQ", "msgId=" + msgObj.getMsgId() + ",类型:" + contentType + ",channelId=" + channelId + ",文件：" + offset + "/" + len + ",本次传输：" + msgObj.getDataSize());
                            MyService.mqttClient.response(msgObj.getClientId(), msgObj.getChannelId(), msgObj.getMsgId(), ("收到file i=" + offset), (byte) 1, null);
                            if (contentType == 2) {
                                QdThreadHelper.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //QDLogger.i("onFrame=" + msgObj.getData().length);
                                        onFrame(msgObj.getData(), 0, msgObj.getData().length);
                                    }
                                });
                            } else {

                            }
                        } else {
                            QDLogger.i("json 解析失败");
                        }
                        //MyService.mqttClient.response(msgObj.getHeader().getSn(),msgObj.getHeader().getMsgId(),"我明天有事，来不了".getBytes(),null);
                    }
                }

                @Override
                public void onDisConnect(MqttException e) {
                    // Log.i("已断开");
                    System.out.println("已断开");
                    e.printStackTrace();
                }
            };
        }

        MyService.setListener(mqttActionListener);
    }
    boolean initedDecoder;
    public void initDecoder() {
        try {
            mCodec = MediaCodec.createDecoderByType(MIME_TYPE);
            setConfigure();
            mCodec.start();
            initedDecoder = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setConfigure() {
        final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT);
        format.setInteger(MediaFormat.KEY_BIT_RATE,  VIDEO_WIDTH * VIDEO_HEIGHT);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        // 横屏
//            byte[] header_sps = {0, 0, 0, 1, 103, 66, -128, 31, -38, 1, 64, 22, -24, 6, -48, -95, 53};
//            byte[] header_pps = {0, 0 ,0, 1, 104, -50, 6, -30};

        // 竖屏
        byte[] header_sps = {0, 0, 0, 1, 103, 66, -128, 31, -38, 2, -48, 40, 104, 6, -48, -95, 53};
        byte[] header_pps = {0, 0 ,0, 1, 104, -50, 6, -30};
//
        format.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps));
        format.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps));
        mCodec.configure(format, mSurfaceView.getHolder().getSurface(),
                null, 0);
    }

    //    int mCount = 0;
    public boolean onFrame(byte[] buf, int offset, int length) {
        if(mCodec==null){
            return false;
        }
        // Get input buffer index
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        int inputBufferIndex = mCodec.dequeueInputBuffer(100);
//        Log.v(TAG, " inputBufferIndex  " + inputBufferIndex);

        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(buf, offset, length);
            mCodec.queueInputBuffer(inputBufferIndex, 0, length, System.currentTimeMillis(), 0);
//            mCount++;
        } else {
            return false;
        }
        // Get output buffer index
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 100);

        while (outputBufferIndex >= 0) {
            mCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
        return true;
    }



    MqttActionListener mqttActionListener;

    boolean visiable;
    @Override
    protected void onResume() {
        super.onResume();
        visiable =true;
        if(initedDecoder) {
            try {
                //mCodec = MediaCodec.createDecoderByType(MIME_TYPE);
                setConfigure();
                //mCodec.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        visiable =false;
        if(initedDecoder) {
            mCodec.stop();
            //mCodec.release();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mCodec!=null){
            try {
                mCodec.release();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        QuickStickerBinder.getInstance().unBind(this);
    }
}