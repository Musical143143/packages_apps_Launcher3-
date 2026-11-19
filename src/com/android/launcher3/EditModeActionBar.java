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

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

        addAction(R.drawable.ic_edit_mode_widgets, R.string.widget_button_text,
                v -> mLauncher.openWidgetsFullSheet());
        addAction(R.drawable.ic_edit_mode_wallpaper, R.string.edit_mode_wallpaper,
                v -> openWallpaperPicker());
        addAction(R.drawable.ic_edit_mode_layout, R.string.edit_mode_layout,
                v -> openLayoutPicker());
        addAction(R.drawable.ic_edit_mode_settings, R.string.edit_mode_home_settings,
                v -> openHomeSettings());
        addAction(R.drawable.ic_edit_mode_done, R.string.edit_mode_done,
                v -> mLauncher.getStateManager().goToState(LauncherState.NORMAL, true));
    }

    /** Layout params for attaching this bar to the bottom of the drag layer. */
    FrameLayout.LayoutParams layoutParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        params.setMargins(dp(16), 0, dp(16), dp(32));
        return params;
    }

    private boolean mEditMode;
    private boolean mBatchToolbarVisible;
    private boolean mDropTargetsVisible;

    /** Shows the bar only in edit mode, and steps aside for the selection toolbar or drag targets. */
    void onStateChanged(LauncherState state) {
        mEditMode = state == LauncherState.EDIT_MODE;
        refreshVisibility();
    }

    void setBatchToolbarVisible(boolean visible) {
        mBatchToolbarVisible = visible;
        refreshVisibility();
    }

    void setDropTargetsVisible(boolean visible) {
        mDropTargetsVisible = visible;
        refreshVisibility();
    }

    private void refreshVisibility() {
        setVisibility(mEditMode && !mBatchToolbarVisible && !mDropTargetsVisible
                ? VISIBLE : GONE);
    }

    private void addAction(int iconRes, int labelRes, View.OnClickListener onClick) {
        LinearLayout action = new LinearLayout(getContext());
        action.setOrientation(VERTICAL);
        action.setGravity(Gravity.CENTER);
        action.setPadding(dp(6), dp(10), dp(6), dp(10));
        action.setClickable(true);
        action.setFocusable(true);
        action.setOnClickListener(onClick);

        ImageView icon = new ImageView(getContext());
        icon.setImageResource(iconRes);
        action.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        TextView label = new TextView(getContext());
        label.setText(labelRes);
        label.setTextColor(Color.WHITE);
        label.setTextSize(12);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams =
                new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        labelParams.topMargin = dp(4);
        action.addView(label, labelParams);

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

    /** Layout picker with a live preview of the chosen grid; the grid applies on Apply. */
    private void openLayoutPicker() {
        final String[] gridNames = {"4_by_6", "5_by_6", "5_by_7"};
        final int[] labels = {
                R.string.edit_mode_layout_4_by_6,
                R.string.edit_mode_layout_5_by_6,
                R.string.edit_mode_layout_5_by_7,
        };
        // Columns and rows of each grid, matching the names in device_profiles.xml.
        final int[] columns = {4, 5, 5};
        final int[] rows = {6, 6, 7};
        String current = LauncherPrefs.get(getContext()).get(LauncherPrefs.GRID_NAME);
        int checked = 0;
        for (int i = 0; i < gridNames.length; i++) {
            if (gridNames[i].equals(current)) {
                checked = i;
            }
        }
        final int[] selection = {checked};

        Context context = getContext();
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(24);
        content.setPadding(padding, dp(8), padding, 0);

        LayoutPreview preview = new LayoutPreview(context);
        preview.setGrid(columns[checked], rows[checked]);
        content.addView(preview, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(200)));

        RadioGroup group = new RadioGroup(context);
        for (int i = 0; i < gridNames.length; i++) {
            final int index = i;
            RadioButton button = new RadioButton(context);
            button.setId(View.generateViewId());
            button.setText(labels[i]);
            button.setChecked(i == checked);
            button.setOnClickListener(v -> {
                selection[0] = index;
                preview.setGrid(columns[index], rows[index]);
            });
            group.addView(button);
        }
        content.addView(group);

        new AlertDialog.Builder(mLauncher)
                .setTitle(R.string.edit_mode_layout)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.edit_mode_layout_apply, (dialog, which) ->
                        applyLayout(gridNames[selection[0]]))
                .show();
    }

    /** Saves the chosen grid; it takes effect when the launcher restarts. */
    private void applyLayout(String gridName) {
        if (gridName.equals(LauncherPrefs.get(getContext()).get(LauncherPrefs.GRID_NAME))) {
            return;
        }
        LauncherPrefs.get(getContext()).put(LauncherPrefs.GRID_NAME, gridName);
        LauncherAppState.INSTANCE.executeIfCreated(app -> app.setNeedsRestart());
    }

    private void openHomeSettings() {
        Intent intent = new Intent(getContext(), SettingsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
    }

    /** Draws a grid as rounded cells, so the picked layout is visible before Apply. */
    private static final class LayoutPreview extends View {
        private final Paint mCell = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int mColumns = 4;
        private int mRows = 6;

        LayoutPreview(Context context) {
            super(context);
            mCell.setColor(Color.argb(110, 128, 128, 128));
        }

        void setGrid(int columns, int rows) {
            mColumns = columns;
            mRows = rows;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float gap = getResources().getDisplayMetrics().density * 6;
            float cellWidth = (getWidth() - gap * (mColumns + 1)) / mColumns;
            float cellHeight = (getHeight() - gap * (mRows + 1)) / mRows;
            if (cellWidth <= 0 || cellHeight <= 0) {
                return;
            }
            float radius = Math.min(cellWidth, cellHeight) * 0.25f;
            for (int row = 0; row < mRows; row++) {
                for (int column = 0; column < mColumns; column++) {
                    float left = gap + column * (cellWidth + gap);
                    float top = gap + row * (cellHeight + gap);
                    canvas.drawRoundRect(left, top, left + cellWidth, top + cellHeight,
                            radius, radius, mCell);
                }
            }
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
