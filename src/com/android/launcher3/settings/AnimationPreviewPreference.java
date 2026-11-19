/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.launcher3.R;
import com.android.launcher3.allapps.DrawerMotion;

/**
 * Two preview cards, Standard and Enhanced, shown above the "Animation effect" list. Tapping a
 * card plays a sample of that motion, so the choice can be felt before it is applied.
 */
public class AnimationPreviewPreference extends Preference {

    private static final int STANDARD = 0;
    private static final int ENHANCED = 1;

    private final Runnable[] mRunning = new Runnable[2];

    public AnimationPreviewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_animation_preview);
        setSelectable(false);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        bindCard(holder.findViewById(R.id.animation_preview_standard),
                holder.findViewById(R.id.animation_preview_dot_standard), STANDARD, false);
        bindCard(holder.findViewById(R.id.animation_preview_enhanced),
                holder.findViewById(R.id.animation_preview_dot_enhanced), ENHANCED, true);
    }

    private void bindCard(View card, View dot, int index, boolean enhanced) {
        if (card == null || dot == null) {
            return;
        }
        card.setOnClickListener(v -> {
            if (mRunning[index] != null) {
                mRunning[index].run();
            }
            mRunning[index] = DrawerMotion.runStyled(enhanced, 0f, 1f, value -> {
                dot.setScaleX(value);
                dot.setScaleY(value);
            });
        });
    }
}
