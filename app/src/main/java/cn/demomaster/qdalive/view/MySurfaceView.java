package cn.demomaster.qdalive.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceView;

public class MySurfaceView extends SurfaceView {
    public MySurfaceView(Context context) {
        super(context);
    }

    public MySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MySurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MySurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    GestureDetector gestureDetector;

    public void setGestureDetector(GestureDetector gestureDetector) {
        this.gestureDetector = gestureDetector;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(gestureDetector!=null){
          return gestureDetector.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    Matrix matrix = new Matrix();

    public void setMatrix(Matrix matrix) {
        this.matrix = matrix;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.setMatrix(matrix);
        super.onDraw(canvas);
    }
}
