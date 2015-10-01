package com.example.vidmantas.transformingfab;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vidmantas Kerbelis (vkerbelis@yahoo.com) on 2015-09-24.
 */
public class TransformingActionButtonLayout extends CoordinatorLayout implements View.OnClickListener {

    private static final String TAG = "TABL";
    private static final long ANIMATION_DURATION = 500;
    private static final long ANIMATION_DELAY = 200;
    private float mElevation;
    private float mPressedTranslation;
    private View mRevealView;
    private int mGravity;
    private int mRevealWidth;
    private int mRevealHeight;
    private int mActionButtonWidth;
    private int mActionButtonHeight;
    private int mActionButtonColor;

    public TransformingActionButtonLayout(Context context) {
        this(context, null);
    }

    public TransformingActionButtonLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TransformingActionButtonLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        this.setClipToPadding(false);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TransformingActionButtonLayout);
        mElevation = a.getDimension(R.styleable.TransformingActionButtonLayout_buttonElevation, 0);
        mPressedTranslation = a.getDimension(R.styleable.TransformingActionButtonLayout_buttonPressedTranslation, 0);
        mGravity = a.getInteger(R.styleable.TransformingActionButtonLayout_buttonGravity, 0);
        a.recycle();
    }

    public void setRevealView(View view) {
        this.mRevealView = view;
        for (int i = 0; i < this.getChildCount(); i++) {
            View child = this.getChildAt(i);
            if (child instanceof FloatingActionButton) {
                child.setOnClickListener(this);
            }
        }
    }

    @Override
    public void onClick(final View view) {
        if (mRevealView != null) {
            mRevealView.setVisibility(INVISIBLE);
            view.setAlpha(1.0f);
            if (!mRevealView.isAttachedToWindow()) {
                ViewCompat.setElevation(mRevealView, mElevation);
                this.addView(mRevealView);
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mRevealView.getLayoutParams();
                params.setBehavior(new RevealViewBehavior());
                mActionButtonColor = view.getBackgroundTintList().getDefaultColor();
                mActionButtonWidth = view.getWidth();
                mActionButtonHeight = view.getHeight();
                mRevealWidth = params.width;
                mRevealHeight = params.height;
            }
            ValueAnimator animatorX = ValueAnimator.ofFloat(0, -mRevealWidth / 2 + mActionButtonWidth / 2);
            animatorX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    view.setTranslationX((float) animation.getAnimatedValue());
                }
            });
            animatorX.setDuration(ANIMATION_DURATION + ANIMATION_DELAY);
            animatorX.start();

            ValueAnimator animatorY = ValueAnimator.ofFloat(0, -mRevealHeight / 2 + mActionButtonHeight / 2);
            animatorY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    view.setTranslationY((float) animation.getAnimatedValue());
                }
            });
            animatorY.setDuration(ANIMATION_DURATION + ANIMATION_DELAY);
            animatorY.start();

            Log.d(TAG, "onClick dimensions: " + mRevealWidth + "x" + mRevealHeight);

            final int cx = mRevealWidth / 2;
            final int cy = mRevealHeight / 2;
            final int endRadius = (int) Math.max(mRevealWidth * 1.3, mRevealHeight * 1.3) / 2;
            final int radius = Math.max(mActionButtonWidth, mActionButtonHeight) / 2;

            Animator circularAnimator = ViewAnimationUtils.createCircularReveal(mRevealView, cx, cy, radius, endRadius);
            circularAnimator.setDuration(ANIMATION_DURATION);
            circularAnimator.setStartDelay(ANIMATION_DELAY);
            circularAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    view.setVisibility(View.INVISIBLE);
                    mRevealView.setVisibility(View.VISIBLE);
                }
            });
            circularAnimator.start();

            ValueAnimator backgroundAnimator = ValueAnimator.ofObject(new ArgbEvaluator(),
                    mActionButtonColor, ((ColorDrawable) mRevealView.getBackground()).getColor());
            backgroundAnimator.setDuration(ANIMATION_DELAY);
            backgroundAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    view.setBackgroundTintList(ColorStateList.valueOf((int)animation.getAnimatedValue()));
                }
            });
            backgroundAnimator.start();

        }
    }

    public static class RevealViewBehavior extends Behavior<View> {

        public RevealViewBehavior() {
        }

        @Override
        public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
            return dependency instanceof FloatingActionButton;
        }

        @Override
        public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
            if(dependency instanceof FloatingActionButton) {
                this.updateTranslation(parent, child, dependency);
            }
            return false;
        }

        private void updateTranslation(CoordinatorLayout parent, View child, View dependency) {
            child.setTranslationX(dependency.getTranslationX());
            child.setTranslationY(dependency.getTranslationY());
        }

        @Override
        public boolean onLayoutChild(CoordinatorLayout parent, View child, int layoutDirection) {
            List dependencies = parent.getDependencies(child);
            int i = 0;
            for(int count = dependencies.size(); i < count; ++i) {
                View dependency = (View) dependencies.get(i);
                if (dependency instanceof FloatingActionButton) {
                    int childWidth;
                    int childHeight;
                    ViewGroup.LayoutParams params = child.getLayoutParams();
                    if (params.width > 0 && params.height > 0) {
                        childWidth = params.width;
                        childHeight = params.height;
                    } else {
                        child.measure(0, 0);
                        childWidth = child.getMeasuredWidth();
                        childHeight = child.getMeasuredHeight();
                    }
                    dependency.measure(0, 0);
                    int dependencyWidth = dependency.getMeasuredWidth();
                    int dependencyHeight = dependency.getMeasuredHeight();
                    child.setRight(dependency.getRight() + (childWidth / 2) - (dependencyWidth / 2));
                    child.setLeft(child.getRight() - childWidth);
                    child.setBottom(dependency.getBottom() + (childHeight / 2) - (dependencyHeight / 2));
                    child.setTop(child.getBottom() - childHeight);
                    break;
                }
            }
            return true;
        }
    }
}
