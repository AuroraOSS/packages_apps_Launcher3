package com.android.launcher3.iconpack;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.UserHandle;

import androidx.preference.PreferenceManager;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.util.ComponentKey;

import java.util.HashSet;
import java.util.Set;

public class AppFilter {
    public final static String HIDE_APPS_PREF = "hidden_app_set";

    public AppFilter(Context context) {
    }

    public static void resetAppFilter(Context context) {
        SharedPreferences.Editor editor = Utilities.getPrefs(context).edit();
        editor.putStringSet(HIDE_APPS_PREF, new HashSet<String>());
        editor.apply();
    }

    public static void setComponentNameState(Context context, ComponentKey key, boolean hidden) {
        String comp = key.componentName.getPackageName();
        Set<String> hiddenApps = getHiddenApps(context);

        while (hiddenApps.contains(comp)) {
            hiddenApps.remove(comp);
        }

        if (hidden != IconUtils.isPackProvider(context, key.componentName.getPackageName())) {
            hiddenApps.add(comp);
        }

        setHiddenApps(context, hiddenApps);

        LauncherModel model = Launcher.getLauncher(context).getModel();
        for (UserHandle user : UserManagerCompat.getInstance(context).getUserProfiles()) {
            model.onPackagesReload(user);
        }
    }

    public static boolean isHiddenApp(Context context, ComponentKey key) {
        return getHiddenApps(context).contains(key.componentName.getPackageName());
    }

    public static Set<String> getHiddenApps(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).getStringSet(HIDE_APPS_PREF, new HashSet<>());
    }

    public static void setHiddenApps(Context context, Set<String> hiddenApps) {
        SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putStringSet(HIDE_APPS_PREF, hiddenApps).apply();
    }
}
