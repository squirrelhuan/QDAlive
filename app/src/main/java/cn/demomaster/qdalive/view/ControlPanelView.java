package cn.demomaster.qdalive.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import cn.demomaster.qdlogger_library.QDLogger;

public class ControlPanelView extends View {
    public ControlPanelView(Context context) {
        super(context);
    }

    public ControlPanelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ControlPanelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ControlPanelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    Bitmap mBitmap_last;
    Bitmap mBitmap;

    public void setBitmap(Bitmap bitmap) {
        if (mBitmap != null) {
            this.mBitmap_last = mBitmap;
        }
           /* long t1 = System.currentTimeMillis();
            mBitmap = getNewBitmap(mBitmap,bitmap);
            QDLogger.i("getNewBitmap time="+(System.currentTimeMillis()-t1));*/

        mBitmap = bitmap;
        postInvalidate();
    }

    private Bitmap getNewBitmap(Bitmap mBitmap, Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (mBitmap.getWidth() != width || mBitmap.getHeight() != height) {
            return bitmap;
        }
        for (int i = 0; i < width; ++i) {
            for (int j = 0; j < height; ++j) {
                int color = bitmap.getPixel(i, j);
                if (mBitmap.getPixel(i, j) != bitmap.getPixel(i, j) && color != Color.TRANSPARENT) {
                    try {
                        mBitmap.setPixel(i, j, color);
                    } catch (Exception e) {
                        QDLogger.i("color=" + color);
                        e.printStackTrace();
                    }
                }
            }
        }
        return mBitmap;
    }

    private Bitmap toConformBitmap(Bitmap background, Bitmap foreground) {
        if (background == null) {
            return null;
        }

        int bgWidth = background.getWidth();
        int bgHeight = background.getHeight();
        //int fgWidth = foreground.getWidth();
        //int fgHeight = foreground.getHeight();
        //create the new blank bitmap 创建一个新的和SRC长度宽度一样的位图
        Bitmap newbmp = Bitmap.createBitmap(bgWidth, bgHeight, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(newbmp);
        //draw bg into
        cv.drawBitmap(background, 0, 0, null);//在 0，0坐标开始画入bg
        //draw fg into
        cv.drawBitmap(foreground, 0, 0, null);//在 0，0坐标开始画入fg ，可以从任意位置画入
        //save all clip
        //cv.save(Canvas.ALL_SAVE_FLAG);//保存
        //cv.save(1);
        //store
        cv.restore();//存储
        return newbmp;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect rect = new Rect(0, 0, getWidth(), getHeight());
        if (mBitmap_last != null&&mBitmap!=null) {
            Rect rect2 = new Rect(0, 0, mBitmap_last.getWidth(), mBitmap_last.getHeight());
            canvas.drawBitmap(mBitmap, rect2, rect, new Paint());
        }
        if (mBitmap != null) {
            Rect rect2 = new Rect(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
            canvas.drawBitmap(mBitmap, rect2, rect, new Paint());
        }

    }
}
