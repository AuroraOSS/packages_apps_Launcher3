/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.aurora.launcher.views.OptionsPopupView;
import com.aurora.launcher.widget.WidgetsBottomSheet;

public class OptionsBottomSheet extends WidgetsBottomSheet {

    public OptionsBottomSheet(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OptionsBottomSheet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void populateAndShow() {
        super.populateAndShow();
        RelativeLayout mWallpaperLayout = findViewById(R.id.layout_wallpaper);
        RelativeLayout mWidgetLayout = findViewById(R.id.layout_widgets);
        RelativeLayout mSettingsLayout = findViewById(R.id.layout_settings);

        mWallpaperLayout.setOnClickListener(v -> {
            OptionsPopupView.startWallpaperPicker(this);
            close(true);
        });
        mWidgetLayout.setOnClickListener(v -> {
            OptionsPopupView.onWidgetsClicked(this);
            close(true);
        });
        mSettingsLayout.setOnClickListener(v -> {
            OptionsPopupView.startSettings(this);
            close(true);
        });
    }

    @Override
    protected void onWidgetsBound() {
    }

}
