package com.aurora.launcher.preferences;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;

import com.aurora.launcher.LauncherAppState;
import com.aurora.launcher.LauncherFiles;
import com.aurora.launcher.R;
import com.aurora.launcher.SettingsActivity;
import com.aurora.launcher.Utilities;
import com.aurora.launcher.graphics.IconShapeOverride;

import static com.aurora.launcher.SettingsActivity.ICON_SIZE;
import static com.aurora.launcher.Utilities.PREF_NOTIFICATIONS_GESTURE;

public class IconFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public final static String ICON_PACK_PREF = "pref_icon_pack";

    private SharedPreferences mPrefs;
    private ActionBar mActionBar;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
        addPreferencesFromResource(R.xml.icon_preferences);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mActionBar = getActivity().getActionBar();
        if (mActionBar != null)
            mActionBar.setTitle(getString(R.string.icons_title));

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
        if (mActionBar != null)
            mActionBar.setTitle(getString(R.string.settings_title));
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        switch (key) {
            case PREF_NOTIFICATIONS_GESTURE:
            case ICON_PACK_PREF:
                LauncherAppState.getInstance(getActivity()).getIconCache().clear();
                SettingsActivity.mShouldRestart = true;
                break;
        }
    }
}
