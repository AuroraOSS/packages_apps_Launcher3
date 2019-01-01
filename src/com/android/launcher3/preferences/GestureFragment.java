package com.android.launcher3.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;

import com.android.launcher3.LauncherFiles;
import com.android.launcher3.R;
import com.android.launcher3.SettingsActivity;

import static com.android.launcher3.Utilities.PREF_DOUBLETAP_GESTURE;
import static com.android.launcher3.Utilities.PREF_NOTIFICATIONS_GESTURE;
import static com.android.launcher3.states.RotationHelper.ALLOW_ROTATION_PREFERENCE_KEY;
import static com.android.launcher3.states.RotationHelper.getAllowRotationDefaultValue;

public class GestureFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
        addPreferencesFromResource(R.xml.gesture_preferences);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
            case PREF_NOTIFICATIONS_GESTURE:
            case PREF_DOUBLETAP_GESTURE:
                SettingsActivity.mShouldRestart = true;
                break;
        }
    }
}
