package com.aurora.launcher.preferences;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;

import com.aurora.launcher.InvariantDeviceProfile;
import com.aurora.launcher.LauncherFiles;
import com.aurora.launcher.R;
import com.aurora.launcher.SessionCommitReceiver;
import com.aurora.launcher.SettingsActivity;
import com.aurora.launcher.Utilities;
import com.aurora.launcher.notification.NotificationListener;
import com.aurora.launcher.util.SettingsObserver;
import com.aurora.launcher.views.ButtonPreference;

import static com.aurora.launcher.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY;
import static com.aurora.launcher.SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGS;
import static com.aurora.launcher.SettingsActivity.ICON_BADGING_PREFERENCE_KEY;
import static com.aurora.launcher.SettingsActivity.KEY_GRID_SIZE;
import static com.aurora.launcher.SettingsActivity.KEY_SHOW_DESKTOP_LABELS;
import static com.aurora.launcher.SettingsActivity.NOTIFICATION_BADGING;
import static com.aurora.launcher.SettingsActivity.NOTIFICATION_ENABLED_LISTENERS;
import static com.aurora.launcher.Utilities.PREF_NOTIFICATIONS_GESTURE;

public class HomescreenFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String PREF_HOME_LABEL_CUSTOMIZATION = "pref_home_label_customization";
    public static final String PREF_HOME_LABEL_COLOR = "pref_home_label_color";
    public static final String PREF_HOME_LABEL_STYLE = "pref_home_label_style";
    public static final String PREF_HOME_LABEL_SHADOW = "pref_home_label_shadow";
    public static final String PREF_HOME_LABEL_CASE = "pref_home_label_allCaps";
    public static final String PREF_HOME_LABEL_SIZE = "pref_home_label_size";
    public static final String PREF_HOME_LABEL_VISIBILITY = "pref_home_show_labels";
    public static final String PREF_HOME_LABEL_LINE = "pref_home_label_line";
    public static final String PREF_BOTTOM_OPTIONS = "pref_bottom_option";
    public static final String PREF_HOME_SCRIM = "pref_home_scrim";

    private IconBadgingObserver mIconBadgingObserver;

    private SharedPreferences mPrefs;
    private Preference mGridPref;
    private ActionBar mActionBar;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
        addPreferencesFromResource(R.xml.homescreen_preferences);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mActionBar = getActivity().getActionBar();
        if (mActionBar != null)
            mActionBar.setTitle(getString(R.string.homescreen_title));

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

        mGridPref = findPreference(KEY_GRID_SIZE);
        if (mGridPref != null) {
            mGridPref.setOnPreferenceClickListener(preference -> {
                setCustomGridSize();
                return true;
            });

            mGridPref.setSummary(mPrefs.getString(KEY_GRID_SIZE, getDefaultGridSize()));
        }

        ListPreference mLabelStyle = (ListPreference) findPreference(PREF_HOME_LABEL_STYLE);
        mLabelStyle.setSummary(mLabelStyle.getEntry());
        mLabelStyle.setOnPreferenceChangeListener((preference, newValue) -> {
            int valueIndex = mLabelStyle.findIndexOfValue((String) newValue);
            mLabelStyle.setSummary(mLabelStyle.getEntries()[valueIndex]);
            SettingsActivity.mShouldRestart = true;
            return true;
        });

        ListPreference mLabelSize = (ListPreference) findPreference(PREF_HOME_LABEL_SIZE);
        mLabelSize.setSummary(mLabelSize.getEntry());
        mLabelSize.setOnPreferenceChangeListener((preference, newValue) -> {
            int valueIndex = mLabelSize.findIndexOfValue((String) newValue);
            mLabelSize.setSummary(mLabelSize.getEntries()[valueIndex]);
            SettingsActivity.mShouldRestart = true;
            return true;
        });

    }

    @Override
    public void onDestroy() {
        if (mIconBadgingObserver != null) {
            mIconBadgingObserver.unregister();
            mIconBadgingObserver = null;
        }
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        if (mActionBar != null)
            mActionBar.setTitle(getString(R.string.settings_title));
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();
        switch (key) {
            case PREF_HOME_LABEL_COLOR:
                SettingsActivity.mShouldRestart = true;
                break;
        }
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        switch (key) {
            case KEY_GRID_SIZE:
                mGridPref.setSummary(mPrefs.getString(KEY_GRID_SIZE, getDefaultGridSize()));
                SettingsActivity.mShouldRestart = true;
                break;
            case KEY_SHOW_DESKTOP_LABELS:
            case PREF_NOTIFICATIONS_GESTURE:
            case PREF_HOME_LABEL_CUSTOMIZATION:
            case PREF_HOME_LABEL_SHADOW:
            case PREF_HOME_LABEL_CASE:
            case PREF_HOME_LABEL_VISIBILITY:
            case PREF_HOME_LABEL_LINE:
            case PREF_HOME_SCRIM:
                SettingsActivity.mShouldRestart = true;
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

    public static class IconBadgingObserver extends SettingsObserver.Secure
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
