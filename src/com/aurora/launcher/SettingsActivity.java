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

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.MenuItem;

import com.aurora.launcher.preferences.SettingsFragment;
import com.aurora.launcher.uioverrides.WallpaperColorInfo;
import com.aurora.launcher.util.LooperExecutor;

import static com.aurora.launcher.Utilities.getPrefs;

/**
 * Settings activity for Launcher. Currently implements the following setting: Allow rotation
 */
public class SettingsActivity extends Activity implements WallpaperColorInfo.OnChangeListener {


    public static final String NOTIFICATION_BADGING = "notification_badging";
    public static final String KEY_MINUS_ONE = "pref_enable_minus_one";
    public static final String PREF_THEME_STYLE_KEY = "pref_theme_style";
    public static final String TAG = "IconShapeOverride";
    public static final long PROCESS_KILL_DELAY_MS = 1000;
    public static final int RESTART_REQUEST_CODE = 42;
    public static final String ICON_BADGING_PREFERENCE_KEY = "pref_icon_badging";
    public static final String NOTIFICATION_ENABLED_LISTENERS = "enabled_notification_listeners";
    public static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";
    public static final String EXTRA_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args";
    public static final int DELAY_HIGHLIGHT_DURATION_MILLIS = 600;
    public static final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";
    public static final String KEY_GRID_SIZE = "pref_grid_size";
    public static final String KEY_SHOW_DESKTOP_LABELS = "pref_desktop_show_labels";
    public static final String KEY_SHOW_DRAWER_LABELS = "pref_drawer_show_labels";
    public static final String ICON_SIZE = "pref_icon_size";
    public static final String PREF_ICON_PACKAGE = "pref_iconPackPackage";

    public static boolean mShouldRestart = false;
    private int mThemeStyle;
    private int mThemeRes = R.style.PreferenceTheme;
    private ActionBar mActionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
        setTheme();
        mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setElevation(2);
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setTheme() {
        WallpaperColorInfo wallpaperColorInfo = WallpaperColorInfo.getInstance(this);
        wallpaperColorInfo.addOnChangeListener(this);
        mThemeRes = getThemeRes(wallpaperColorInfo);
        setTheme(mThemeRes);
    }

    protected int getThemeRes(WallpaperColorInfo wallpaperColorInfo) {
        mThemeStyle = Integer.parseInt(getPrefs(getApplicationContext()).getString(PREF_THEME_STYLE_KEY, "0"));
        if (mThemeStyle == 1) {
            return R.style.PreferenceTheme;
        } else if (mThemeStyle == 2) {
            return R.style.PreferenceTheme_Dark;
        } else if (mThemeStyle == 3) {
            return R.style.PreferenceTheme_Black;
        } else {
            if (wallpaperColorInfo.isDark()) {
                return wallpaperColorInfo.supportsDarkText() ?
                        R.style.LauncherTheme_DarkText : R.style.PreferenceTheme_Dark;
            } else {
                return wallpaperColorInfo.supportsDarkText() ?
                        R.style.LauncherTheme_DarkText : R.style.PreferenceTheme;
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mShouldRestart) {
            new LooperExecutor(LauncherModel.getWorkerLooper()).execute(
                    new OverrideApplyHandler(this));
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        if (mShouldRestart) {
            new LooperExecutor(LauncherModel.getWorkerLooper()).execute(
                    new OverrideApplyHandler(this));
        }
        super.onStop();
    }

    @Override
    public void onExtractedColorsChanged(WallpaperColorInfo wallpaperColorInfo) {
        if (mThemeRes != getThemeRes(wallpaperColorInfo)) {
            recreate();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static class OverrideApplyHandler implements Runnable {

        private final Context mContext;

        private OverrideApplyHandler(Context context) {
            mContext = context;
        }

        @Override
        public void run() {

            try {
                Thread.sleep(PROCESS_KILL_DELAY_MS);
            } catch (Exception e) {
                Log.e(TAG, "Error waiting", e);
            }

            Intent homeIntent = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .setPackage(mContext.getPackageName())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(mContext, RESTART_REQUEST_CODE,
                    homeIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
            mContext.getSystemService(AlarmManager.class).setExact(
                    AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 50, pi);

            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

}
