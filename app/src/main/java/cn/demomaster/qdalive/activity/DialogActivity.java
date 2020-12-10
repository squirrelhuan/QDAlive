package cn.demomaster.qdalive.activity;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.demomaster.huan.quickdeveloplibrary.base.dialog.QdDialogActivity;
import cn.demomaster.huan.quickdeveloplibrary.util.DisplayUtil;
import cn.demomaster.huan.quickdeveloplibrary.view.drawable.DividerGravity;
import cn.demomaster.huan.quickdeveloplibrary.view.drawable.QDividerDrawable;
import cn.demomaster.huan.quickdeveloplibrary.widget.dialog.ActionButton;
import cn.demomaster.huan.quickdeveloplibrary.widget.dialog.QDDialog;

public class DialogActivity extends QdDialogActivity {

    public int actionButtonPadding;
    private Context context;
    private String title="134";
    private String message="6898";
    private int icon;
    private QDDialog.ShowType showType = QDDialog.ShowType.normal;
    private QDDialog.DataType dataType = QDDialog.DataType.text;
    private int width = ViewGroup.LayoutParams.MATCH_PARENT;
    private int gravity_header = Gravity.LEFT;
    private int gravity_body = Gravity.LEFT;
    private int gravity_foot = Gravity.CENTER;
    private int gravity = Gravity.CENTER;
    private boolean isFullScreen = false;
    private int margin = 0;//当isFullScreen=true时生效
    private int padding = -1;
    private int padding_header;
    private int padding_body;
    private int padding_foot;
    private int minHeight_header;
    private int minHeight_body;
    private int minHeight_foot;
    private int color_header = Color.TRANSPARENT;
    private int color_body = Color.TRANSPARENT;
    private int color_foot = Color.TRANSPARENT;
    private int text_color_header = Color.BLACK;
    private int text_color_body = Color.BLACK;
    private int text_color_foot = Color.BLACK;
    private int text_size_header = 18;
    private int text_size_body = 16;
    private int text_size_foot = 16;
    private View contentView;
    private int contentViewLayoutID;

    private int backgroundColor = Color.WHITE;
    private int lineColor = Color.GRAY;
    private float[] backgroundRadius = new float[8];
    private int animationStyleID = cn.demomaster.huan.quickdeveloplibrary.R.style.qd_dialog_animation_center_scale;
    private List<ActionButton> actionButtons = new ArrayList<>();

    private LinearLayout contentLinearView;
    private LinearLayout headerView;
    private LinearLayout bodyView;
    private LinearLayout footView;

