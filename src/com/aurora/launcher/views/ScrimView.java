/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.aurora.launcher.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.RectEvaluator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Property;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.customview.widget.ExploreByTouchHelper;

import com.aurora.launcher.DeviceProfile;
import com.aurora.launcher.Insettable;
import com.aurora.launcher.Launcher;
import com.aurora.launcher.LauncherState;
import com.aurora.launcher.LauncherStateManager;
import com.aurora.launcher.LauncherStateManager.StateListener;
import com.aurora.launcher.R;
import com.aurora.launcher.Utilities;
import com.aurora.launcher.uioverrides.WallpaperColorInfo;
import com.aurora.launcher.uioverrides.WallpaperColorInfo.OnChangeListener;
import com.aurora.launcher.userevent.nano.LauncherLogProto.Action;
import com.aurora.launcher.userevent.nano.LauncherLogProto.ControlType;

import java.util.List;

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static android.view.MotionEvent.ACTION_DOWN;
import static androidx.core.graphics.ColorUtils.compositeColors;
import static androidx.core.graphics.ColorUtils.setAlphaComponent;
import static com.aurora.launcher.LauncherState.ALL_APPS;
import static com.aurora.launcher.LauncherState.NORMAL;
import static com.aurora.launcher.anim.Interpolators.ACCEL;
import static com.aurora.launcher.anim.Interpolators.DEACCEL;
import static com.aurora.launcher.preferences.DrawerFragment.PREF_DRAWER_BG_COLOR;
import static com.aurora.launcher.preferences.DrawerFragment.PREF_DRAWER_BG_CUSTOMIZATION;

/**
 * Simple scrim which draws a flat color
 */
