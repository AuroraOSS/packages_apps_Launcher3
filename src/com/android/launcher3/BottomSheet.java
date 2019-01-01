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
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.launcher3.preferences.PrefUtils;
import com.android.launcher3.widget.WidgetsBottomSheet;

import java.util.Map;

/*import android.content.res.ColorStateList;*/

public class BottomSheet extends WidgetsBottomSheet implements TextView.OnEditorActionListener {

    public static final String PREF_EDIT_TITLE = "pref_edit_title";

    private FragmentManager mFragmentManager;
    private String mPackageName;
    private Map<String, String> mCustomTitle;
    private EditText mTitleTxt;
    private TextView mPackageNameTxt;
    private ItemInfo mAppInfo;

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
        mAppInfo = itemInfo;
        mTitleTxt = findViewById(R.id.title);
        mPackageNameTxt = findViewById(R.id.package_name);

        mCustomTitle = getCustomTitles();
        mPackageName = itemInfo.getTargetComponent().getPackageName();

        if (!mCustomTitle.isEmpty() && hasCustomTitle(mPackageName))
            mTitleTxt.setText(mCustomTitle.get(mPackageName));
        else
            mTitleTxt.setText(itemInfo.title);

        mTitleTxt.setOnEditorActionListener(this);
        mPackageNameTxt.setText(itemInfo.getTargetComponent().getPackageName());

        ((EditPreference) mFragmentManager.findFragmentById(R.id.sheet_prefs)).loadForApp(itemInfo);

        ImageView mIcon = findViewById(R.id.icon);
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

    private boolean hasCustomTitle(String key) {
        return mCustomTitle.containsKey(key);
    }

    private Map<String, String> getCustomTitles() {
        return PrefUtils.getMap(PREF_EDIT_TITLE, getContext());
    }

    private void setCustomTitle(Map<String, String> mCustomTitle) {
        PrefUtils.saveMap(PREF_EDIT_TITLE, getContext(), mCustomTitle);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            mCustomTitle.put(mPackageName, mTitleTxt.getText().toString());
            setCustomTitle(mCustomTitle);
            ((InputMethodManager) mLauncher.getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(v.getWindowToken(), 0);
            Launcher.getLauncher(mLauncher).getModel().forceReload();
            return true;
        }
        return false;
    }
}
