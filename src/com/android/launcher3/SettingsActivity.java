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

package com.android.launcher3;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Adapter;
import android.widget.ListView;
import android.widget.NumberPicker;

import com.android.launcher3.graphics.IconShapeOverride;
import com.android.launcher3.notification.NotificationListener;
import com.android.launcher3.uioverrides.WallpaperColorInfo;
import com.android.launcher3.util.ListViewHighlighter;
import com.android.launcher3.util.LooperExecutor;
import com.android.launcher3.util.SettingsObserver;
import com.android.launcher3.views.ButtonPreference;

import java.util.Objects;

import static com.android.launcher3.Utilities.PREF_NOTIFICATIONS_GESTURE;
import static com.android.launcher3.Utilities.getPrefs;
import static com.android.launcher3.states.RotationHelper.ALLOW_ROTATION_PREFERENCE_KEY;
import static com.android.launcher3.states.RotationHelper.getAllowRotationDefaultValue;

/**
 * Settings activity for Launcher. Currently implements the following setting: Allow rotation
 */
public class SettingsActivity extends Activity implements WallpaperColorInfo.OnChangeListener {

    /**
     * Hidden field Settings.Secure.NOTIFICATION_BADGING
     */
    public static final String NOTIFICATION_BADGING = "notification_badging";
    public static final String KEY_MINUS_ONE = "pref_enable_minus_one";
    public static final String PREF_THEME_STYLE_KEY = "pref_theme_style";
    private static final String TAG = "IconShapeOverride";
    private static final long PROCESS_KILL_DELAY_MS = 1000;
    private static final int RESTART_REQUEST_CODE = 42;
    private static final String ICON_BADGING_PREFERENCE_KEY = "pref_icon_badging";
    /**
     * Hidden field Settings.Secure.ENABLED_NOTIFICATION_LISTENERS
     */
    private static final String NOTIFICATION_ENABLED_LISTENERS = "enabled_notification_listeners";
    private static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";
    private static final String EXTRA_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args";
    private static final int DELAY_HIGHLIGHT_DURATION_MILLIS = 600;
    private static final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";
    private static final String KEY_GRID_SIZE = "pref_grid_size";
    private static final String KEY_SHOW_DESKTOP_LABELS = "pref_desktop_show_labels";
    private static final String KEY_SHOW_DRAWER_LABELS = "pref_drawer_show_labels";
    private static final String ICON_SIZE = "pref_icon_size";
    protected int mThemeStyle;
    private int mThemeRes = R.style.PreferenceTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // Display the fragment as the main content.
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, getNewFragment())
                    .commit();
        }

        WallpaperColorInfo wallpaperColorInfo = WallpaperColorInfo.getInstance(this);
        wallpaperColorInfo.addOnChangeListener(this);
        int themeRes = getThemeRes(wallpaperColorInfo);
        if (themeRes != mThemeRes) {
            mThemeRes = themeRes;
            setTheme(themeRes);
        }
    }

    protected PreferenceFragment getNewFragment() {
        return new LauncherSettingsFragment();
    }

    @Override
    public void onExtractedColorsChanged(WallpaperColorInfo wallpaperColorInfo) {
        if (mThemeRes != getThemeRes(wallpaperColorInfo)) {
            recreate();
        }
    }

    protected int getThemeRes(WallpaperColorInfo wallpaperColorInfo) {
        mThemeStyle = Integer.parseInt(getPrefs(getApplicationContext()).getString(SettingsActivity.PREF_THEME_STYLE_KEY, "0"));
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

    /**
     * This fragment shows the launcher preferences.
     */
    public static class LauncherSettingsFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        private IconBadgingObserver mIconBadgingObserver;

        private String mPreferenceKey;
        private boolean mPreferenceHighlighted = false;
        private boolean mShouldRestart = false;

        private SharedPreferences mPrefs;
        private Preference mGridPref;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                mPreferenceHighlighted = savedInstanceState.getBoolean(SAVE_HIGHLIGHTED_KEY);
            }

            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            addPreferencesFromResource(R.xml.launcher_preferences);

            mPrefs = Utilities.getPrefs(getActivity().getApplicationContext());
            mPrefs.registerOnSharedPreferenceChangeListener(this);

            ContentResolver resolver = getActivity().getContentResolver();

            ButtonPreference iconBadgingPref =
                    (ButtonPreference) findPreference(ICON_BADGING_PREFERENCE_KEY);
            if (!Utilities.ATLEAST_OREO) {
                getPreferenceScreen().removePreference(
                        findPreference(SessionCommitReceiver.ADD_ICON_PREFERENCE_KEY));
                getPreferenceScreen().removePreference(iconBadgingPref);
            } else if (!getResources().getBoolean(R.bool.notification_badging_enabled)) {
                getPreferenceScreen().removePreference(iconBadgingPref);
            } else {
                // Listen to system notification badge settings while this UI is active.
                mIconBadgingObserver = new IconBadgingObserver(
                        iconBadgingPref, resolver, getFragmentManager());
                mIconBadgingObserver.register(NOTIFICATION_BADGING, NOTIFICATION_ENABLED_LISTENERS);
            }

            Preference iconShapeOverride = findPreference(IconShapeOverride.KEY_PREFERENCE);
            if (iconShapeOverride != null) {
                if (IconShapeOverride.isSupported(getActivity())) {
                    IconShapeOverride.handlePreferenceUi((ListPreference) iconShapeOverride);
                } else {
                    getPreferenceScreen().removePreference(iconShapeOverride);
                }
            }

            // Setup allow rotation preference
            Preference rotationPref = findPreference(ALLOW_ROTATION_PREFERENCE_KEY);
            if (getResources().getBoolean(R.bool.allow_rotation)) {
                // Launcher supports rotation by default. No need to show this setting.
                getPreferenceScreen().removePreference(rotationPref);
            } else {
                // Initialize the UI once
                rotationPref.setDefaultValue(getAllowRotationDefaultValue());
            }

            mGridPref = findPreference(KEY_GRID_SIZE);
            if (mGridPref != null) {
                mGridPref.setOnPreferenceClickListener(preference -> {
                    setCustomGridSize();
                    return true;
                });

                mGridPref.setSummary(mPrefs.getString(KEY_GRID_SIZE, getDefaultGridSize()));
            }

            ListPreference iconSizes = (ListPreference) findPreference(ICON_SIZE);
            iconSizes.setSummary(iconSizes.getEntry());
            iconSizes.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int index = iconSizes.findIndexOfValue((String) newValue);
                    iconSizes.setSummary(iconSizes.getEntries()[index]);
                    mShouldRestart = true;
                    return true;
                }
            });

            Preference hiddenApp = findPreference(Utilities.KEY_HIDDEN_APPS);
            hiddenApp.setOnPreferenceClickListener(
                    preference -> {
                        startActivity(new Intent(getActivity(), HiddenAppsActivity.class));
                        return false;
                    });

            ListPreference mThemeStyle = (ListPreference) findPreference(PREF_THEME_STYLE_KEY);
            mThemeStyle.setSummary(mThemeStyle.getEntry());
            mThemeStyle.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int valueIndex = mThemeStyle.findIndexOfValue((String) newValue);
                    mThemeStyle.setSummary(mThemeStyle.getEntries()[valueIndex]);
                    mShouldRestart = true;
                    return true;
                }
            });
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean(SAVE_HIGHLIGHTED_KEY, mPreferenceHighlighted);
        }

        @Override
        public void onResume() {
            super.onResume();

            Intent intent = getActivity().getIntent();
            mPreferenceKey = intent.getStringExtra(EXTRA_FRAGMENT_ARG_KEY);
            if (isAdded() && !mPreferenceHighlighted && !TextUtils.isEmpty(mPreferenceKey)) {
                getView().postDelayed(this::highlightPreference, DELAY_HIGHLIGHT_DURATION_MILLIS);
            }
        }

        private void highlightPreference() {
            Preference pref = findPreference(mPreferenceKey);
            if (pref == null || getPreferenceScreen() == null) {
                return;
            }
            PreferenceScreen screen = getPreferenceScreen();
            if (Utilities.ATLEAST_OREO) {
                screen = selectPreferenceRecursive(pref, screen);
            }
            if (screen == null) {
                return;
            }

            View root = screen.getDialog() != null
                    ? screen.getDialog().getWindow().getDecorView() : getView();
            ListView list = root.findViewById(android.R.id.list);
            if (list == null || list.getAdapter() == null) {
                return;
            }
            Adapter adapter = list.getAdapter();

            // Find the position
            int position = -1;
            for (int i = adapter.getCount() - 1; i >= 0; i--) {
                if (pref == adapter.getItem(i)) {
                    position = i;
                    break;
                }
            }
            new ListViewHighlighter(list, position);
            mPreferenceHighlighted = true;
        }

        @Override
        public void onDestroy() {
            if (mIconBadgingObserver != null) {
                mIconBadgingObserver.unregister();
                mIconBadgingObserver = null;
            }
            mPrefs.unregisterOnSharedPreferenceChangeListener(this);

            if (mShouldRestart) {
                new LooperExecutor(LauncherModel.getWorkerLooper()).execute(
                        new OverrideApplyHandler(getActivity()));
            }
            super.onDestroy();
        }

        @Override
        public void onStop() {
            if (mShouldRestart) {
                new LooperExecutor(LauncherModel.getWorkerLooper()).execute(
                        new OverrideApplyHandler(getActivity()));
            }
            super.onStop();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            switch (key) {
                case KEY_GRID_SIZE:
                    mGridPref.setSummary(mPrefs.getString(KEY_GRID_SIZE, getDefaultGridSize()));
                    mShouldRestart = true;
                    break;
                case KEY_SHOW_DESKTOP_LABELS:
                case KEY_SHOW_DRAWER_LABELS:
                case PREF_NOTIFICATIONS_GESTURE:
                    mShouldRestart = true;
                    break;
            }
        }

        private void setCustomGridSize() {
            int minValue = 3;
            int maxValue = 9;

            String storedValue = mPrefs.getString(KEY_GRID_SIZE, "4x4");
            Pair<Integer, Integer> currentValues = Utilities.extractCustomGrid(storedValue);

            LayoutInflater inflater = (LayoutInflater)
                    getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (inflater == null) {
                return;
            }
            View contentView = inflater.inflate(R.layout.dialog_custom_grid, null);
            NumberPicker columnPicker = contentView.findViewById(R.id.dialog_grid_column);
            NumberPicker rowPicker = contentView.findViewById(R.id.dialog_grid_row);

            columnPicker.setMinValue(minValue);
            rowPicker.setMinValue(minValue);
            columnPicker.setMaxValue(maxValue);
            rowPicker.setMaxValue(maxValue);
            columnPicker.setValue(currentValues.first);
            rowPicker.setValue(currentValues.second);

            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.grid_size_text)
                    .setMessage(R.string.grid_size_custom_message)
                    .setView(contentView)
                    .setPositiveButton(R.string.grid_size_custom_positive, (dialog, i) -> {
                        String newValues = Utilities.getGridValue(columnPicker.getValue(),
                                rowPicker.getValue());
                        mPrefs.edit().putString(KEY_GRID_SIZE, newValues).apply();
                    })
                    .show();
        }

        private String getDefaultGridSize() {
            InvariantDeviceProfile profile = new InvariantDeviceProfile(getActivity());
            return Utilities.getGridValue(profile.numColumns, profile.numRows);
        }

        @TargetApi(Build.VERSION_CODES.O)
        private PreferenceScreen selectPreferenceRecursive(
                Preference pref, PreferenceScreen topParent) {
            if (!(pref.getParent() instanceof PreferenceScreen)) {
                return null;
            }

            PreferenceScreen parent = (PreferenceScreen) pref.getParent();
            if (Objects.equals(parent.getKey(), topParent.getKey())) {
                return parent;
            } else if (selectPreferenceRecursive(parent, topParent) != null) {
                ((PreferenceScreen) parent.getParent())
                        .onItemClick(null, null, parent.getOrder(), 0);
                return parent;
            } else {
                return null;
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

    /**
     * Content observer which listens for system badging setting changes,
     * and updates the launcher badging setting subtext accordingly.
     */
    private static class IconBadgingObserver extends SettingsObserver.Secure
            implements Preference.OnPreferenceClickListener {

        private final ButtonPreference mBadgingPref;
        private final ContentResolver mResolver;
        private final FragmentManager mFragmentManager;

        public IconBadgingObserver(ButtonPreference badgingPref, ContentResolver resolver,
                                   FragmentManager fragmentManager) {
            super(resolver);
            mBadgingPref = badgingPref;
            mResolver = resolver;
            mFragmentManager = fragmentManager;
        }

        @Override
        public void onSettingChanged(boolean enabled) {
            int summary = enabled ? R.string.icon_badging_desc_on : R.string.icon_badging_desc_off;

            boolean serviceEnabled = true;
            if (enabled) {
                // Check if the listener is enabled or not.
                String enabledListeners =
                        Settings.Secure.getString(mResolver, NOTIFICATION_ENABLED_LISTENERS);
                ComponentName myListener =
                        new ComponentName(mBadgingPref.getContext(), NotificationListener.class);
                serviceEnabled = enabledListeners != null &&
                        (enabledListeners.contains(myListener.flattenToString()) ||
                                enabledListeners.contains(myListener.flattenToShortString()));
                if (!serviceEnabled) {
                    summary = R.string.title_missing_notification_access;
                }
            }
            mBadgingPref.setWidgetFrameVisible(!serviceEnabled);
            mBadgingPref.setOnPreferenceClickListener(serviceEnabled ? null : this);
            mBadgingPref.setSummary(summary);

        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            new NotificationAccessConfirmation().show(mFragmentManager, "notification_access");
            return true;
        }
    }

    public static class NotificationAccessConfirmation
            extends DialogFragment implements DialogInterface.OnClickListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            String msg = context.getString(R.string.msg_missing_notification_access,
                    context.getString(R.string.derived_app_name));
            return new AlertDialog.Builder(context)
                    .setTitle(R.string.title_missing_notification_access)
                    .setMessage(msg)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.title_change_settings, this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            ComponentName cn = new ComponentName(getActivity(), NotificationListener.class);
            Bundle showFragmentArgs = new Bundle();
            showFragmentArgs.putString(EXTRA_FRAGMENT_ARG_KEY, cn.flattenToString());

            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(EXTRA_FRAGMENT_ARG_KEY, cn.flattenToString())
                    .putExtra(EXTRA_SHOW_FRAGMENT_ARGS, showFragmentArgs);
            getActivity().startActivity(intent);
        }
    }
}
