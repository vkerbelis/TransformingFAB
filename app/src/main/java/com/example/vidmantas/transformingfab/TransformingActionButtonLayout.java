package com.example.vidmantas.transformingfab;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;

import java.util.List;

/**
 * @author Vidmantas Kerbelis (vkerbelis@yahoo.com) on 2015-09-24.
 */
public class TransformingActionButtonLayout extends CoordinatorLayout implements View.OnClickListener {

    private static final String TAG = "TABL";
    private static final long ANIMATION_DURATION = 5000;
    private static final long ANIMATION_DELAY = 2000;
    private static final float ANIMATION_SPEED_MULTIPLIER = 5;
    private float mElevation;
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
        mGravity = a.getInteger(R.styleable.TransformingActionButtonLayout_buttonGravity, 0);
        a.recycle();
    }

    /**
     * NOTE: This method overrides the action button's previous onClickListener.
     *
     * @param view the view that will be revealed on FAB click.
     */
    public void setRevealView(View view) {
        this.mRevealView = view;
        FloatingActionButton actionButton = (FloatingActionButton) findActionButton();
        ViewCompat.setElevation(actionButton, mElevation);
        ViewCompat.setElevation(mRevealView, mElevation);
        setRevealViewLayoutParams();
        actionButton.setOnClickListener(this);
        actionButton.measure(0, 0);
        mActionButtonWidth = actionButton.getMeasuredWidth();
        mActionButtonHeight = actionButton.getMeasuredHeight();
        mActionButtonColor = ViewCompat.getBackgroundTintList(actionButton).getDefaultColor();
    }

    private View findActionButton() {
        View view = null;
        for (int i = 0; i < this.getChildCount(); i++) {
            View child = this.getChildAt(i);
            if (child instanceof FloatingActionButton) {
                view = child;
            }
        }
        return view;
    }

    private void setRevealViewLayoutParams() {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mRevealView.getLayoutParams();
        params.setBehavior(new RevealViewBehavior());
        if (params.width > 0 && params.height > 0) {
            mRevealWidth = params.width;
            mRevealHeight = params.height;
        } else {
            mRevealView.measure(0, 0);
            mRevealWidth = mRevealView.getMeasuredWidth();
            mRevealHeight = mRevealView.getMeasuredHeight();
        }
    }

    private void addRevealViewIfNecessary() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (!mRevealView.isAttachedToWindow()) {
                this.addView(mRevealView);
            }
        } else {
            for (int i = 0; i < this.getChildCount(); i++) {
                View child = this.getChildAt(i);
                if (child.equals(mRevealView)) {
                    return;
                }
            }
            this.addView(mRevealView);
        }
    }

    @Override
    public void onClick(final View view) {
        if (mRevealView != null) {
            mRevealView.setVisibility(INVISIBLE);
            view.setVisibility(View.VISIBLE);
            view.setClickable(false);
            addRevealViewIfNecessary();


//            Path path = new Path();
//            path.arcTo(-mRevealWidth / 2 + mActionButtonWidth / 2 - 200,
//                    -mRevealHeight / 2 + mActionButtonHeight / 2 - 200,
//                    -mRevealWidth / 2 + mActionButtonWidth / 2 + 200,
//                    -mRevealHeight / 2 + mActionButtonHeight / 2 + 200,
//                    360, 270, false);
//            ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, View.TRANSLATION_X,
//                            View.TRANSLATION_Y, path);
//            objectAnimator.setDuration(500);
//            objectAnimator.start();


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

            final int cx = mRevealWidth / 2;
            final int cy = mRevealHeight / 2;
            final int endRadius = (int) Math.max(mRevealWidth * 1.3, mRevealHeight * 1.3) / 2;
            final int radius = Math.max(mActionButtonWidth, mActionButtonHeight) / 2;

            Animator circularAnimator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                circularAnimator = ViewAnimationUtils.createCircularReveal(mRevealView, cx, cy, radius, endRadius);
            } else {
                mRevealView.setAlpha(0f);
                view.setAlpha(1.0f);
                circularAnimator = ValueAnimator.ofFloat(0f, 1f);
                ((ValueAnimator) circularAnimator).addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        mRevealView.setAlpha((float) animation.getAnimatedValue());
                        view.setAlpha(1 - (float) animation.getAnimatedValue() * ANIMATION_SPEED_MULTIPLIER);
                    }
                });
            }
            circularAnimator.setDuration(ANIMATION_DURATION);
            circularAnimator.setStartDelay(ANIMATION_DELAY);
            circularAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        view.setVisibility(View.INVISIBLE);
                    }
                    mRevealView.setVisibility(View.VISIBLE);
                }
            });
            circularAnimator.start();

            ValueAnimator backgroundAnimator = ValueAnimator.ofObject(new ArgbEvaluator(),
                    mActionButtonColor, ((ColorDrawable) mRevealView.getBackground()).getColor()); // ViewCompat.getBackgroundTintList(mRevealView).getDefaultColor()
            backgroundAnimator.setDuration(ANIMATION_DELAY);
            backgroundAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    ViewCompat.setBackgroundTintList(view, ColorStateList.valueOf((int) animation.getAnimatedValue()));
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
            if (dependency instanceof FloatingActionButton) {
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
            for (int count = dependencies.size(); i < count; ++i) {
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
