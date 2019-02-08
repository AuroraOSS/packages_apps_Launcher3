package com.aurora.launcher.preferences;

import android.app.ActionBar;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;

import com.aurora.launcher.LauncherFiles;
import com.aurora.launcher.R;
import com.aurora.launcher.Utilities;

public class SettingsFragment extends PreferenceFragment {

    private static final String FRAGMENT_ICONS = "fragment_icon";
    private static final String FRAGMENT_HOMESCREEN = "fragment_homescreen";
    private static final String FRAGMENT_GESTURE = "fragment_gesture";
    private static final String FRAGMENT_MISC = "fragment_misc";
    private static final String FRAGMENT_DRAWER = "fragment_drawer";

    private ActionBar mActionBar;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
        addPreferencesFromResource(R.xml.preferences);
        mActionBar = getActivity().getActionBar();
        if (mActionBar != null)
            mActionBar.setTitle(getString(R.string.settings_title));
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();
        PreferenceFragment fragment = null;
        switch (key) {
            case FRAGMENT_ICONS:
                fragment = new IconFragment();
                break;
            case FRAGMENT_HOMESCREEN:
                fragment = new HomescreenFragment();
                break;
            case FRAGMENT_DRAWER:
                fragment = new DrawerFragment();
                break;
            case FRAGMENT_GESTURE:
                fragment = new GestureFragment();
                break;
            case FRAGMENT_MISC:
                fragment = new ThemeFragment();
                break;
            case Utilities.KEY_HIDDEN_APPS:
                fragment = new HiddenFragment();
                break;
        }

        if (fragment != null) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .addToBackStack(key)
                    .commit();
            return true;
        } else
            return false;
    }

}
