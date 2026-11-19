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
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.launcher3.Utilities;
import com.android.launcher3.util.Themes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Persistent A-Z letter strip on the right edge of the app drawer. Touching or dragging along
 * it jumps the list to that letter's section. Letters come from the drawer's fast-scroll
 * sections, so the strip always matches the list.
 */
public final class AlphaIndexBar extends LinearLayout {

    private final List<AlphabeticalAppsList.FastScrollSectionInfo> mSections = new ArrayList<>();
    private final List<TextView> mLetters = new ArrayList<>();
    private final List<Runnable> mLetterMotion = new ArrayList<>();
    private Consumer<AlphabeticalAppsList.FastScrollSectionInfo> mOnSelect;
    private int mSelectedIndex = -1;

    public AlphaIndexBar(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setClickable(false);
    }

    /**
     * Rebuilds the letters from the given sections. Letters with no section are left out.
     *
     * @param sections fast-scroll sections of the list (one per letter)
     * @param onSelect called with the section for the letter the user touches
     */
    public void setSections(List<AlphabeticalAppsList.FastScrollSectionInfo> sections,
            Consumer<AlphabeticalAppsList.FastScrollSectionInfo> onSelect) {
        mOnSelect = onSelect;
        mSections.clear();
        removeAllViews();
        cancelAllLetterMotion();
        mLetterMotion.clear();
        mLetters.clear();
        mSelectedIndex = -1;
        if (sections == null) {
            return;
        }
        for (AlphabeticalAppsList.FastScrollSectionInfo section : sections) {
            if (section == null || section.sectionName == null
                    || section.sectionName.length() == 0) {
                continue;
            }
            mSections.add(section);
            TextView letter = new TextView(getContext());
            letter.setText(section.sectionName);
            letter.setTextSize(10);
            letter.setGravity(Gravity.CENTER);
            letter.setTextColor(Themes.getAttrColor(getContext(),
                    android.R.attr.textColorSecondary));
            letter.setPadding(0, dp(1), 0, dp(1));
            addView(letter, new LayoutParams(dp(18), dp(14)));
            mLetters.add(letter);
            mLetterMotion.add(null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mSections.isEmpty()) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                // fall through
            case MotionEvent.ACTION_MOVE:
                selectAt(event.getY());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setSelectedIndex(-1);
                return true;
            default:
                return false;
        }
    }

    private void selectAt(float y) {
        int height = getHeight();
        if (height <= 0) {
            return;
        }
        int count = mSections.size();
        int index = Utilities.boundToRange((int) (y / height * count), 0, count - 1);
        if (index == mSelectedIndex) {
            return;
        }
        setSelectedIndex(index);
        if (mOnSelect != null) {
            mOnSelect.accept(mSections.get(index));
        }
    }

    private void setSelectedIndex(int index) {
        if (mSelectedIndex >= 0 && mSelectedIndex < mLetters.size()) {
            styleLetter(mSelectedIndex, false);
        }
        mSelectedIndex = index;
        if (index >= 0 && index < mLetters.size()) {
            styleLetter(index, true);
        }
    }

    private void styleLetter(int index, boolean selected) {
        TextView letter = mLetters.get(index);
        letter.setTextColor(selected
                ? Themes.getAttrColor(getContext(), android.R.attr.colorAccent)
                : Themes.getAttrColor(getContext(), android.R.attr.textColorSecondary));
        Runnable previous = mLetterMotion.get(index);
        if (previous != null) {
            previous.run();
        }
        float from = letter.getScaleX();
        float to = selected ? 1.4f : 1f;
        mLetterMotion.set(index, DrawerMotion.run(getContext(), from, to, scale -> {
            letter.setScaleX(scale);
            letter.setScaleY(scale);
        }));
    }

    private void cancelAllLetterMotion() {
        for (Runnable motion : mLetterMotion) {
            if (motion != null) {
                motion.run();
            }
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
