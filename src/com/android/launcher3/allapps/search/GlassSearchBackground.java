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

package com.android.launcher3.allapps.search;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * iOS-style "liquid glass" background for the app drawer search field.
 *
 * <p>Draws a translucent fill (which sits on top of the drawer's backdrop blur), a soft
 * highlight along the top rim and a hairline outline. {@link #setProgress(float)} blends between
 * the idle look (0) and the active, focused look (1). Light and dark variants are chosen with
 * {@link #setDark(boolean)}.
 */
public final class GlassSearchBackground extends Drawable {

    private static final float STROKE_PX = 1f;

    private final Paint mFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mRim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mHairline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBody = new RectF();

    /** Explicit corner radius in px, or negative for "half the height" (fully round). */
    private float mRadius = -1f;
    private float mProgress = 0f;
    private boolean mDark = false;

    public GlassSearchBackground() {
        mRim.setStyle(Paint.Style.STROKE);
        mRim.setStrokeWidth(STROKE_PX);
        mHairline.setStyle(Paint.Style.STROKE);
        mHairline.setStrokeWidth(STROKE_PX);
    }

    /** Sets the corner radius in px. Negative values mean half the drawable's height. */
    public void setCornerRadius(float radiusPx) {
        if (mRadius != radiusPx) {
            mRadius = radiusPx;
            invalidateSelf();
        }
    }

    /** Switches between the light and dark glass palettes. */
    public void setDark(boolean dark) {
        if (mDark != dark) {
            mDark = dark;
            invalidateSelf();
        }
    }

    /** 0 = idle, 1 = active (focused or results shown). */
    public void setProgress(float progress) {
        float clamped = Math.max(0f, Math.min(1f, progress));
        if (mProgress != clamped) {
            mProgress = clamped;
            invalidateSelf();
        }
    }

    public float getProgress() {
        return mProgress;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mBody.set(bounds);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) {
            return;
        }
        float inset = STROKE_PX / 2f;
        mBody.set(bounds);
        mBody.inset(inset, inset);
        float radius = resolveRadius(mBody);

        float p = mProgress;
        int rgb = mDark ? 0x2C2C2E : 0xFFFFFF;
        int topAlpha = Math.round(lerp(mDark ? 0.42f : 0.62f, mDark ? 0.62f : 0.86f, p) * 255);
        int bottomAlpha = Math.round(lerp(mDark ? 0.30f : 0.46f, mDark ? 0.52f : 0.74f, p) * 255);
        int rimAlpha = Math.round(lerp(mDark ? 0.16f : 0.70f, mDark ? 0.28f : 0.95f, p) * 255);
        int hairlineAlpha = Math.round(lerp(mDark ? 0.14f : 0.07f, mDark ? 0.22f : 0.12f, p) * 255);

        // Body: a light-to-dim vertical gradient so the glass reads as having depth.
        mFill.setShader(new LinearGradient(0, mBody.top, 0, mBody.bottom,
                Color.argb(topAlpha, Color.red(rgb), Color.green(rgb), Color.blue(rgb)),
                Color.argb(bottomAlpha, Color.red(rgb), Color.green(rgb), Color.blue(rgb)),
                Shader.TileMode.CLAMP));
        mFill.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(mBody, radius, radius, mFill);

        // Rim light: bright along the top edge, fading out by the vertical centre.
        float centre = mBody.centerY();
        mRim.setShader(new LinearGradient(0, mBody.top, 0, centre,
                Color.argb(rimAlpha, 255, 255, 255), Color.argb(0, 255, 255, 255),
                Shader.TileMode.CLAMP));
        canvas.drawRoundRect(mBody, radius, radius, mRim);

        mHairline.setColor(mDark
                ? Color.argb(hairlineAlpha, 255, 255, 255)
                : Color.argb(hairlineAlpha, 0, 0, 0));
        canvas.drawRoundRect(mBody, radius, radius, mHairline);
    }

    @Override
    public void getOutline(Outline outline) {
        Rect bounds = getBounds();
        RectF rect = new RectF(bounds);
        outline.setRoundRect(bounds, resolveRadius(rect));
    }

    private float resolveRadius(RectF rect) {
        float half = Math.min(rect.width(), rect.height()) / 2f;
        if (mRadius < 0) {
            return half;
        }
        return Math.min(mRadius, half);
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    @Override
    public void setAlpha(int alpha) {
        // Alpha is handled by the palette; the view's own alpha controls fading.
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        // Not supported: the glass palette is fixed per theme.
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
