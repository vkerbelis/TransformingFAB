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

    public static final float DEFAULT_MODAL_ALPHA = 0.5f;
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
    private float mModalAlpha;
    private boolean mAnimationRunning;

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
        setModalAlpha(DEFAULT_MODAL_ALPHA);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        LayoutParams params = ((LayoutParams) findActionButton().getLayoutParams());
        if (mGravity == 0) {
            mGravity = params.anchorGravity;
            if (mGravity == 0) {
                mGravity = Gravity.BOTTOM | Gravity.END;
                setAnchorGravity(params);
            }
        } else {
            setAnchorGravity(params);
        }
    }

    private void setAnchorGravity(LayoutParams params) {
        params.setAnchorId(this.getId());
        params.anchorGravity = mGravity;
    }

    public void setModalAlpha(float alpha) {
        mModalAlpha = alpha;
        if (mModalAlpha > 1) {
            mModalAlpha = 1;
        } else if (mModalAlpha < 0) {
            mModalAlpha = 0;
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
        mRevealViewWrapper.setClickable(true);
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
        mModalView.setAlpha(0f);
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
        if (mRevealView != null && !mAnimationRunning) {
            View actionButton;
            boolean actionButtonClickable;
            boolean modalClickable;
            boolean reveal = true;
            if (view.getId() == mActionButtonId) {
                actionButton = view;
                mRevealViewWrapper.setVisibility(INVISIBLE);
                actionButton.setVisibility(View.VISIBLE);
                actionButtonClickable = false;
                modalClickable = true;
                addRevealViewIfNecessary();
            } else {
                actionButton = findActionButton();
                actionButtonClickable = true;
                modalClickable = false;
                reveal = false;
            }
            actionButton.setClickable(actionButtonClickable);
            mModalView.setClickable(modalClickable);
            startAnimators(actionButton, reveal);
        }
    }

    private void startAnimators(final View view, boolean reveal) {
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
            }
        });
        movementSet.playTogether(animatorX, animatorY);
        movementSet.start();
        if (reveal) {
            revealSet.playTogether(animatorCircular, animatorModal, animatorBackgroundAlpha);
            revealSet.playSequentially(animatorRevealAlpha);
            revealSet.setStartDelay(ANIMATION_DELAY);
        } else {
            revealSet.playTogether(animatorCircular, animatorModal, animatorRevealAlpha);
            revealSet.playSequentially(animatorBackgroundAlpha);
        }
        revealSet.start();
        mAnimationRunning = true;
    }

    private Animator getAnimatorX(final View view, boolean reveal) {
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
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                view.setTranslationX((float) animation.getAnimatedValue());
            }
        });
        animator.setDuration(ANIMATION_DURATION + ANIMATION_DELAY);
        animator.start();
        return animator;
    }

    private Animator getAnimatorY(final View view, boolean reveal) {
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
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                view.setTranslationY((float) animation.getAnimatedValue());
            }
        });
        animator.setDuration(ANIMATION_DURATION + ANIMATION_DELAY);
        animator.start();
        return animator;
    }

    private Animator getAnimatorCircular(final View view, final boolean reveal) {
        final int cx = mRevealWidth / 2;
        final int cy = mRevealHeight / 2;
        int endRadius = Math.max(mActionButtonWidth, mActionButtonHeight) / 2;
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
        animator.setDuration(ANIMATION_DURATION);
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
                    view.setVisibility(View.VISIBLE);
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
