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

package com.android.launcher3;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.launcher3.settings.SettingsActivity;

/**
 * Bottom action bar shown in home screen edit mode: Widgets, Wallpaper & style, Layout,
 * Home screen settings and Done. Each action reuses an existing launcher path.
 */
final class EditModeActionBar extends LinearLayout {

    private final Launcher mLauncher;

    EditModeActionBar(Launcher launcher) {
        super(launcher);
        mLauncher = launcher;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(200, 24, 24, 24));
        background.setCornerRadius(dp(28));
        setBackground(background);
        setPadding(dp(8), dp(8), dp(8), dp(8));
        setVisibility(GONE);

        addAction(R.string.widget_button_text, v -> mLauncher.openWidgetsFullSheet());
        addAction(R.string.edit_mode_wallpaper, v -> openWallpaperPicker());
        addAction(R.string.edit_mode_layout, v -> openHomeSettings());
        addAction(R.string.edit_mode_home_settings, v -> openHomeSettings());
        addAction(R.string.edit_mode_done, v -> mLauncher.getStateManager().goToState(
                LauncherState.NORMAL, true));
    }

    /** Layout params for attaching this bar to the bottom of the drag layer. */
    FrameLayout.LayoutParams layoutParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        params.setMargins(dp(16), 0, dp(16), dp(32));
        return params;
    }

    /** Shows the bar only while the launcher is in edit mode. */
    void onStateChanged(LauncherState state) {
        setVisibility(state == LauncherState.EDIT_MODE ? VISIBLE : GONE);
    }

    private void addAction(int labelRes, View.OnClickListener onClick) {
        TextView action = new TextView(getContext());
        action.setText(labelRes);
        action.setTextColor(Color.WHITE);
        action.setTextSize(12);
        action.setGravity(Gravity.CENTER);
        action.setPadding(dp(6), dp(10), dp(6), dp(10));
        action.setClickable(true);
        action.setFocusable(true);
        action.setOnClickListener(onClick);
        addView(action, new LayoutParams(0, WRAP_CONTENT, 1f));
    }

    private void openWallpaperPicker() {
        Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            mLauncher.startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            // No wallpaper picker installed; nothing to open.
        }
    }

    private void openHomeSettings() {
        Intent intent = new Intent(getContext(), SettingsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
