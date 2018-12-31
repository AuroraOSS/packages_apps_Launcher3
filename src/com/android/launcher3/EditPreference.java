package com.android.launcher3;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;

import com.android.launcher3.iconpack.AppFilter;
import com.android.launcher3.util.ComponentKey;

public class EditPreference extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    private final static String PREF_HIDE = "pref_app_hide";

    private ComponentKey mKey;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.edit_preferences);
    }

    public void loadForApp(ItemInfo itemInfo) {
        mKey = new ComponentKey(itemInfo.getTargetComponent(), itemInfo.user);
        SwitchPreference mPrefHide = (SwitchPreference) findPreference(PREF_HIDE);
        Context context = getActivity();
        mPrefHide.setChecked(AppFilter.isHiddenApp(context, mKey));
        mPrefHide.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (boolean) newValue;
        Launcher launcher = Launcher.getLauncher(getActivity());
        switch (preference.getKey()) {
            case PREF_HIDE:
                AppFilter.setComponentNameState(launcher, mKey, enabled);
                Launcher.getLauncher(getActivity()).getModel().forceReload();
                break;
        }
        return true;
    }
}
