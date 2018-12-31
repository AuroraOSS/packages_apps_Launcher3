package com.android.launcher3;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragment;

import static com.android.launcher3.SettingsActivity.PREF_THEME_STYLE_KEY;

public class ThemeFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String PREF_ALL_LABEL_RAINBOW = "pref_all_labels_rainbow";
    public static final String PREF_ADAPTIVE_ICONS = "pref_adaptive_icons";
    public static final String PREF_ADAPTIVE_BG = "pref_adaptive_bg";

    private SharedPreferences mPrefs;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
        addPreferencesFromResource(R.xml.theme_preferences);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPrefs = Utilities.getPrefs(getActivity().getApplicationContext());
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        ListPreference mThemeStyle = (ListPreference) findPreference(PREF_THEME_STYLE_KEY);
        mThemeStyle.setSummary(mThemeStyle.getEntry());
        mThemeStyle.setOnPreferenceChangeListener((preference, newValue) -> {
            int valueIndex = mThemeStyle.findIndexOfValue((String) newValue);
            mThemeStyle.setSummary(mThemeStyle.getEntries()[valueIndex]);
            SettingsActivity.mShouldRestart = true;
            return true;
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PREF_ALL_LABEL_RAINBOW:
            case PREF_ADAPTIVE_ICONS:
            case PREF_ADAPTIVE_BG:
                SettingsActivity.mShouldRestart = true;
                break;
        }
    }
}
