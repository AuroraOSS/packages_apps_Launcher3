package com.aurora.launcher;

import android.content.Context;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

public class StringSetAppFilter implements AppFilter {
    private final HashSet<String> mBlackList = new HashSet<>();


    public StringSetAppFilter() {
        mBlackList.add("com.google.android.googlequicksearchbox");
        mBlackList.add("com.google.android.apps.wallpaper");
        mBlackList.add("com.google.android.launcher");
        mBlackList.add("com.aurora.launcher");
    }

    @Override
    public boolean shouldShowApp(String packageName, Context context) {
        Set<String> hiddenApps = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(Utilities.KEY_HIDDEN_APPS_SET, null);

        return !mBlackList.contains(packageName) && (hiddenApps == null || !hiddenApps.contains(packageName));
    }
}
