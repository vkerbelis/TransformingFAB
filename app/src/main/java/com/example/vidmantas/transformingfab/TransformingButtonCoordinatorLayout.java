package com.example.vidmantas.transformingfab;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.IdRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.List;

/**
 * @author Vidmantas Kerbelis (vkerbelis@yahoo.com) on 2015-09-24.
 */
public class TransformingButtonCoordinatorLayout extends CoordinatorLayout implements View.OnClickListener {

    public static final float DEFAULT_MODAL_ALPHA = 0.5f;
    public static final double DEFAULT_SIZE_CONSTRAINT_RATIO = 0.8;
    private static final long ANIMATION_DURATION = 300;
    private static final long ANIMATION_DELAY = 100;
    private static final float ANIMATION_SPEED_MULTIPLIER = 5;
    @IdRes
    private static final int BACKGROUND_FADE_ID = 564;
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
    private float mModalAlpha;
    private boolean mAnimationRunning;
    private boolean firstLaunch = true;
    private boolean mReveal;
    private boolean mCompleteDismiss;

    public TransformingButtonCoordinatorLayout(Context context) {
        this(context, null);
    }

    public TransformingButtonCoordinatorLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TransformingButtonCoordinatorLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TransformingButtonCoordinatorLayout);
        mElevation = a.getDimension(R.styleable.TransformingButtonCoordinatorLayout_buttonElevation, 0);
        if (mElevation == 0) {
            mElevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
        }
        mGravity = a.getInteger(R.styleable.TransformingButtonCoordinatorLayout_buttonGravity, 0);
        a.recycle();
        setModalAlpha(DEFAULT_MODAL_ALPHA);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        LayoutParams params = ((LayoutParams) findActionButton().getLayoutParams());
        if (mGravity == 0) {
            mGravity = params.gravity == 0 ? params.anchorGravity : params.gravity;
            if (mGravity == 0) {
                mGravity = Gravity.BOTTOM | Gravity.END;
                params.gravity = mGravity;
            }
        } else {
            params.gravity = mGravity;
        }
    }

    public void setModalAlpha(float alpha) {
        mModalAlpha = alpha;
        if (mModalAlpha > 1) {
            mModalAlpha = 1;
        } else if (mModalAlpha < 0) {
            mModalAlpha = 0;
        }
    }

    public void setRevealClickListener() {
        findActionButton().setOnClickListener(this);
    }

    /**
     * NOTE: This method overrides the action button's previous onClickListener and
     * the default FloatingActionButton.Behavior.
     *
     * @param view the view that will be revealed on FAB click.
     */
    public void setRevealView(View view) {
        if (mRevealViewWrapper == null) {
            this.mRevealView = view;
            FloatingActionButton actionButton = (FloatingActionButton) findActionButton();
            ((CoordinatorLayout.LayoutParams) actionButton.getLayoutParams()).setBehavior(new FloatingButtonBehavior());
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
    }

    public void setRevealColor(int color) {
        mActionButtonColor = color;
        mBackgroundFadeView.setBackgroundColor(mActionButtonColor);
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
        ViewGroup.LayoutParams params = mRevealView.getLayoutParams();
        if (params.width > 0 && params.height > 0) {
            mRevealWidth = params.width;
            mRevealHeight = params.height;
        } else {
            int constrainedWidth = (int) (this.getWidth() * DEFAULT_SIZE_CONSTRAINT_RATIO);
            int constrainedHeight = (int) (this.getHeight() * DEFAULT_SIZE_CONSTRAINT_RATIO);
            int widthSpec = MeasureSpec.makeMeasureSpec(constrainedWidth, MeasureSpec.AT_MOST);
            int heightSpec = MeasureSpec.makeMeasureSpec(constrainedHeight, MeasureSpec.AT_MOST);
            mRevealView.measure(widthSpec, heightSpec);
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
        CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setBehavior(new RevealViewBehavior(new RevealViewCallback() {
            @Override
            public void setRevealSize(int childWidth, int childHeight) {
                mRevealHeight = childHeight;
                mRevealWidth = childWidth;
                setBackgroundFadeParams();
                if (firstLaunch && !mAnimationRunning) {
                    firstLaunch = false;
                    startAnimators(findActionButton(), mReveal);
                }
            }
        }));
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
        mRevealViewWrapper.setClickable(true);
        mRevealViewWrapper.addView(mRevealView);
        mBackgroundFadeView = new View(getContext());
        mBackgroundFadeView.setId(BACKGROUND_FADE_ID);
        mBackgroundFadeView.setMinimumHeight(mActionButtonHeight);
        mBackgroundFadeView.setBackgroundColor(mActionButtonColor);
        setBackgroundFadeParams();
        mRevealViewWrapper.addView(mBackgroundFadeView);
    }

    private void setBackgroundFadeParams() {
        ViewGroup.LayoutParams fadeParams = new FrameLayout.LayoutParams(mRevealWidth, mRevealHeight);
        mBackgroundFadeView.setLayoutParams(fadeParams);
    }

    private void setUpModalView() {
        mModalView = new View(getContext());
        CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        ViewCompat.setElevation(mModalView, mElevation);
        mModalView.setOnClickListener(this);
        mModalView.setAlpha(0f);
        mModalView.setLayoutParams(params);
        mModalView.setBackgroundColor(Color.BLACK);
        mModalView.setClickable(true);
    }

    public boolean isRevealed() {
        return ViewCompat.isAttachedToWindow(mRevealViewWrapper);
    }

    private void addRevealViewIfNecessary() {
        if (!isRevealed()) {
            this.addView(mModalView);
            this.addView(mRevealViewWrapper);
        }
    }

    @Override
    public void onClick(final View view) {
        if (mRevealView != null && !mAnimationRunning) {
            View actionButton;
            boolean reveal = true;
            if (view.getId() == mActionButtonId) {
                actionButton = view;
                mRevealViewWrapper.setVisibility(INVISIBLE);
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setClickable(false);
                mModalView.setClickable(true);
                addRevealViewIfNecessary();
            } else {
                actionButton = findActionButton();
                reveal = false;
            }
            mReveal = reveal;
            if (!firstLaunch) {
                startAnimators(actionButton, reveal);
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mAnimationRunning || super.onInterceptTouchEvent(ev);
    }

    private void startAnimators(final View view, final boolean reveal) {
        AnimatorSet movementSet = new AnimatorSet();
        AnimatorSet revealSet = new AnimatorSet();
        Animator animatorX = getAnimatorX(view, reveal);
        Animator animatorY = getAnimatorY(view, reveal);
        Animator animatorCircular = getAnimatorCircular(view, reveal);
        Animator animatorModal = getAnimatorModal(reveal);
        Animator animatorRevealAlpha = getAnimatorRevealAlpha(reveal);
        Animator animatorBackgroundAlpha = getAnimatorBackgroundAlpha(reveal);
        movementSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimationRunning = false;
                if (!reveal) {
                    TransformingButtonCoordinatorLayout.this.removeView(mModalView);
                    TransformingButtonCoordinatorLayout.this.removeView(mRevealViewWrapper);
                    view.setClickable(true);
                }
            }
        });
        movementSet.playTogether(animatorX, animatorY);
        movementSet.start();
        if (reveal) {
            if (animatorModal != null) {
                revealSet.playTogether(animatorCircular, animatorModal, animatorBackgroundAlpha);
            } else {
                revealSet.playTogether(animatorCircular, animatorBackgroundAlpha);
            }
            revealSet.playSequentially(animatorRevealAlpha);
            revealSet.setStartDelay(ANIMATION_DELAY);
        } else {
            if (animatorModal != null) {
                revealSet.playTogether(animatorCircular, animatorModal, animatorRevealAlpha);
            } else {
                revealSet.playTogether(animatorCircular, animatorRevealAlpha);
            }
            revealSet.playSequentially(animatorBackgroundAlpha);
        }
        revealSet.start();
        mAnimationRunning = true;
    }

    private Animator getAnimatorX(final View view, final boolean reveal) {
        int xGravity = 1;
        if ((mGravity & Gravity.END) == Gravity.END) {
            xGravity = -1;
        }
        int startPos = xGravity * mRevealWidth / 2 + mActionButtonWidth / 2;
        int endPos = 0;
        if (reveal) {
            endPos = startPos;
            startPos = 0;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(startPos, endPos);
        final int finalStartPos = startPos;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public float additiveValue;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float currentValue = (float) animation.getAnimatedValue() - (reveal ? 0 : finalStartPos);
                float value = currentValue - additiveValue;
                additiveValue += value;
                ViewCompat.setTranslationX(view, view.getTranslationX() + value);
            }
        });
        animator.setDuration(ANIMATION_DURATION + ANIMATION_DELAY);
        animator.start();
        return animator;
    }

    private Animator getAnimatorY(final View view, final boolean reveal) {
        int yGravity = 1;
        if ((mGravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
            yGravity = -1;
        }
        int startPos = yGravity * mRevealHeight / 2 + mActionButtonHeight / 2;
        int endPos = 0;
        if (reveal) {
            endPos = startPos;
            startPos = 0;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(startPos, endPos);
        final int finalStartPos = startPos;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public float additiveValue;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float currentValue = (float) animation.getAnimatedValue() - (reveal ? 0 : finalStartPos);
                float value = currentValue - additiveValue;
                additiveValue += value;
                ViewCompat.setTranslationY(view, view.getTranslationY() + value);
            }
        });
        animator.setDuration(ANIMATION_DURATION + ANIMATION_DELAY);
        animator.start();
        return animator;
    }

    private Animator getAnimatorCircular(final View view, final boolean reveal) {
        final int cx = mRevealWidth / 2;
        final int cy = mRevealHeight / 2;
        int endRadius;
        if (mCompleteDismiss) {
            endRadius = 0;
        } else {
            endRadius = Math.max(mActionButtonWidth, mActionButtonHeight) / 2;
        }
        int radius = (int) (Math.max(mRevealWidth, mRevealHeight) / 1.3);
        if (reveal) {
            endRadius = radius;
            radius = Math.max(mActionButtonWidth, mActionButtonHeight) / 2;
        }
        Animator animator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            animator = ViewAnimationUtils.createCircularReveal(mRevealViewWrapper, cx, cy, radius, endRadius);
        } else {
            mRevealViewWrapper.setAlpha(0f);
            view.setAlpha(1.0f);
            float endAlpha = 0f;
            float startAlpha = 1f;
            if (reveal) {
                endAlpha = startAlpha;
                startAlpha = 0f;
            }
            animator = ValueAnimator.ofFloat(startAlpha, endAlpha);
            ((ValueAnimator) animator).addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mRevealViewWrapper.setAlpha((float) animation.getAnimatedValue());
                    view.setAlpha(1 - (float) animation.getAnimatedValue() * ANIMATION_SPEED_MULTIPLIER);
                }
            });
        }
        long duration = ANIMATION_DURATION;
        if (mCompleteDismiss) {
            duration += ANIMATION_DELAY;
        }
        animator.setDuration(duration);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    view.setVisibility(View.INVISIBLE);
                }
                mRevealViewWrapper.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!reveal) {
                    mRevealViewWrapper.setVisibility(View.INVISIBLE);
                    if (!mCompleteDismiss) {
                        view.setVisibility(View.VISIBLE);
                    } else {
                        mCompleteDismiss = false;
                    }
                }
            }
        });
        return animator;
    }

    private Animator getAnimatorModal(boolean reveal) {
        ValueAnimator animator = null;
        if (mModalAlpha != 0f) {
            float endAlpha = 0f;
            float startAlpha = mModalAlpha;
            if (reveal) {
                endAlpha = startAlpha;
                startAlpha = 0f;
            }
            mModalView.setAlpha(startAlpha);
            animator = ValueAnimator.ofFloat(startAlpha, endAlpha);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mModalView.setAlpha((float) animation.getAnimatedValue());
                }
            });
            animator.setDuration(ANIMATION_DURATION);
        }
        return animator;
    }

    private Animator getAnimatorBackgroundAlpha(boolean reveal) {
        float endAlpha = 1f;
        float startAlpha = 0f;
        if (reveal) {
            endAlpha = startAlpha;
            startAlpha = 1f;
        }
        mBackgroundFadeView.setAlpha(startAlpha);
        ValueAnimator animator = ValueAnimator.ofFloat(startAlpha, endAlpha);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mBackgroundFadeView.setAlpha((float) animation.getAnimatedValue());
            }
        });
        animator.setDuration(ANIMATION_DURATION / 2);
        if (!reveal) {
            animator.setStartDelay(ANIMATION_DELAY);
        }
        return animator;
    }

    private Animator getAnimatorRevealAlpha(boolean reveal) {
        float endAlpha = 0f;
        float startAlpha = 1f;
        if (reveal) {
            endAlpha = startAlpha;
            startAlpha = 0f;
        }
        mRevealView.setAlpha(startAlpha);
        ValueAnimator animator = ValueAnimator.ofFloat(startAlpha, endAlpha);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mRevealView.setAlpha((float) animation.getAnimatedValue());
            }
        });
        animator.setDuration(ANIMATION_DURATION / 2);
        if (reveal) {
            animator.setStartDelay(ANIMATION_DELAY);
        }
        return animator;
    }

    public void dismissActionButton() {
        onClick(mModalView);
    }

    public void dismissActionButtonComplete() {
        mCompleteDismiss = true;
        dismissActionButton();
    }

    interface RevealViewCallback {

        void setRevealSize(int childWidth, int childHeight);

    }

    public static class FloatingButtonBehavior extends Behavior<FloatingActionButton> {

        private float additiveValue;

        public FloatingButtonBehavior() {
        }

        @Override
        public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
            return dependency instanceof Snackbar.SnackbarLayout;
        }

        @Override
        public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
            if (dependency instanceof Snackbar.SnackbarLayout) {
                this.updateTranslation(parent, child);
            }
            return false;
        }

        private void updateTranslation(CoordinatorLayout parent, View child) {
            float translationY = getTranslationForSnackbar(parent, child);
            float value = translationY - additiveValue;
            additiveValue += value;
            ViewCompat.setTranslationY(child, child.getTranslationY() + value);
        }

        private float getTranslationForSnackbar(CoordinatorLayout parent, View view) {
            float minOffset = 0f;
            List dependencies = parent.getDependencies(view);
            int i = 0;
            for (int z = dependencies.size(); i < z; ++i) {
                View child = (View) dependencies.get(i);
                if (child instanceof Snackbar.SnackbarLayout && parent.doViewsOverlap(child, child)) {
                    minOffset = Math.min(minOffset, ViewCompat.getTranslationY(child) - (float) child.getHeight());
                }
            }
            return minOffset;
        }

    }

    private static class RevealViewBehavior extends Behavior<View> {

        private RevealViewCallback listener;
        private int lastWidth;
        private int lastHeight;

        public RevealViewBehavior(RevealViewCallback listener) {
            this.listener = listener;
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
            ViewCompat.setTranslationX(child, dependency.getTranslationX());
            ViewCompat.setTranslationY(child, dependency.getTranslationY());
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
                    int constrainedWidth = (int) (parent.getWidth() * DEFAULT_SIZE_CONSTRAINT_RATIO);
                    int constrainedHeight = (int) (parent.getHeight() * DEFAULT_SIZE_CONSTRAINT_RATIO);
                    int widthSpec = MeasureSpec.makeMeasureSpec(constrainedWidth, MeasureSpec.AT_MOST);
                    int heightSpec = MeasureSpec.makeMeasureSpec(constrainedHeight, MeasureSpec.AT_MOST);
                    if (params.width > 0 && params.height > 0) {
                        childWidth = params.width;
                        childHeight = params.height;
                        if (childWidth > constrainedWidth) {
                            childWidth = constrainedWidth;
                        }
                        if (childHeight > constrainedHeight) {
                            childHeight = constrainedHeight;
                        }
                    } else {
                        child.measure(widthSpec, heightSpec);
                        childWidth = child.getMeasuredWidth();
                        childHeight = child.getMeasuredHeight();
                    }
                    parent.onLayoutChild(child, layoutDirection);
                    dependency.measure(widthSpec, heightSpec);
                    int dependencyWidth = dependency.getMeasuredWidth();
                    int dependencyHeight = dependency.getMeasuredHeight();
                    child.setRight(dependency.getRight() + (childWidth / 2) - (dependencyWidth / 2));
                    child.setLeft(child.getRight() - childWidth);
                    child.setBottom(dependency.getBottom() + (childHeight / 2) - (dependencyHeight / 2));
                    child.setTop(child.getBottom() - childHeight);
                    if (lastWidth != childWidth || lastHeight != childHeight) {
                        if (listener != null) {
                            listener.setRevealSize(childWidth, childHeight);
                        }
                    }
                    lastWidth = childWidth;
                    lastHeight = childHeight;
                    return true;
                }
            }
            return false;
        }
    }
}
