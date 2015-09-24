package com.example.vidmantas.transformingfab;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * @author Vidmantas Kerbelis (vkerbelis@yahoo.com) on 2015-09-24.
 */
public class TransformingActionButton extends FrameLayout {

    private float mElevation;
    private float mPressedTranslation;

    public TransformingActionButton(Context context) {
        this(context, null);
    }

    public TransformingActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TransformingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
        setUpFloatingActionButton(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        this.setClipToPadding(false);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TransformingActionButton);
        mElevation = a.getDimension(R.styleable.TransformingActionButton_buttonElevation, 0);
        mPressedTranslation = a.getDimension(R.styleable.TransformingActionButton_buttonPressedTranslation, 0);
        int padding = Math.round(mElevation * 2);
        this.setPadding(padding, padding, padding, padding);
        a.recycle();
    }

    private void setUpFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        FloatingActionButton actionButton = new FloatingActionButton(context, attrs, defStyleAttr);
        actionButton.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        ViewCompat.setElevation(actionButton, mElevation);
        this.addView(actionButton, 0);
    }

}
