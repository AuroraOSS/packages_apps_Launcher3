/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.aurora.launcher;

import android.view.View;
import android.view.animation.Interpolator;

import com.aurora.launcher.LauncherState.PageAlphaProvider;
import com.aurora.launcher.LauncherStateManager.AnimationConfig;
import com.aurora.launcher.anim.AnimatorSetBuilder;
import com.aurora.launcher.anim.PropertySetter;
import com.aurora.launcher.graphics.WorkspaceAndHotseatScrim;

import static com.aurora.launcher.LauncherAnimUtils.DRAWABLE_ALPHA;
import static com.aurora.launcher.LauncherAnimUtils.SCALE_PROPERTY;
import static com.aurora.launcher.LauncherState.HOTSEAT_ICONS;
import static com.aurora.launcher.LauncherState.HOTSEAT_SEARCH_BOX;
import static com.aurora.launcher.anim.AnimatorSetBuilder.ANIM_WORKSPACE_FADE;
import static com.aurora.launcher.anim.AnimatorSetBuilder.ANIM_WORKSPACE_SCALE;
import static com.aurora.launcher.anim.Interpolators.LINEAR;
import static com.aurora.launcher.anim.Interpolators.ZOOM_OUT;
import static com.aurora.launcher.anim.PropertySetter.NO_ANIM_PROPERTY_SETTER;
import static com.aurora.launcher.graphics.WorkspaceAndHotseatScrim.SCRIM_PROGRESS;
import static com.aurora.launcher.graphics.WorkspaceAndHotseatScrim.SYSUI_PROGRESS;

/**
 * Manages the animations between each of the workspace states.
 */
public class WorkspaceStateTransitionAnimation {

    private final Launcher mLauncher;
    private final Workspace mWorkspace;

    private float mNewScale;

    public WorkspaceStateTransitionAnimation(Launcher launcher, Workspace workspace) {
        mLauncher = launcher;
        mWorkspace = workspace;
    }

    public void setState(LauncherState toState) {
        setWorkspaceProperty(toState, NO_ANIM_PROPERTY_SETTER, new AnimatorSetBuilder(),
                new AnimationConfig());
    }

    public void setStateWithAnimation(LauncherState toState, AnimatorSetBuilder builder,
                                      AnimationConfig config) {
        setWorkspaceProperty(toState, config.getPropertySetter(builder), builder, config);
    }

    public float getFinalScale() {
        return mNewScale;
    }

    /**
     * Starts a transition animation for the workspace.
     */
    private void setWorkspaceProperty(LauncherState state, PropertySetter propertySetter,
                                      AnimatorSetBuilder builder, AnimationConfig config) {
        float[] scaleAndTranslation = state.getWorkspaceScaleAndTranslation(mLauncher);
        mNewScale = scaleAndTranslation[0];
        PageAlphaProvider pageAlphaProvider = state.getWorkspacePageAlphaProvider(mLauncher);
        final int childCount = mWorkspace.getChildCount();
        for (int i = 0; i < childCount; i++) {
            applyChildState(state, (CellLayout) mWorkspace.getChildAt(i), i, pageAlphaProvider,
                    propertySetter, builder, config);
        }

        int elements = state.getVisibleElements(mLauncher);
        Interpolator fadeInterpolator = builder.getInterpolator(ANIM_WORKSPACE_FADE,
                pageAlphaProvider.interpolator);
        boolean playAtomicComponent = config.playAtomicComponent();
        if (playAtomicComponent) {
            Interpolator scaleInterpolator = builder.getInterpolator(ANIM_WORKSPACE_SCALE, ZOOM_OUT);
            propertySetter.setFloat(mWorkspace, SCALE_PROPERTY, mNewScale, scaleInterpolator);
            float hotseatIconsAlpha = (elements & HOTSEAT_ICONS) != 0 ? 1 : 0;
            propertySetter.setViewAlpha(mLauncher.getHotseat().getLayout(), hotseatIconsAlpha,
                    fadeInterpolator);
            propertySetter.setViewAlpha(mLauncher.getWorkspace().getPageIndicator(),
                    hotseatIconsAlpha, fadeInterpolator);
        }

        if (!config.playNonAtomicComponent()) {
            // Only the alpha and scale, handled above, are included in the atomic animation.
            return;
        }

        Interpolator translationInterpolator = !playAtomicComponent ? LINEAR : ZOOM_OUT;
        propertySetter.setFloat(mWorkspace, View.TRANSLATION_X,
                scaleAndTranslation[1], translationInterpolator);
        propertySetter.setFloat(mWorkspace, View.TRANSLATION_Y,
                scaleAndTranslation[2], translationInterpolator);

        propertySetter.setViewAlpha(mLauncher.getHotseatSearchBox(),
                (elements & HOTSEAT_SEARCH_BOX) != 0 ? 1 : 0, fadeInterpolator);

        // Set scrim
        WorkspaceAndHotseatScrim scrim = mLauncher.getDragLayer().getScrim();
        propertySetter.setFloat(scrim, SCRIM_PROGRESS, state.getWorkspaceScrimAlpha(mLauncher),
                LINEAR);
        propertySetter.setFloat(scrim, SYSUI_PROGRESS, state.hasSysUiScrim ? 1 : 0, LINEAR);
    }

    public void applyChildState(LauncherState state, CellLayout cl, int childIndex) {
        applyChildState(state, cl, childIndex, state.getWorkspacePageAlphaProvider(mLauncher),
                NO_ANIM_PROPERTY_SETTER, new AnimatorSetBuilder(), new AnimationConfig());
    }

    private void applyChildState(LauncherState state, CellLayout cl, int childIndex,
                                 PageAlphaProvider pageAlphaProvider, PropertySetter propertySetter,
                                 AnimatorSetBuilder builder, AnimationConfig config) {
        float pageAlpha = pageAlphaProvider.getPageAlpha(childIndex);
        int drawableAlpha = Math.round(pageAlpha * (state.hasWorkspacePageBackground ? 255 : 0));

        if (config.playNonAtomicComponent()) {
            propertySetter.setInt(cl.getScrimBackground(),
                    DRAWABLE_ALPHA, drawableAlpha, ZOOM_OUT);
        }
        if (config.playAtomicComponent()) {
            Interpolator fadeInterpolator = builder.getInterpolator(ANIM_WORKSPACE_FADE,
                    pageAlphaProvider.interpolator);
            propertySetter.setFloat(cl.getShortcutsAndWidgets(), View.ALPHA,
                    pageAlpha, fadeInterpolator);
        }
    }
}