public class ScrimView extends View implements Insettable, OnChangeListener,
        AccessibilityStateChangeListener, StateListener {

    public static final Property<ScrimView, Integer> DRAG_HANDLE_ALPHA =
            new Property<ScrimView, Integer>(Integer.TYPE, "dragHandleAlpha") {

                @Override
                public Integer get(ScrimView scrimView) {
                    return scrimView.mDragHandleAlpha;
                }

                @Override
                public void set(ScrimView scrimView, Integer value) {
                    scrimView.setDragHandleAlpha(value);
                }
            };
    private static final int WALLPAPERS = R.string.wallpaper_button_text;
    private static final int WIDGETS = R.string.widget_button_text;
    private static final int SETTINGS = R.string.settings_button_text;
    protected final Launcher mLauncher;
    protected final int mDragHandleSize;
    private final Rect mTempRect = new Rect();
    private final int[] mTempPos = new int[2];
    private final WallpaperColorInfo mWallpaperColorInfo;
    private final AccessibilityManager mAM;
    private final Rect mDragHandleBounds;
    private final RectF mHitRect = new RectF();
    private final AccessibilityHelper mAccessibilityHelper;
    protected int mEndScrim = 0x00000000;
    protected float mMaxScrimAlpha;
    protected float mProgress = 1;
    protected int mScrimColor;
    protected int mCurrentFlatColor;
    protected int mEndFlatColor;
    protected int mEndFlatColorAlpha;
    protected float mDragHandleOffset;
    @Nullable
    protected Drawable mDragHandle;
    private SharedPreferences mPrefs;
    private boolean mDrawerBgCustomization;
    private int mDrawerBgColor;
    private int mDragHandleAlpha = 255;

    public ScrimView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mLauncher = Launcher.getLauncher(context);
        mWallpaperColorInfo = WallpaperColorInfo.getInstance(context);

        mPrefs = Utilities.getPrefs(context.getApplicationContext());
        mDrawerBgCustomization = mPrefs.getBoolean(PREF_DRAWER_BG_CUSTOMIZATION, false);
        mDrawerBgColor = mPrefs.getInt(PREF_DRAWER_BG_COLOR, Utilities.getScrimColor(context));

        if (mDrawerBgCustomization)
            mEndScrim = mDrawerBgColor;
        else
            mEndScrim = Utilities.getScrimColor(context);

        mMaxScrimAlpha = 0.75f;

        mDragHandleSize = context.getResources()
                .getDimensionPixelSize(R.dimen.vertical_drag_handle_size);
        mDragHandleBounds = new Rect(0, 0, mDragHandleSize, mDragHandleSize);

        mAccessibilityHelper = createAccessibilityHelper();
        ViewCompat.setAccessibilityDelegate(this, mAccessibilityHelper);

        mAM = (AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
        setFocusable(false);
    }

    @NonNull
    protected AccessibilityHelper createAccessibilityHelper() {
        return new AccessibilityHelper();
    }

    @Override
    public void setInsets(Rect insets) {
        updateDragHandleBounds();
        updateDragHandleVisibility(null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        updateDragHandleBounds();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mWallpaperColorInfo.addOnChangeListener(this);
        onExtractedColorsChanged(mWallpaperColorInfo);

        mAM.addAccessibilityStateChangeListener(this);
        onAccessibilityStateChanged(mAM.isEnabled());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mWallpaperColorInfo.removeOnChangeListener(this);
        mAM.removeAccessibilityStateChangeListener(this);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public void onExtractedColorsChanged(WallpaperColorInfo wallpaperColorInfo) {
        mScrimColor = wallpaperColorInfo.getMainColor();
        mEndFlatColor = compositeColors(mEndScrim, setAlphaComponent(
                mScrimColor, Math.round(mMaxScrimAlpha * 255)));
        if (mDrawerBgCustomization)
            mEndFlatColor = mEndScrim;
        mEndFlatColorAlpha = Color.alpha(mEndFlatColor);
        updateColors();
        invalidate();
    }

    public void setProgress(float progress) {
        if (mProgress != progress) {
            mProgress = progress;
            updateColors();
            updateDragHandleAlpha();
            invalidate();
        }
    }

    public void reInitUi() {
    }

    protected void updateColors() {
        mCurrentFlatColor = mProgress >= 1 ? 0 : setAlphaComponent(
                mEndFlatColor, Math.round((1 - mProgress) * mEndFlatColorAlpha));
    }

    protected void updateDragHandleAlpha() {
        if (mDragHandle != null) {
            mDragHandle.setAlpha(mDragHandleAlpha);
        }
    }

    private void setDragHandleAlpha(int alpha) {
        if (alpha != mDragHandleAlpha) {
            mDragHandleAlpha = alpha;
            if (mDragHandle != null) {
                mDragHandle.setAlpha(mDragHandleAlpha);
                invalidate();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mCurrentFlatColor != 0) {
            canvas.drawColor(mCurrentFlatColor);
        }
        drawDragHandle(canvas);
    }

    protected void drawDragHandle(Canvas canvas) {
        if (mDragHandle != null) {
            canvas.translate(0, -mDragHandleOffset);
            mDragHandle.draw(canvas);
            canvas.translate(0, mDragHandleOffset);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean value = super.onTouchEvent(event);
        if (!value && mDragHandle != null && event.getAction() == ACTION_DOWN
                && mDragHandle.getAlpha() == 255
                && mHitRect.contains(event.getX(), event.getY())) {

            final Drawable drawable = mDragHandle;
            mDragHandle = null;

            Rect bounds = new Rect(mDragHandleBounds);
            bounds.offset(0, -(int) mDragHandleOffset);
            drawable.setBounds(bounds);

            Rect topBounds = new Rect(bounds);
            topBounds.offset(0, -bounds.height() / 2);

            Rect invalidateRegion = new Rect(bounds);
            invalidateRegion.top = topBounds.top;

            Keyframe frameTop = Keyframe.ofObject(0.6f, topBounds);
            frameTop.setInterpolator(DEACCEL);
            Keyframe frameBot = Keyframe.ofObject(1, bounds);
            frameBot.setInterpolator(ACCEL);
            PropertyValuesHolder holder = PropertyValuesHolder.ofKeyframe("bounds",
                    Keyframe.ofObject(0, bounds), frameTop, frameBot);
            holder.setEvaluator(new RectEvaluator());

            ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(drawable, holder);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    getOverlay().remove(drawable);
                    updateDragHandleVisibility(drawable);
                }
            });
            anim.addUpdateListener((v) -> invalidate(invalidateRegion));
            getOverlay().add(drawable);
            anim.start();
        }
        return value;
    }

    protected void updateDragHandleBounds() {
        DeviceProfile grid = mLauncher.getDeviceProfile();
        final int left;
        final int width = getMeasuredWidth();
        final int top = getMeasuredHeight() - mDragHandleSize - grid.getInsets().bottom;
        final int topMargin;

        if (grid.isVerticalBarLayout()) {
            topMargin = grid.workspacePadding.bottom;
            if (grid.isSeascape()) {
                left = width - grid.getInsets().right - mDragHandleSize;
            } else {
                left = mDragHandleSize + grid.getInsets().left;
            }
        } else {
            left = (width - mDragHandleSize) / 2;
            topMargin = grid.hotseatBarSizePx;
        }
        mDragHandleBounds.offsetTo(left, top - topMargin);
        mHitRect.set(mDragHandleBounds);
        float inset = -mDragHandleSize / 2;
        mHitRect.inset(inset, inset);

        if (mDragHandle != null) {
            mDragHandle.setBounds(mDragHandleBounds);
        }
    }

    @Override
    public void onAccessibilityStateChanged(boolean enabled) {
        LauncherStateManager stateManager = mLauncher.getStateManager();
        stateManager.removeStateListener(this);

        if (enabled) {
            stateManager.addStateListener(this);
            onStateSetImmediately(mLauncher.getStateManager().getState());
        } else {
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        }
        updateDragHandleVisibility(null);
    }

    private void updateDragHandleVisibility(Drawable recycle) {
        boolean visible = mLauncher.getDeviceProfile().isVerticalBarLayout() || mAM.isEnabled();
        boolean wasVisible = mDragHandle != null;
        if (visible != wasVisible) {
            if (visible) {
                mDragHandle = recycle != null ? recycle :
                        mLauncher.getDrawable(R.drawable.drag_handle_indicator);
                mDragHandle.setBounds(mDragHandleBounds);

                updateDragHandleAlpha();
            } else {
                mDragHandle = null;
            }
            invalidate();
        }
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        return mAccessibilityHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mAccessibilityHelper.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    @Override
    public void onFocusChanged(boolean gainFocus, int direction,
                               Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        mAccessibilityHelper.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    @Override
    public void onStateTransitionStart(LauncherState toState) {
    }

    @Override
    public void onStateTransitionComplete(LauncherState finalState) {
        onStateSetImmediately(finalState);
    }

    @Override
    public void onStateSetImmediately(LauncherState state) {
        setImportantForAccessibility(state == ALL_APPS
                ? IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                : IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    }

    public int getDragHandleSize() {
        return mDragHandleSize;
    }

    protected class AccessibilityHelper extends ExploreByTouchHelper {

        private static final int DRAG_HANDLE_ID = 1;

        public AccessibilityHelper() {
            super(ScrimView.this);
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            return mDragHandleBounds.contains((int) x, (int) y)
                    ? DRAG_HANDLE_ID : INVALID_ID;
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            virtualViewIds.add(DRAG_HANDLE_ID);
        }

        @Override
        protected void onPopulateNodeForVirtualView(int virtualViewId,
                                                    AccessibilityNodeInfoCompat node) {
            node.setContentDescription(getContext().getString(R.string.all_apps_button_label));
            node.setBoundsInParent(mDragHandleBounds);

            getLocationOnScreen(mTempPos);
            mTempRect.set(mDragHandleBounds);
            mTempRect.offset(mTempPos[0], mTempPos[1]);
            node.setBoundsInScreen(mTempRect);

            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            node.setClickable(true);
            node.setFocusable(true);

            if (mLauncher.isInState(NORMAL)) {
                Context context = getContext();
                if (Utilities.isWallpaperAllowed(context)) {
                    node.addAction(
                            new AccessibilityActionCompat(WALLPAPERS, context.getText(WALLPAPERS)));
                }
                node.addAction(new AccessibilityActionCompat(WIDGETS, context.getText(WIDGETS)));
                node.addAction(new AccessibilityActionCompat(SETTINGS, context.getText(SETTINGS)));
            }
        }

        @Override
        protected boolean onPerformActionForVirtualView(
                int virtualViewId, int action, Bundle arguments) {
            if (action == AccessibilityNodeInfoCompat.ACTION_CLICK) {
                mLauncher.getUserEventDispatcher().logActionOnControl(
                        Action.Touch.TAP, ControlType.ALL_APPS_BUTTON,
                        mLauncher.getStateManager().getState().containerType);
                mLauncher.getStateManager().goToState(ALL_APPS);
                return true;
            } else if (action == WALLPAPERS) {
                return OptionsPopupView.startWallpaperPicker(ScrimView.this);
            } else if (action == WIDGETS) {
                return OptionsPopupView.onWidgetsClicked(ScrimView.this);
            } else if (action == SETTINGS) {
                return OptionsPopupView.startSettings(ScrimView.this);
            }

            return false;
        }
    }
}
