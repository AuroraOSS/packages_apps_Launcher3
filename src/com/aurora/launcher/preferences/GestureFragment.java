package com.aurora.launcher.preferences;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;

import com.aurora.launcher.LauncherFiles;
import com.aurora.launcher.R;
import com.aurora.launcher.SettingsActivity;

import static com.aurora.launcher.Utilities.PREF_NOTIFICATIONS_GESTURE;
import static com.aurora.launcher.states.RotationHelper.ALLOW_ROTATION_PREFERENCE_KEY;
import static com.aurora.launcher.states.RotationHelper.getAllowRotationDefaultValue;

public class GestureFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private ActionBar mActionBar;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
        addPreferencesFromResource(R.xml.gesture_preferences);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mActionBar = getActivity().getActionBar();
        if (mActionBar != null)
            mActionBar.setTitle(getString(R.string.gestures_title));

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
    public void onDestroy() {
        if (mActionBar != null)
            mActionBar.setTitle(getString(R.string.settings_title));
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PREF_NOTIFICATIONS_GESTURE:
                SettingsActivity.mShouldRestart = true;
                break;
        }
    }
}
