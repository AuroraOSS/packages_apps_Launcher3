package com.android.launcher3;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.View;

public class SettingsFragment extends PreferenceFragment {

    private static final String FRAGMENT_ICONS = "fragment_icon";
    private static final String FRAGMENT_HOMESCREEN = "fragment_homescreen";
    private static final String FRAGMENT_GESTURE = "fragment_gesture";
    private static final String FRAGMENT_MISC = "fragment_misc";
    private static final String FRAGMENT_DRAWER = "fragment_drawer";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
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
                    .add(android.R.id.content, fragment)
                    .addToBackStack(key)
                    .commit();
            return true;
        } else
            return false;
    }

}
