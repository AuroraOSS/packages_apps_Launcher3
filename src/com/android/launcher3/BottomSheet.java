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

package com.android.launcher3;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.launcher3.widget.WidgetsBottomSheet;

public class BottomSheet extends WidgetsBottomSheet {
    private FragmentManager mFragmentManager;

    public BottomSheet(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BottomSheet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mFragmentManager = Launcher.getLauncher(context).getFragmentManager();
    }

    @Override
    public void populateAndShow(ItemInfo itemInfo) {
        super.populateAndShow(itemInfo);
        TextView mTitle = findViewById(R.id.title);
        TextView mPackageName = findViewById(R.id.package_name);
        ImageView mIcon = findViewById(R.id.icon);

        (mTitle).setText(itemInfo.title);
        (mPackageName).setText(itemInfo.getTargetComponent().getPackageName());
        ((EditPreference) mFragmentManager.findFragmentById(R.id.sheet_prefs)).loadForApp(itemInfo);

        if (itemInfo instanceof ItemInfoWithIcon) {
            Bitmap bitmap = ((ItemInfoWithIcon) itemInfo).iconBitmap;
            if (bitmap != null)
                mIcon.setImageBitmap(bitmap);
            int color = ((ItemInfoWithIcon) itemInfo).iconColor;
            /*setBackgroundTintList(ColorStateList.valueOf(color));
            mTitle.setTextColor(ColorUtil.manipulateColor(color, .65f));
            mPackageName.setTextColor(ColorUtil.manipulateColor(color, .75f));*/
        }
    }

    @Override
    public void onDetachedFromWindow() {
        Fragment pf = mFragmentManager.findFragmentById(R.id.sheet_prefs);
        if (pf != null) {
            mFragmentManager.beginTransaction().remove(pf).commitAllowingStateLoss();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWidgetsBound() {
    }
}
