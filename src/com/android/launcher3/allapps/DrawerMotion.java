/*
 * Copyright (C) 2026 VoltageOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.allapps;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.animation.DecelerateInterpolator;

import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.android.launcher3.LauncherPrefs;

/**
 * Shared motion engine for the app drawer. Every drawer section (search bar, category page, A-Z
 * index and so on) animates a single value through {@link #run}, so they all follow the same
 * rules:
 *
 * <ul>
 *   <li><b>Enhanced</b> (the "Animation effect" setting, same flag as ColorOS app animations):
 *       a damped spring with a little bounce, driven by velocity.</li>
 *   <li><b>Standard</b>: a short ease-out tween with no overshoot.</li>
 * </ul>
 */
public final class DrawerMotion {

    /** Receives each intermediate value of an animation. */
    public interface Listener {
        void onValue(float value);
    }

    private static final float SPRING_STIFFNESS = 380f;
    private static final float SPRING_DAMPING_RATIO = 0.85f;
    private static final long TWEEN_DURATION_MS = 240;

    private DrawerMotion() {
    }

    /**
     * Animates a value from {@code from} to {@code to}, calling {@code listener} with each step.
     * The final call is always {@code to} unless the animation is cancelled.
     *
     * @return a handle that cancels the animation when run
     */
    public static Runnable run(Context context, float from, float to, Listener listener) {
        if (LauncherPrefs.COS_APP_ANIMATIONS.get(context)) {
            return spring(from, to, listener);
        }
        return tween(from, to, listener);
    }

    private static Runnable spring(float from, float to, Listener listener) {
        SpringAnimation animation = new SpringAnimation(new FloatValueHolder(from));
        animation.setSpring(new SpringForce(to)
                .setStiffness(SPRING_STIFFNESS)
                .setDampingRatio(SPRING_DAMPING_RATIO));
        animation.addUpdateListener((a, value, velocity) -> listener.onValue(value));
        animation.addEndListener((a, canceled, value, velocity) -> {
            if (!canceled) {
                listener.onValue(to);
            }
        });
        animation.start();
        return animation::cancel;
    }

    private static Runnable tween(float from, float to, Listener listener) {
        ValueAnimator animator = ValueAnimator.ofFloat(from, to);
        animator.setDuration(TWEEN_DURATION_MS);
        animator.setInterpolator(new DecelerateInterpolator(1.5f));
        animator.addUpdateListener(a -> listener.onValue((float) a.getAnimatedValue()));
        animator.start();
        return animator::cancel;
    }
}
