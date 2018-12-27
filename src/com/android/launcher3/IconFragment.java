package com.android.launcher3;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.view.View;

import com.android.launcher3.graphics.IconShapeOverride;

import static com.android.launcher3.SettingsActivity.ICON_SIZE;
import static com.android.launcher3.Utilities.PREF_NOTIFICATIONS_GESTURE;

public class IconFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences mPrefs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
        addPreferencesFromResource(R.xml.icon_preferences);
        view.setBackgroundColor(Utilities.getBackgroundColor(getActivity()));
        mPrefs = Utilities.getPrefs(getActivity().getApplicationContext());
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        Preference iconShapeOverride = findPreference(IconShapeOverride.KEY_PREFERENCE);
        if (iconShapeOverride != null) {
            if (IconShapeOverride.isSupported(getActivity())) {
                IconShapeOverride.handlePreferenceUi((ListPreference) iconShapeOverride);
            } else {
                getPreferenceScreen().removePreference(iconShapeOverride);
            }
        }

        ListPreference iconSizes = (ListPreference) findPreference(ICON_SIZE);
        iconSizes.setSummary(iconSizes.getEntry());
        iconSizes.setOnPreferenceChangeListener((preference, newValue) -> {
            int index = iconSizes.findIndexOfValue((String) newValue);
            iconSizes.setSummary(iconSizes.getEntries()[index]);
            SettingsActivity.mShouldRestart = true;
            return true;
        });
    }


    @Override
    public void onDestroy() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        switch (key) {
            case PREF_NOTIFICATIONS_GESTURE:
                SettingsActivity.mShouldRestart = true;
                break;
        }
    }
}
