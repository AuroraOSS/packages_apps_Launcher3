package com.aurora.launcher.iconpack;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Process;

import com.aurora.launcher.FastBitmapDrawable;
import com.aurora.launcher.ItemInfo;
import com.aurora.launcher.LauncherSettings;
import com.aurora.launcher.Utilities;
import com.aurora.launcher.graphics.DrawableFactory;
import com.aurora.launcher.iconpack.clock.DynamicClock;

public class DynamicDrawableFactory extends DrawableFactory {
    private final DynamicClock mDynamicClockDrawer;

    public DynamicDrawableFactory(Context context) {
        mDynamicClockDrawer = new DynamicClock(context);
    }

    @Override
    public FastBitmapDrawable newIcon(Bitmap icon, ItemInfo info) {
        if (info != null &&
                Utilities.ATLEAST_OREO &&
                info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION &&
                DynamicClock.DESK_CLOCK.equals(info.getTargetComponent()) &&
                info.user.equals(Process.myUserHandle())) {
            return mDynamicClockDrawer.drawIcon(icon);
        }
        return super.newIcon(icon, info);
    }
}
