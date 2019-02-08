package com.android.launcher3.preferences;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragment;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.R;
import com.android.launcher3.SettingsActivity;
import com.android.launcher3.Utilities;

import static com.android.launcher3.SettingsActivity.PREF_THEME_STYLE_KEY;

public class ThemeFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String PREF_ALL_LABEL_RAINBOW = "pref_all_labels_rainbow";
    public static final String PREF_ADAPTIVE_ICONS = "pref_adaptive_icons";
    public static final String PREF_ADAPTIVE_BG = "pref_adaptive_bg";

    private SharedPreferences mPrefs;
    private ActionBar mActionBar;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
        addPreferencesFromResource(R.xml.theme_preferences);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mActionBar = getActivity().getActionBar();
        if (mActionBar != null)
            mActionBar.setTitle(getString(R.string.theme_title));


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
    public void onDestroy() {
        if (mActionBar != null)
            mActionBar.setTitle(getString(R.string.settings_title));
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PREF_ALL_LABEL_RAINBOW:
                SettingsActivity.mShouldRestart = true;
                break;
            case PREF_ADAPTIVE_ICONS:
            case PREF_ADAPTIVE_BG:
                LauncherAppState.getInstance(getActivity()).getIconCache().clear();
                SettingsActivity.mShouldRestart = true;
                break;
        }
    }
}
