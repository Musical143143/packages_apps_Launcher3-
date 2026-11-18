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

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.R;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.views.ActivityContext;

import java.util.List;
import java.util.function.Consumer;

/**
 * App drawer for {@link AppDrawerStyle#NOTHING}. Shows an "All | Categories" toggle, a
 * "Recently installed" card and folder-style category tiles. Tapping a tile opens the
 * category's apps. In "All" mode this view only shows the toggle and the existing
 * alphabetical list (owned by the container) stays visible underneath.
 */
public class NothingDrawerView extends LinearLayout {

    private static final int RECENT_COUNT = 4;
    private static final int DETAIL_COLUMNS = 4;
    private static final int TILE_APPS = 4;

    private final ActivityContext mActivityContext;
    private final TextView mAllTab;
    private final TextView mCategoriesTab;
    private final FrameLayout mContent;
    private final ScrollView mCategoriesScroll;
    private final LinearLayout mCategoriesColumn;
    private final LinearLayout mDetailPage;
    private final TextView mDetailTitle;
    private final LinearLayout mDetailGrid;

    private boolean mCategoriesMode;
    private List<AppInfo> mApps = List.of();
    private Consumer<Boolean> mModeListener;

    public NothingDrawerView(Context context) {
        super(context);
        mActivityContext = ActivityContext.lookupContext(context);
        setOrientation(VERTICAL);
        setClipChildren(false);

        LinearLayout toggle = new LinearLayout(context);
        toggle.setOrientation(HORIZONTAL);
        toggle.setBackground(CaddyStyle.background(context, true));
        int pad = dp(4);
        toggle.setPadding(pad, pad, pad, pad);
        mAllTab = createTab(R.string.nothing_drawer_all, () -> setCategoriesMode(false));
        mCategoriesTab = createTab(R.string.nothing_drawer_categories,
                () -> setCategoriesMode(true));
        toggle.addView(mAllTab);
        toggle.addView(mCategoriesTab);
        LayoutParams toggleParams = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        toggleParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        toggleParams.topMargin = getResources().getDimensionPixelSize(
                R.dimen.all_apps_header_top_padding);
        addView(toggle, toggleParams);

        mCategoriesColumn = new LinearLayout(context);
        mCategoriesColumn.setOrientation(VERTICAL);
        mCategoriesColumn.setPadding(dp(12), dp(12), dp(12), dp(96));
        mCategoriesScroll = new ScrollView(context);
        mCategoriesScroll.setClipToPadding(false);
        mCategoriesScroll.addView(mCategoriesColumn);

        mDetailTitle = new TextView(context);
        mDetailTitle.setTextSize(28);
        mDetailTitle.setTextColor(CaddyStyle.textColor(context));
        mDetailTitle.setPadding(dp(16), dp(8), dp(16), dp(12));
        mDetailTitle.setOnClickListener(v -> showCategories());
        mDetailGrid = new LinearLayout(context);
        mDetailGrid.setOrientation(VERTICAL);
        mDetailGrid.setPadding(dp(12), 0, dp(12), dp(96));
        ScrollView detailScroll = new ScrollView(context);
        detailScroll.setClipToPadding(false);
        detailScroll.addView(mDetailGrid);
        mDetailPage = new LinearLayout(context);
        mDetailPage.setOrientation(VERTICAL);
        mDetailPage.addView(mDetailTitle);
        mDetailPage.addView(detailScroll, new LayoutParams(MATCH_PARENT, 0, 1f));
        mDetailPage.setVisibility(GONE);

        mContent = new FrameLayout(context);
        mContent.addView(mCategoriesScroll, new FrameLayout.LayoutParams(MATCH_PARENT,
                MATCH_PARENT));
        mContent.addView(mDetailPage, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        addView(mContent, new LayoutParams(MATCH_PARENT, 0, 1f));

        updateTabs();
        mContent.setVisibility(GONE);
    }

    /** Called with {@code true} when Categories is selected, {@code false} for All. */
    public void setOnModeChangedListener(Consumer<Boolean> listener) {
        mModeListener = listener;
    }

    public boolean isCategoriesMode() {
        return mCategoriesMode;
    }

    /** Refreshes the card and tiles from the current app list. */
    public void setApps(List<AppInfo> apps) {
        mApps = apps == null ? List.of() : List.copyOf(apps);
        rebuildCategories();
        if (mDetailPage.getVisibility() == VISIBLE) {
            showCategories();
        }
    }

    public void setCategoriesMode(boolean categories) {
        if (mCategoriesMode == categories) {
            return;
        }
        mCategoriesMode = categories;
        if (!categories) {
            showCategories();
        }
        mContent.setVisibility(categories ? VISIBLE : GONE);
        updateTabs();
        ViewGroup.LayoutParams lp = getLayoutParams();
        if (lp != null) {
            lp.height = categories ? MATCH_PARENT : WRAP_CONTENT;
            setLayoutParams(lp);
        }
        if (mModeListener != null) {
            mModeListener.accept(categories);
        }
    }

    private void rebuildCategories() {
        mCategoriesColumn.removeAllViews();
        if (mApps.isEmpty()) {
            return;
        }
        List<AppInfo> recent = mApps.subList(0, Math.min(RECENT_COUNT, mApps.size()));
        mCategoriesColumn.addView(recentCard(recent));

        List<CaddyAppLibrary.Category> categories =
                CaddyAppLibrary.categories(getContext(), mApps);
        for (int i = 0; i < categories.size(); i += 2) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(HORIZONTAL);
            for (int j = i; j < Math.min(i + 2, categories.size()); j++) {
                row.addView(categoryTile(categories.get(j)), tileParams());
            }
            if (categories.size() - i == 1) {
                row.addView(new View(getContext()), tileParams());
            }
            mCategoriesColumn.addView(row);
        }
    }

