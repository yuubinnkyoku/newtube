package com.newtube.mobile.ui.common;

import android.graphics.Path;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

/**
 * NEWTUBE(motion): the app's motion tokens - Material 3 easing curves and the few durations the
 * phone UI shares, so every screen moves the same way. Things that arrive decelerate (EMPHASIZED_
 * DECELERATE), things that leave accelerate (EMPHASIZED_ACCELERATE), things that move from one
 * resting place to another use EMPHASIZED, and small fades use STANDARD. Nothing here is slower than
 * what it replaced: the point of the curves is that the first frames move a lot, so a transition
 * reads as fast even at 250-300 ms.
 */
public final class Motion {
    // M3 Expressive spring tokens (Compose token set mirrored by MDC 1.14 Views resources).
    // Spatial properties may overshoot; effects (alpha/color) are critically damped.
    public static final float EXPRESSIVE_FAST_SPATIAL_STIFFNESS = 800f;
    public static final float EXPRESSIVE_FAST_SPATIAL_DAMPING = 0.6f;
    public static final float EXPRESSIVE_DEFAULT_SPATIAL_STIFFNESS = 380f;
    public static final float EXPRESSIVE_DEFAULT_SPATIAL_DAMPING = 0.8f;
    public static final float EXPRESSIVE_SLOW_SPATIAL_STIFFNESS = 200f;
    public static final float EXPRESSIVE_SLOW_SPATIAL_DAMPING = 0.8f;
    public static final float EXPRESSIVE_FAST_EFFECTS_STIFFNESS = 3800f;
    public static final float EXPRESSIVE_DEFAULT_EFFECTS_STIFFNESS = 1600f;
    public static final float EXPRESSIVE_SLOW_EFFECTS_STIFFNESS = 800f;
    public static final float EXPRESSIVE_EFFECTS_DAMPING = 1f;

    /** Legacy M3 easing, retained only for window/effect animations that cannot use a spring. */
    public static final Interpolator EMPHASIZED = new PathInterpolator(emphasizedPath());
    public static final Interpolator EMPHASIZED_DECELERATE = new PathInterpolator(0.05f, 0.7f, 0.1f, 1f);
    public static final Interpolator EMPHASIZED_ACCELERATE = new PathInterpolator(0.3f, 0f, 0.8f, 0.15f);
    public static final Interpolator STANDARD = new PathInterpolator(0.2f, 0f, 0f, 1f);
    public static final Interpolator STANDARD_DECELERATE = new PathInterpolator(0f, 0f, 0f, 1f);
    public static final Interpolator STANDARD_ACCELERATE = new PathInterpolator(0.3f, 0f, 1f, 1f);

    /** Content that swaps in place (text, counts, a list replacing its skeleton). */
    public static final long FADE_IN_MS = 150;
    /** Content leaving before other content takes its place (the first half of a fade-through). */
    public static final long FADE_OUT_MS = 90;
    /** A surface arriving (a sheet sliding up). */
    public static final long ENTER_MS = 250;
    /** A surface leaving (a sheet sliding down). */
    public static final long EXIT_MS = 200;

    /** The like "pop": a quick press-in, overshoot and settle (the comments' thumb uses the same). */
    public static final long POP_MS = 260;

    private Motion() {
    }

    /**
     * NEWTUBE(m3e): a small action uses the official fast-spatial expressive spring. Press-in is
     * immediate; the spring owns only geometry on the way back. Alpha/color never borrow this spec.
     */
    public static void pop(android.view.View view) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        view.setScaleX(0.86f);
        view.setScaleY(0.86f);
        androidx.dynamicanimation.animation.SpringAnimation sx =
                new androidx.dynamicanimation.animation.SpringAnimation(
                        view, androidx.dynamicanimation.animation.DynamicAnimation.SCALE_X, 1f);
        androidx.dynamicanimation.animation.SpringAnimation sy =
                new androidx.dynamicanimation.animation.SpringAnimation(
                        view, androidx.dynamicanimation.animation.DynamicAnimation.SCALE_Y, 1f);
        sx.getSpring().setStiffness(EXPRESSIVE_FAST_SPATIAL_STIFFNESS)
                .setDampingRatio(EXPRESSIVE_FAST_SPATIAL_DAMPING);
        sy.getSpring().setStiffness(EXPRESSIVE_FAST_SPATIAL_STIFFNESS)
                .setDampingRatio(EXPRESSIVE_FAST_SPATIAL_DAMPING);
        sx.start();
        sy.start();
    }

    /**
     * NEWTUBE(haptics): a damped spring from one value to another, solved in closed form and played
     * as an interpolator, so an ordinary ValueAnimator runs it (its cancel and end handling stay as
     * they are). This is how the Pixel settles what a finger lets go of - the launcher's recents
     * flick and SystemUI's notification rows are springs that start at the finger's speed, and a
     * spring below critical damping lands with a small settle past its target.
     *
     * <p>{@link #durationMs} is when the spring stays within {@code precision} of its target.</p>
     */
    public static final class Spring implements Interpolator {
        public final long durationMs;
        private final float mFrom;
        private final float mTo;
        private final double mX0;
        private final double mV0;
        private final double mOmega;
        private final double mZeta;

        /** {@code velocity}: units per second, positive in the direction of increasing value. */
        public Spring(float from, float to, float velocity, float stiffness, float dampingRatio,
                float precision) {
            mFrom = from;
            mTo = to;
            mX0 = from - to;
            mV0 = velocity;
            mOmega = Math.sqrt(stiffness);
            mZeta = dampingRatio;
            long settled = 0;
            if (Math.abs(to - from) > 1e-6f) {
                for (int ms = 1; ms <= 2000; ms++) {
                    if (Math.abs(offset(ms / 1000.0)) > precision) {
                        settled = ms;
                    }
                }
            }
            durationMs = settled;
        }

        /** Displacement from the target after {@code t} seconds. */
        private double offset(double t) {
            if (mZeta < 1.0) {
                double damped = mOmega * Math.sqrt(1.0 - mZeta * mZeta);
                return Math.exp(-mZeta * mOmega * t) * (mX0 * Math.cos(damped * t)
                        + (mV0 + mZeta * mOmega * mX0) / damped * Math.sin(damped * t));
            }
            return (mX0 + (mV0 + mOmega * mX0) * t) * Math.exp(-mOmega * t);
        }

        @Override
        public float getInterpolation(float input) {
            if (input >= 1f || durationMs == 0) {
                return 1f;
            }
            double value = mTo + offset(input * durationMs / 1000.0);
            return (float) ((value - mFrom) / (mTo - mFrom));
        }
    }

    private static Path emphasizedPath() {
        Path path = new Path();
        path.moveTo(0f, 0f);
        path.cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f);
        path.cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f);
        return path;
    }
}