    @Override
    public void generateView(LayoutInflater layoutInflater, ViewGroup viewParent) {

        if(data!=null) {
            QDDialog.Builder builder = (QDDialog.Builder) data;
            this.title = builder.title;
            this.message = builder.message;
            actionButtonPadding = builder.actionButtonPadding;
            this.context = builder.context;
            title = builder.title;
            message = builder.message;
            icon = builder.icon;
            showType = builder.showType;
            dataType = builder.dataType;
            width = builder.width;
            gravity_header = builder.gravity_header;
            gravity_body = builder.gravity_body;
            gravity_foot = builder.gravity_foot;
            padding_header = builder.padding_header;
            padding_body = builder.padding_body;
            padding_foot = builder.padding_foot;
            minHeight_header = builder.minHeight_header;
            minHeight_body = builder.minHeight_body;
            minHeight_foot = builder.minHeight_foot;
            color_header = builder.color_header;
            color_body = builder.color_body;
            color_foot = builder.color_foot;
            text_color_header = builder.text_color_header;
            text_color_body = builder.text_color_body;
            text_color_foot = builder.text_color_foot;
            text_size_header = builder.text_size_header;
            text_size_body = builder.text_size_body;
            text_size_foot = builder.text_size_foot;
            contentView = builder.contentView;
            contentViewLayoutID = builder.contentViewLayoutID;

            backgroundColor = builder.backgroundColor;
            lineColor = builder.lineColor;
            backgroundRadius = builder.backgroundRadius;
            animationStyleID = builder.animationStyleID;
            actionButtons = builder.actionButtons;

            padding = builder.padding;
            isFullScreen = builder.isFullScreen;
            margin = builder.margin;
        }
        //title = builder.title;
       // View view = layoutInflater.inflate(R.);
        context = this;
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.leftMargin = DisplayUtil.dip2px(this,20);
        layoutParams.topMargin = DisplayUtil.dip2px(this,20);
        layoutParams.rightMargin = DisplayUtil.dip2px(this,20);
        layoutParams.bottomMargin = DisplayUtil.dip2px(this,20);
        //@drawable/panel_background

        contentLinearView = new LinearLayout(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            contentLinearView.setId(View.generateViewId());
        }
        contentLinearView.setOrientation(LinearLayout.VERTICAL);
        //新建一个Drawable对象
        QDividerDrawable drawable_bg = new QDividerDrawable(DividerGravity.NONE);
        drawable_bg.setCornerRadii(backgroundRadius);
        drawable_bg.setBackGroundColor(backgroundColor);
        contentLinearView.setBackground(drawable_bg);
        //contentView.setPadding((int)builder.backgroundRadius[0],(int)builder.backgroundRadius[2],(int)builder.backgroundRadius[4],(int)builder.backgroundRadius[6]);
        if (title == null && message != null && actionButtons.size() == 0) {
            showType = QDDialog.ShowType.onlyBody;
        }
        if (title != null && message != null && actionButtons.size() == 0) {
            showType = QDDialog.ShowType.noFoot;
        }
/*
        int padding_header = padding_header;
        int padding_body = padding_body;*/
        switch (showType) {
            case normal:
                headerView = new LinearLayout(context);
                bodyView = new LinearLayout(context);
                footView = new LinearLayout(context);
                headerView.setPadding(padding_header, padding_header, padding_header, padding_header);
                bodyView.setPadding(padding_body, 0, padding_body, padding_body);
                break;
            case noHeader:
                bodyView = new LinearLayout(context);
                footView = new LinearLayout(context);
                bodyView.setPadding(padding_body, padding_body, padding_body, padding_body);
                break;
            case onlyBody:
                bodyView = new LinearLayout(context);
                bodyView.setPadding(padding_body, padding_body, padding_body, padding_body);
                break;
            case noFoot:
                headerView = new LinearLayout(context);
                bodyView = new LinearLayout(context);
                headerView.setPadding(padding_header, padding_header, padding_header, padding_header);
                bodyView.setPadding(padding_body, 0, padding_body, padding_body);
                break;
            case contentView:
                ViewGroup.LayoutParams layoutParams1 = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                contentLinearView.addView(contentView, layoutParams1);
                break;
            case contentLayout:
                ViewGroup.LayoutParams layoutParams2 = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                View view = LayoutInflater.from(context).inflate(contentViewLayoutID, null, false);
                contentLinearView.addView(view, layoutParams2);
                break;
        }
        if (headerView != null) {
            contentLinearView.addView(headerView);
            headerView.setMinimumHeight(minHeight_header);
            headerView.setBackgroundColor(color_header);
            headerView.setGravity(gravity_header);
            headerView.setTag(gravity_header);
            addTextView(headerView, title, text_color_header, text_size_header);
        }
        if (bodyView != null) {
            contentLinearView.addView(bodyView);
            bodyView.setMinimumHeight(minHeight_body);
            bodyView.setBackgroundColor(color_body);

            bodyView.setGravity(gravity_body);
            bodyView.setTag(gravity_body);
            addBodyTextView(bodyView, message, text_color_body, text_size_body);
        }
        int actionPadding = actionButtonPadding;//DisplayUtil.dip2px(getContext(), 10);
        if (footView != null) {
            contentLinearView.addView(footView);
            footView.setMinimumHeight(minHeight_foot);
            footView.setGravity(gravity_foot);
            footView.setBackgroundColor(color_foot);
            footView.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams layoutParams_button;
            if (gravity_foot == Gravity.CENTER) {
                layoutParams_button = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                //新建一个Drawable对象
                QDividerDrawable drawable = new QDividerDrawable(DividerGravity.TOP);
                drawable.setmStrokeColors(this.lineColor);
                footView.setBackground(drawable);
            } else {
                layoutParams_button = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            for (int i = 0; i < actionButtons.size(); i++) {
                final ActionButton actionButton = actionButtons.get(i);
                TextView button = new TextView(this);
                button.setText(actionButton.getText());
                button.setTextSize(text_size_foot);
                button.setTextColor(text_color_foot);
                button.setPadding(actionPadding * 3, (int) (actionPadding * 2), actionPadding * 3, (int) (actionPadding * 2));
                button.setGravity(Gravity.CENTER);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    //获取selectableItemBackground中对应的attrId
                    TypedValue typedValue = new TypedValue();
                    getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);

                    int[] attribute = new int[]{android.R.attr.selectableItemBackground};
                    TypedArray typedArray = getTheme().obtainStyledAttributes(typedValue.resourceId, attribute);
                    button.setForeground(typedArray.getDrawable(0));
                    typedArray.recycle();
                }
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (actionButton.getOnClickListener() != null) {
                            ((QdDialogActivity.OnClickListener)actionButton.getOnClickListener()).onClick(DialogActivity.this,null);
                        } else {
                            finish();
                        }
                    }
                });

                //button.setBackgroundDrawable(null);
                footView.addView(button, layoutParams_button);
                if (i != actionButtons.size() - 1 && gravity_foot == Gravity.CENTER) {
                    View centerLineView = new View(this);
                    LinearLayout.LayoutParams layoutParams_line = new LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT);
                    centerLineView.setLayoutParams(layoutParams_line);
                    centerLineView.setBackgroundColor(lineColor);
                    footView.addView(centerLineView);
                }
            }
        }

        viewParent.addView(contentLinearView,layoutParams);
    }

    boolean cancelable = true;
    public void setCancelable(boolean flag) {
        cancelable = flag;
    }
    boolean canceledOnTouchOutside = true;
    public void setCanceledOnTouchOutside(boolean cancel) {
        canceledOnTouchOutside = cancel;
    }

    private void addTextView(LinearLayout viewGroup, String title, int color, int textSize) {
        if (title != null) {
            TextView textView = new TextView(this);
            textView.setText(title);
            textView.setTextColor(color);
            textView.setTextSize(textSize);
            //textView.setPadding(padding, padding, padding, padding);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1);
            switch ((int) viewGroup.getTag()) {
                case Gravity.LEFT:
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    break;
                case Gravity.CENTER:
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                    break;
                case Gravity.RIGHT:
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    break;
            }
            textView.setLayoutParams(layoutParams);
            //
            textView.setGravity((int) viewGroup.getTag());
            viewGroup.addView(textView);
        }
    }

    private void addBodyTextView(LinearLayout viewGroup, String title, int color, int textSize) {
        addTextView(viewGroup, title, color, textSize);
    }

}