    private View recentCard(List<AppInfo> recent) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(CaddyStyle.background(getContext(), false));
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        for (int i = 0; i < RECENT_COUNT; i++) {
            if (i < recent.size()) {
                row.addView(icon(recent.get(i), false), new LayoutParams(0, WRAP_CONTENT, 1f));
            } else {
                row.addView(new View(getContext()), new LayoutParams(0, WRAP_CONTENT, 1f));
            }
        }
        card.addView(row);
        card.addView(label(getContext().getString(R.string.nothing_drawer_recent)));
        LayoutParams params = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.bottomMargin = dp(12);
        card.setLayoutParams(params);
        return card;
    }

    private View categoryTile(CaddyAppLibrary.Category category) {
        LinearLayout tile = new LinearLayout(getContext());
        tile.setOrientation(VERTICAL);
        tile.setPadding(dp(12), dp(12), dp(12), dp(12));
        tile.setBackground(CaddyStyle.background(getContext(), false));
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setOnClickListener(v -> showCategory(category));

        LinearLayout preview = new LinearLayout(getContext());
        preview.setOrientation(VERTICAL);
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(HORIZONTAL);
            for (int c = 0; c < 2; c++) {
                int index = r * 2 + c;
                if (index < category.apps.size() && index < TILE_APPS) {
                    row.addView(icon(category.apps.get(index), true),
                            new LayoutParams(0, WRAP_CONTENT, 1f));
                } else {
                    row.addView(new View(getContext()), new LayoutParams(0, WRAP_CONTENT, 1f));
                }
            }
            preview.addView(row);
        }
        tile.addView(preview);
        tile.addView(label(category.title));
        return tile;
    }

    private void showCategory(CaddyAppLibrary.Category category) {
        mDetailTitle.setText(getContext().getString(R.string.nothing_drawer_back)
                + "  " + category.title);
        mDetailGrid.removeAllViews();
        addAppGrid(mDetailGrid, category.apps);
        mCategoriesScroll.setVisibility(GONE);
        mDetailPage.setVisibility(VISIBLE);
    }

    private void showCategories() {
        mDetailPage.setVisibility(GONE);
        mCategoriesScroll.setVisibility(VISIBLE);
    }

    private void addAppGrid(LinearLayout target, List<AppInfo> apps) {
        for (int i = 0; i < apps.size(); i += DETAIL_COLUMNS) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(HORIZONTAL);
            for (int j = i; j < i + DETAIL_COLUMNS; j++) {
                if (j < apps.size()) {
                    row.addView(icon(apps.get(j), false), new LayoutParams(0, WRAP_CONTENT, 1f));
                } else {
                    row.addView(new View(getContext()), new LayoutParams(0, WRAP_CONTENT, 1f));
                }
            }
            target.addView(row);
        }
    }

    private BubbleTextView icon(AppInfo app, boolean preview) {
        BubbleTextView icon = (BubbleTextView) mActivityContext.getLayoutInflater().inflate(
                preview ? R.layout.caddy_preview_icon : R.layout.caddy_app_icon, this, false);
        icon.applyFromApplicationInfo(app);
        if (preview) {
            icon.setClickable(false);
            icon.setLongClickable(false);
            icon.setFocusable(false);
        } else {
            icon.setOnClickListener(mActivityContext.getItemOnClickListener());
            icon.setOnLongClickListener(mActivityContext.getAllAppsItemLongClickListener());
        }
        return icon;
    }

    private TextView createTab(int textRes, Runnable onClick) {
        TextView tab = new TextView(getContext());
        tab.setText(textRes);
        tab.setTextSize(15);
        tab.setPadding(dp(18), dp(8), dp(18), dp(8));
        tab.setClickable(true);
        tab.setFocusable(true);
        tab.setOnClickListener(v -> onClick.run());
        return tab;
    }

    private void updateTabs() {
        styleTab(mAllTab, !mCategoriesMode);
        styleTab(mCategoriesTab, mCategoriesMode);
    }

    private void styleTab(TextView tab, boolean selected) {
        GradientDrawable pill = new GradientDrawable();
        pill.setCornerRadius(dp(28));
        if (selected) {
            pill.setColor(Color.WHITE);
            tab.setTextColor(Color.BLACK);
        } else {
            pill.setColor(Color.TRANSPARENT);
            tab.setTextColor(CaddyStyle.textColor(getContext()));
        }
        tab.setBackground(pill);
    }

    private TextView label(String text) {
        TextView label = new TextView(getContext());
        label.setText(text);
        label.setGravity(android.view.Gravity.CENTER);
        label.setTextSize(13);
        label.setTextColor(CaddyStyle.textColor(getContext()));
        label.setPadding(0, dp(8), 0, 0);
        return label;
    }

    private LayoutParams tileParams() {
        LayoutParams params = new LayoutParams(0, WRAP_CONTENT, 1f);
        params.setMargins(dp(6), dp(6), dp(6), dp(6));
        return params;
    }

    private int dp(float value) {
        return CaddyStyle.dp(getContext(), value);
    }
}
