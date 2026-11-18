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
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.R;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.util.AppsListUtils;
import com.android.launcher3.views.ActivityContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * App drawer for {@link AppDrawerStyle#NOTHING}. Shows an "All | Categories" toggle, a
 * "Recently installed" card and folder-style category tiles. Tapping a tile opens the
 * category's apps. In "All" mode only the toggle is shown and the container's alphabetical
 * list (with its A-Z index) stays visible underneath.
 */
public class NothingDrawerView extends LinearLayout {

    private static final int RECENT_COUNT = 4;
    private static final int TILE_APPS = 4;
    private static final int DETAIL_COLUMNS = 4;
    private static final long ANIM_DURATION = 250;

    /**
     * Category order and names as shown in the reference design, each mapped to the existing
     * AppsListUtils category it is built from: {design label, source label}.
     */
    private static final int[][] CATEGORY_SOURCES = {
            {R.string.nothing_category_social, R.string.social_category_label},
            {R.string.nothing_category_tools, R.string.caddy_utilities},
            {R.string.nothing_category_photography, R.string.caddy_multimedia},
            {R.string.nothing_category_entertainment, R.string.caddy_entertainment},
            {R.string.nothing_category_shopping, R.string.caddy_shopping},
            {R.string.nothing_category_travel, R.string.caddy_travel},
            {R.string.nothing_category_office, R.string.productivity_category_label},
            {R.string.nothing_category_finance, R.string.caddy_finance},
            {R.string.nothing_category_education, R.string.caddy_education},
    };

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
    private int mBottomInset;

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
        toggleParams.gravity = Gravity.CENTER_HORIZONTAL;
        toggleParams.topMargin = getResources().getDimensionPixelSize(
                R.dimen.all_apps_header_top_padding);
        addView(toggle, toggleParams);

        mCategoriesColumn = new LinearLayout(context);
        mCategoriesColumn.setOrientation(VERTICAL);
        mCategoriesColumn.setPadding(dp(12), dp(12), dp(12), dp(12));
        mCategoriesScroll = new ScrollView(context);
        mCategoriesScroll.setClipToPadding(false);
        mCategoriesScroll.addView(mCategoriesColumn);

        mDetailTitle = new TextView(context);
        mDetailTitle.setTextSize(30);
        mDetailTitle.setTextColor(CaddyStyle.textColor(context));
        mDetailTitle.setPadding(dp(20), dp(12), dp(20), dp(16));
        mDetailTitle.setOnClickListener(v -> showCategories());
        mDetailGrid = new LinearLayout(context);
        mDetailGrid.setOrientation(VERTICAL);
        mDetailGrid.setPadding(dp(12), 0, dp(12), dp(12));
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

    /**
     * Keeps the bottom search bar tappable in Categories mode by leaving {@code px} of the
     * bottom of the parent free.
     */
    public void setBottomInset(int px) {
        mBottomInset = px;
        applySizing();
    }

    /** Refreshes the card and tiles from the current app list. */
    public void setApps(List<AppInfo> apps) {
        mApps = apps == null ? List.of() : List.copyOf(apps);
        rebuildCategories();
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
        applySizing();
        if (mModeListener != null) {
            mModeListener.accept(categories);
        }
    }

    private void applySizing() {
        ViewGroup.LayoutParams raw = getLayoutParams();
        if (!(raw instanceof RelativeLayout.LayoutParams lp)) {
            return;
        }
        lp.height = mCategoriesMode ? MATCH_PARENT : WRAP_CONTENT;
        lp.bottomMargin = mCategoriesMode ? mBottomInset : 0;
        setLayoutParams(lp);
    }

    private void rebuildCategories() {
        mCategoriesColumn.removeAllViews();
        if (mApps.isEmpty()) {
            return;
        }
        List<AppInfo> recent = recentlyInstalled(mApps);
        mCategoriesColumn.addView(recentCard(recent));

        List<Map.Entry<String, List<AppInfo>>> categories =
                new ArrayList<>(designCategories(getContext(), mApps).entrySet());
        for (int i = 0; i < categories.size(); i += 2) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(HORIZONTAL);
            for (int j = i; j < Math.min(i + 2, categories.size()); j++) {
                Map.Entry<String, List<AppInfo>> category = categories.get(j);
                row.addView(categoryTile(category.getKey(), category.getValue()), tileParams());
            }
            if (categories.size() - i == 1) {
                row.addView(new View(getContext()), tileParams());
            }
            mCategoriesColumn.addView(row);
        }
    }

    /** Recently installed apps first, by package first-install time, newest first. */
    private List<AppInfo> recentlyInstalled(List<AppInfo> apps) {
        PackageManager pm = getContext().getPackageManager();
        Map<AppInfo, Long> times = new HashMap<>();
        for (AppInfo app : apps) {
            long time = 0;
            if (app.componentName != null) {
                try {
                    time = pm.getPackageInfo(app.componentName.getPackageName(), 0)
                            .firstInstallTime;
                } catch (PackageManager.NameNotFoundException ignored) {
                    time = 0;
                }
            }
            times.put(app, time);
        }
        List<AppInfo> sorted = new ArrayList<>(apps);
        sorted.sort(Comparator.comparingLong((AppInfo a) -> times.getOrDefault(a, 0L)).reversed());
        return sorted.subList(0, Math.min(RECENT_COUNT, sorted.size()));
    }

    /**
     * Groups apps into the reference design's categories, in its order. Empty categories are
     * left out, and anything not matched lands in "Other".
     */
    static Map<String, List<AppInfo>> designCategories(Context context, List<AppInfo> apps) {
        Map<String, List<AppInfo>> source = AppsListUtils.categorizeApps(context, apps);
        Map<String, List<AppInfo>> result = new LinkedHashMap<>();
        java.util.Set<AppInfo> used = new java.util.HashSet<>();
        for (int[] mapping : CATEGORY_SOURCES) {
            List<AppInfo> matched = source.get(context.getString(mapping[1]));
            if (matched == null) {
                continue;
            }
            List<AppInfo> list = new ArrayList<>(matched);
            list.removeIf(used::contains);
            used.addAll(list);
            if (!list.isEmpty()) {
                result.put(context.getString(mapping[0]), list);
            }
        }
        List<AppInfo> other = new ArrayList<>();
        for (AppInfo app : apps) {
            if (!used.contains(app)) {
                other.add(app);
            }
        }
        if (!other.isEmpty()) {
            result.put(context.getString(R.string.nothing_category_other), other);
        }
        return result;
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
        params.setMargins(dp(6), dp(6), dp(6), dp(6));
        card.setLayoutParams(params);
        return card;
    }

    private View categoryTile(String title, List<AppInfo> apps) {
        LinearLayout tile = new LinearLayout(getContext());
        tile.setOrientation(VERTICAL);
        tile.setPadding(dp(12), dp(12), dp(12), dp(12));
        tile.setBackground(CaddyStyle.background(getContext(), false));
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setOnClickListener(v -> showCategory(title, apps));

        LinearLayout preview = new LinearLayout(getContext());
        preview.setOrientation(VERTICAL);
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(HORIZONTAL);
            for (int c = 0; c < 2; c++) {
                int index = r * 2 + c;
                if (index < apps.size() && index < TILE_APPS) {
                    row.addView(icon(apps.get(index), true),
                            new LayoutParams(0, WRAP_CONTENT, 1f));
                } else {
                    row.addView(new View(getContext()), new LayoutParams(0, WRAP_CONTENT, 1f));
                }
            }
            preview.addView(row);
        }
        tile.addView(preview);
        tile.addView(label(title));
        return tile;
    }

    /** Expands the tapped tile into its category page with a short fade and rise. */
    private void showCategory(String title, List<AppInfo> apps) {
        mDetailTitle.setText(title);
        mDetailGrid.removeAllViews();
        addAppGrid(mDetailGrid, apps);
        mCategoriesScroll.animate().cancel();
        mCategoriesScroll.animate().alpha(0f).setDuration(ANIM_DURATION / 2)
                .withEndAction(() -> mCategoriesScroll.setVisibility(GONE)).start();
        mDetailPage.animate().cancel();
        mDetailPage.setAlpha(0f);
        mDetailPage.setTranslationY(dp(24));
        mDetailPage.setVisibility(VISIBLE);
        mDetailPage.animate().alpha(1f).translationY(0).setDuration(ANIM_DURATION).start();
    }

    private void showCategories() {
        mDetailPage.animate().cancel();
        mDetailPage.setVisibility(GONE);
        mCategoriesScroll.animate().cancel();
        mCategoriesScroll.setAlpha(1f);
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
        label.setGravity(Gravity.CENTER);
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
