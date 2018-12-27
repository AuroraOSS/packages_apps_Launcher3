package com.android.launcher3;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import androidx.annotation.Nullable;
import android.view.View;

import static com.android.launcher3.SettingsActivity.PREF_THEME_STYLE_KEY;

public class ThemeFragment extends PreferenceFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
        addPreferencesFromResource(R.xml.theme_preferences);
        view.setBackgroundColor(Utilities.getBackgroundColor(getActivity()));

        ListPreference mThemeStyle = (ListPreference) findPreference(PREF_THEME_STYLE_KEY);
        mThemeStyle.setSummary(mThemeStyle.getEntry());
        mThemeStyle.setOnPreferenceChangeListener((preference, newValue) -> {
            int valueIndex = mThemeStyle.findIndexOfValue((String) newValue);
            mThemeStyle.setSummary(mThemeStyle.getEntries()[valueIndex]);
            SettingsActivity.mShouldRestart = true;
            return true;
        });
    }
}
