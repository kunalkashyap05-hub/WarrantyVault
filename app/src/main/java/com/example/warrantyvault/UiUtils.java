package com.example.warrantyvault;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class UiUtils {

    /**
     * Enables edge-to-edge layout on an activity and applies top padding for status bar insets.
     */
    public static void setupEdgeToEdge(Activity activity, View headerView) {
        if (activity == null) return;

        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
        boolean isDark = (activity.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        controller.setAppearanceLightStatusBars(!isDark);
        controller.setAppearanceLightNavigationBars(!isDark);

        if (headerView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(headerView, (v, insets) -> {
                int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                v.setPadding(v.getPaddingLeft(), statusBarHeight + 12, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
    }

    /**
     * Adds press-scale spring touch animation (shrink on press, scale back on release) to any View.
     */
    @SuppressLint("ClickableViewAccessibility")
    public static void applyPressScaleAnimation(View view) {
        if (view == null) return;

        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    ObjectAnimator scaleXDown = ObjectAnimator.ofFloat(v, "scaleX", 0.96f);
                    ObjectAnimator scaleYDown = ObjectAnimator.ofFloat(v, "scaleY", 0.96f);
                    AnimatorSet downSet = new AnimatorSet();
                    downSet.playTogether(scaleXDown, scaleYDown);
                    downSet.setDuration(120);
                    downSet.setInterpolator(new DecelerateInterpolator());
                    downSet.start();
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(v, "scaleX", 1.0f);
                    ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(v, "scaleY", 1.0f);
                    AnimatorSet upSet = new AnimatorSet();
                    upSet.playTogether(scaleXUp, scaleYUp);
                    upSet.setDuration(220);
                    upSet.setInterpolator(new OvershootInterpolator(1.2f));
                    upSet.start();
                    break;
            }
            return false; // allow click listener to receive event
        });
    }

    /**
     * Card long-press spatial 3D lift animation with haptic feedback and Z-elevation.
     */
    public static void applyLongPressLiftAnimation(View view, Runnable onLongClick) {
        if (view == null) return;

        view.setOnLongClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

            ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1.03f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1.03f);
            ObjectAnimator transZ = ObjectAnimator.ofFloat(v, "translationZ", 20f);
            AnimatorSet liftSet = new AnimatorSet();
            liftSet.playTogether(scaleX, scaleY, transZ);
            liftSet.setDuration(160);
            liftSet.setInterpolator(new OvershootInterpolator(1.2f));
            liftSet.start();

            v.postDelayed(() -> {
                ObjectAnimator scaleXReset = ObjectAnimator.ofFloat(v, "scaleX", 1.0f);
                ObjectAnimator scaleYReset = ObjectAnimator.ofFloat(v, "scaleY", 1.0f);
                ObjectAnimator transZReset = ObjectAnimator.ofFloat(v, "translationZ", 0f);
                AnimatorSet resetSet = new AnimatorSet();
                resetSet.playTogether(scaleXReset, scaleYReset, transZReset);
                resetSet.setDuration(220);
                resetSet.start();
            }, 320);

            if (onLongClick != null) {
                onLongClick.run();
            }
            return true;
        });
    }

    /**
     * Smoothly animates a integer counter from startValue to endValue in a TextView.
     */
    public static void animateCountUp(TextView textView, int startValue, int endValue, long durationMs) {
        if (textView == null) return;

        ValueAnimator animator = ValueAnimator.ofInt(startValue, endValue);
        animator.setDuration(durationMs);
        animator.setInterpolator(new DecelerateInterpolator(1.5f));
        animator.addUpdateListener(animation -> {
            int currentVal = (int) animation.getAnimatedValue();
            textView.setText(String.valueOf(currentVal));
        });
        animator.start();
    }

    /**
     * Slide up and scale entrance animation simulating a glass panel zooming in from spatial depth.
     */
    public static void animateEntrance(View view, int delayMs) {
        if (view == null) return;

        view.setAlpha(0f);
        view.setScaleX(0.93f);
        view.setScaleY(0.93f);
        view.setTranslationY(30f);
        view.animate()
                .alpha(1f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .translationY(0f)
                .setStartDelay(delayMs)
                .setDuration(350)
                .setInterpolator(new OvershootInterpolator(1.1f))
                .start();
    }

    /**
     * Fades a top header bar into a translucent frosted glass background as user scrolls down.
     */
    public static void attachScrollHeaderBehavior(NestedScrollView scrollView, View topHeaderView) {
        if (scrollView == null || topHeaderView == null) return;

        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            float alpha = Math.min(1.0f, (float) scrollY / 120f);
            int bgColor = Color.argb((int) (alpha * 230), 241, 245, 249);
            topHeaderView.setBackgroundColor(bgColor);
            topHeaderView.setElevation(alpha * 12f);
        });
    }

    /**
     * Shrinks ExtendedFAB on scroll down and extends on scroll up in NestedScrollView.
     */
    public static void attachFabScrollBehavior(NestedScrollView scrollView, ExtendedFloatingActionButton fab) {
        if (scrollView == null || fab == null) return;

        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY > oldScrollY + 12 && fab.isExtended()) {
                fab.shrink();
            } else if (scrollY < oldScrollY - 12 && !fab.isExtended()) {
                fab.extend();
            }
        });
    }

    /**
     * Shrinks ExtendedFAB on scroll down and extends on scroll up in RecyclerView.
     */
    public static void attachFabScrollBehavior(RecyclerView recyclerView, ExtendedFloatingActionButton fab) {
        if (recyclerView == null || fab == null) return;

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 10 && fab.isExtended()) {
                    fab.shrink();
                } else if (dy < -10 && !fab.isExtended()) {
                    fab.extend();
                }
            }
        });
    }
}
