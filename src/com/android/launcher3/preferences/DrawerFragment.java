package com.android.launcher3.preferences;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;

import com.android.launcher3.LauncherFiles;
import com.android.launcher3.R;
import com.android.launcher3.SettingsActivity;
import com.android.launcher3.Utilities;

public class DrawerFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String PREF_DRAWER_BG_CUSTOMIZATION = "pref_drawer_background_customization";
    public static final String PREF_DRAWER_LABEL_CUSTOMIZATION = "pref_drawer_label_customization";
    public static final String PREF_DRAWER_BG_COLOR = "pref_drawer_bg_color";
    public static final String PREF_DRAWER_LABEL_COLOR = "pref_drawer_label_color";
    public static final String PREF_DRAWER_LABEL_STYLE = "pref_drawer_label_style";
    public static final String PREF_DRAWER_LABEL_SHADOW = "pref_drawer_label_shadow";
    public static final String PREF_DRAWER_LABEL_CASE = "pref_drawer_label_allCaps";
    public static final String PREF_DRAWER_LABEL_SIZE = "pref_drawer_label_size";
    public static final String PREF_DRAWER_LABEL_VISIBILITY = "pref_drawer_show_labels";
    public static final String PREF_DRAWER_LABEL_LINE = "pref_drawer_label_line";

    private SharedPreferences mPrefs;
    private ActionBar mActionBar;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
        addPreferencesFromResource(R.xml.drawer_preferences);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mActionBar = getActivity().getActionBar();
        if (mActionBar != null)
            mActionBar.setTitle(getString(R.string.appdrawer_title));

        mPrefs = Utilities.getPrefs(getActivity().getApplicationContext());
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        ListPreference mLabelStyle = (ListPreference) findPreference(PREF_DRAWER_LABEL_STYLE);
        mLabelStyle.setSummary(mLabelStyle.getEntry());
        mLabelStyle.setOnPreferenceChangeListener((preference, newValue) -> {
            int valueIndex = mLabelStyle.findIndexOfValue((String) newValue);
            mLabelStyle.setSummary(mLabelStyle.getEntries()[valueIndex]);
            SettingsActivity.mShouldRestart = true;
            return true;
        });

        ListPreference mLabelSize = (ListPreference) findPreference(PREF_DRAWER_LABEL_SIZE);
        mLabelSize.setSummary(mLabelSize.getEntry());
        mLabelSize.setOnPreferenceChangeListener((preference, newValue) -> {
            int valueIndex = mLabelSize.findIndexOfValue((String) newValue);
            mLabelSize.setSummary(mLabelSize.getEntries()[valueIndex]);
            SettingsActivity.mShouldRestart = true;
            return true;
        });
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();
        switch (key) {
            case PREF_DRAWER_BG_COLOR:
            case PREF_DRAWER_LABEL_COLOR:
                SettingsActivity.mShouldRestart = true;
                break;
        }
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        switch (key) {
            case PREF_DRAWER_BG_CUSTOMIZATION:
            case PREF_DRAWER_LABEL_CUSTOMIZATION:
            case PREF_DRAWER_LABEL_SHADOW:
            case PREF_DRAWER_LABEL_CASE:
            case PREF_DRAWER_LABEL_VISIBILITY:
            case PREF_DRAWER_LABEL_LINE:
                SettingsActivity.mShouldRestart = true;
                break;
        }
    }

    @Override
    public void onDestroy() {
        if (mActionBar != null)
            mActionBar.setTitle(getString(R.string.settings_title));
        super.onDestroy();
    }
}

