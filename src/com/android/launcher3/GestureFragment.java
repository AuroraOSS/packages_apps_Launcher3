package com.android.launcher3;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import androidx.annotation.Nullable;
import android.view.View;

import static com.android.launcher3.Utilities.PREF_NOTIFICATIONS_GESTURE;
import static com.android.launcher3.states.RotationHelper.ALLOW_ROTATION_PREFERENCE_KEY;
import static com.android.launcher3.states.RotationHelper.getAllowRotationDefaultValue;

public class GestureFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
        addPreferencesFromResource(R.xml.gesture_preferences);
        view.setBackgroundColor(Utilities.getBackgroundColor(getActivity()));
        // Setup allow rotation preference

        getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
        Preference rotationPref = findPreference(ALLOW_ROTATION_PREFERENCE_KEY);
        if (getResources().getBoolean(R.bool.allow_rotation)) {
            // Launcher supports rotation by default. No need to show this setting.
            getPreferenceScreen().removePreference(rotationPref);
        } else {
            // Initialize the UI once
            rotationPref.setDefaultValue(getAllowRotationDefaultValue());
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PREF_NOTIFICATIONS_GESTURE: SettingsActivity.mShouldRestart = true;
                break;
        }
    }
}
