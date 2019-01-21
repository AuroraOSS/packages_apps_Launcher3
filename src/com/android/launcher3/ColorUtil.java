package com.android.launcher3;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.ColorInt;

import com.android.launcher3.uioverrides.WallpaperColorInfo;

import static com.android.launcher3.SettingsActivity.PREF_THEME_STYLE_KEY;
import static com.android.launcher3.Utilities.getPrefs;

public class ColorUtil {

    public static int manipulateColor(@ColorInt int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a,
                Math.min(r, 255),
                Math.min(g, 255),
                Math.min(b, 255));
    }

    /**
     * Taken from AppThemeHelper @author Karim Abou Zeid (kabouzeid)
     */

    public static boolean isColorLight(@ColorInt int color) {
        final double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness < 0.4;
    }

    public static boolean isDark(Context context) {
        int mThemeStyle = Integer.parseInt(getPrefs(context).getString(PREF_THEME_STYLE_KEY, "0"));
        WallpaperColorInfo wallpaperColorInfo = WallpaperColorInfo.getInstance(context);
        if (mThemeStyle == 0 && wallpaperColorInfo.isDark())
            return wallpaperColorInfo.supportsDarkText();
        else if (mThemeStyle == 1)
            return false;
        else
            return true;
    }

}
