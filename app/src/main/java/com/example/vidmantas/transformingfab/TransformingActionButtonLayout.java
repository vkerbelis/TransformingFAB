package com.example.vidmantas.transformingfab;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.List;

/**
 * @author Vidmantas Kerbelis (vkerbelis@yahoo.com) on 2015-09-24.
 */
public class TransformingActionButtonLayout extends CoordinatorLayout implements View.OnClickListener {

    private static final long ANIMATION_DURATION = 500;
    private static final long ANIMATION_DELAY = 200;
    private static final float ANIMATION_SPEED_MULTIPLIER = 5;
    private float mElevation;
    private int mGravity;
    private int mRevealWidth;
    private int mRevealHeight;
    private int mActionButtonColor;
    private int mActionButtonId;
    private int mActionButtonWidth;
    private int mActionButtonHeight;
    private View mBackgroundFadeView;
    private View mModalView;
    private View mRevealView;
    private ViewGroup mRevealViewWrapper;

    public TransformingActionButtonLayout(Context context) {
        this(context, null);
    }

    public TransformingActionButtonLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TransformingActionButtonLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        this.setClipToPadding(false);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TransformingActionButtonLayout);
        mElevation = a.getDimension(R.styleable.TransformingActionButtonLayout_buttonElevation, 0);
        mGravity = a.getInteger(R.styleable.TransformingActionButtonLayout_buttonGravity, 0);
        a.recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        LayoutParams params = ((LayoutParams) findActionButton().getLayoutParams());
        if (mGravity == 0) {
            mGravity = params.anchorGravity;
        } else {
            params.setAnchorId(this.getId());
            params.anchorGravity = mGravity;
        }
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
        actionButton.setOnClickListener(this);
        actionButton.measure(0, 0);
        mActionButtonId = actionButton.getId();
        mActionButtonWidth = actionButton.getMeasuredWidth();
        mActionButtonHeight = actionButton.getMeasuredHeight();
        mActionButtonColor = ViewCompat.getBackgroundTintList(actionButton).getDefaultColor();
        setRevealViewLayoutParams();
        setUpRevealViewWrapper();
        setUpModalView();
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
        if (params.width > 0 && params.height > 0) {
            mRevealWidth = params.width;
            mRevealHeight = params.height;
        } else {
            mRevealView.measure(0, 0);
            mRevealWidth = mRevealView.getMeasuredWidth();
            mRevealHeight = mRevealView.getMeasuredHeight();
        }
        if (mRevealHeight < mActionButtonHeight) {
            mRevealHeight = mActionButtonHeight;
        }
    }

    private void setUpRevealViewWrapper() {
        mRevealViewWrapper = new FrameLayout(getContext());
        mRevealViewWrapper.setMinimumHeight(mActionButtonHeight);
        CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(mRevealWidth, mRevealHeight);
        params.setBehavior(new RevealViewBehavior());
        ViewCompat.setElevation(mRevealViewWrapper, mElevation);
        mRevealViewWrapper.setLayoutParams(params);
        int background;
        if (mRevealView.getBackground() != null) {
            if (mRevealView.getBackground() instanceof ColorDrawable) {
                background = ((ColorDrawable) mRevealView.getBackground()).getColor();
            } else {
                background = ViewCompat.getBackgroundTintList(mRevealView).getDefaultColor();
            }
        } else {
            background = Color.WHITE;
        }
        mRevealViewWrapper.setBackgroundColor(background);
        mRevealViewWrapper.addView(mRevealView);
        mBackgroundFadeView = new View(getContext());
        mBackgroundFadeView.setMinimumHeight(mActionButtonHeight);
        CoordinatorLayout.LayoutParams fadeParams = new CoordinatorLayout.LayoutParams(mRevealWidth, mRevealHeight);
        mBackgroundFadeView.setBackgroundColor(mActionButtonColor);
        mBackgroundFadeView.setLayoutParams(fadeParams);
        mRevealViewWrapper.addView(mBackgroundFadeView);
    }

    private void setUpModalView() {
        mModalView = new View(getContext());
        CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        ViewCompat.setElevation(mModalView, mElevation);
        mModalView.setOnClickListener(this);
        mModalView.setLayoutParams(params);
        mModalView.setBackgroundColor(Color.BLACK);
        mModalView.setClickable(true);
    }

    private void addRevealViewIfNecessary() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (!ViewCompat.isAttachedToWindow(mRevealViewWrapper)) {
                this.addView(mModalView);
                this.addView(mRevealViewWrapper);
            }
        }
    }

    @Override
    public void onClick(final View view) {
        View actionButton;
        if (mRevealView != null) {
            if (view.getId() == mActionButtonId) {
                actionButton = view;
                mRevealViewWrapper.setVisibility(INVISIBLE);
                view.setVisibility(View.VISIBLE);
                view.setClickable(false);
                addRevealViewIfNecessary();
            } else {
                actionButton = findActionButton();
                mRevealViewWrapper.setVisibility(INVISIBLE);
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setClickable(false);
            }
            startAnimators(actionButton);
        }
    }

    private void startAnimators(final View view) {
        startImmediateAnimators(view);
        startDelayedAnimators(view);
    }

    private void startImmediateAnimators(final View view) {
        int xGravity = 1;
        int yGravity = 1;
        if ((mGravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
            yGravity = -1;
        }
        if ((mGravity & Gravity.END) == Gravity.END) {
            xGravity = -1;
        }
        ValueAnimator animatorX = ValueAnimator.ofFloat(0, xGravity * mRevealWidth / 2 + mActionButtonWidth / 2);
        animatorX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                view.setTranslationX((float) animation.getAnimatedValue());
            }
        });
        animatorX.setDuration(ANIMATION_DURATION + ANIMATION_DELAY);
        animatorX.start();

        ValueAnimator animatorY = ValueAnimator.ofFloat(0, yGravity * mRevealHeight / 2 + mActionButtonHeight / 2);
        animatorY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                view.setTranslationY((float) animation.getAnimatedValue());
            }
        });
        animatorY.setDuration(ANIMATION_DURATION + ANIMATION_DELAY);
        animatorY.start();
    }

    private void startDelayedAnimators(final View view) {
        final int cx = mRevealWidth / 2;
        final int cy = mRevealHeight / 2;
        final int endRadius = (int) (Math.max(mRevealWidth, mRevealHeight) / 1.3);
        final int radius = Math.max(mActionButtonWidth, mActionButtonHeight) / 2;
        Animator circularAnimator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            circularAnimator = ViewAnimationUtils.createCircularReveal(mRevealViewWrapper, cx, cy, radius, endRadius);
        } else {
            mRevealViewWrapper.setAlpha(0f);
            view.setAlpha(1.0f);
            circularAnimator = ValueAnimator.ofFloat(0f, 1f);
            ((ValueAnimator) circularAnimator).addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mRevealViewWrapper.setAlpha((float) animation.getAnimatedValue());
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
                mRevealViewWrapper.setVisibility(View.VISIBLE);
            }
        });
        circularAnimator.start();

        mModalView.setAlpha(0f);
        mRevealView.setAlpha(0f);
        ValueAnimator alphaAnimator = ValueAnimator.ofFloat(0f, 1f);
        alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                mRevealView.setAlpha(value);
                mModalView.setAlpha(value / 2);
            }
        });
        alphaAnimator.setDuration(ANIMATION_DURATION / 2);
        alphaAnimator.setStartDelay(ANIMATION_DELAY + ANIMATION_DURATION / 2);
        alphaAnimator.start();

        mBackgroundFadeView.setAlpha(1f);
        ValueAnimator bgAlphaAnimator = ValueAnimator.ofFloat(1f, 0f);
        bgAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mBackgroundFadeView.setAlpha((float) animation.getAnimatedValue());
            }
        });
        bgAlphaAnimator.setDuration(ANIMATION_DURATION / 2);
        bgAlphaAnimator.setStartDelay(ANIMATION_DELAY);
        bgAlphaAnimator.start();
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
                this.updateTranslation(child, dependency);
            }
            return false;
        }

        private void updateTranslation(View child, View dependency) {
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
                    parent.onLayoutChild(child, layoutDirection);
                    int childWidth;
                    int childHeight;
                    ViewGroup.LayoutParams params = child.getLayoutParams();
                    int widthSpec = MeasureSpec.makeMeasureSpec(parent.getWidth(), MeasureSpec.AT_MOST);
                    int heightSpec = MeasureSpec.makeMeasureSpec(parent.getHeight(), MeasureSpec.AT_MOST);
                    if (params.width > 0 && params.height > 0) {
                        childWidth = params.width;
                        childHeight = params.height;
                    } else {
                        child.measure(widthSpec, heightSpec);
                        childWidth = child.getMeasuredWidth();
                        childHeight = child.getMeasuredHeight();
                    }
                    dependency.measure(widthSpec, heightSpec);
                    int dependencyWidth = dependency.getMeasuredWidth();
                    int dependencyHeight = dependency.getMeasuredHeight();
                    child.setRight(dependency.getRight() + (childWidth / 2) - (dependencyWidth / 2));
                    child.setLeft(child.getRight() - childWidth);
                    child.setBottom(dependency.getBottom() + (childHeight / 2) - (dependencyHeight / 2));
                    child.setTop(child.getBottom() - childHeight);
                    return true;
                }
            }
            return false;
        }
    }
}
