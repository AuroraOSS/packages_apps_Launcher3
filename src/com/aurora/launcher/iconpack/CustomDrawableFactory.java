package com.aurora.launcher.iconpack;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Process;

import com.aurora.launcher.FastBitmapDrawable;
import com.aurora.launcher.ItemInfo;
import com.aurora.launcher.LauncherModel;
import com.aurora.launcher.LauncherSettings;
import com.aurora.launcher.Utilities;
import com.aurora.launcher.iconpack.clock.CustomClock;
import com.aurora.launcher.iconpack.utils.ActionIntentFilter;
import com.aurora.launcher.util.ComponentKey;
import com.aurora.launcher.util.LooperExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class CustomDrawableFactory extends DynamicDrawableFactory implements Runnable {
    public final Map<ComponentName, Integer> packComponents = new HashMap<>();
    public final Map<ComponentName, String> packCalendars = new HashMap<>();
    public final Map<Integer, CustomClock.Metadata> packClocks = new HashMap<>();
    private final Context mContext;
    private final BroadcastReceiver mAutoUpdatePack;
    public String iconPack;
    private boolean mRegistered = false;
    private CustomClock mCustomClockDrawer;
    private Semaphore waiter = new Semaphore(0);

    public CustomDrawableFactory(Context context) {
        super(context);
        mContext = context;
        mCustomClockDrawer = new CustomClock(context);
        mAutoUpdatePack = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!IconUtils.usingValidPack(context)) {
                    IconUtils.setCurrentPack(context, "");
                }
                IconUtils.applyIconPackAsync(context);
            }
        };

        new LooperExecutor(LauncherModel.getWorkerLooper()).execute(this);
    }

    @Override
    public void run() {
        reloadIconPack();
        waiter.release();
    }

    void reloadIconPack() {
        iconPack = IconUtils.getCurrentPack(mContext);

        if (mRegistered) {
            mContext.unregisterReceiver(mAutoUpdatePack);
            mRegistered = false;
        }
        if (!iconPack.isEmpty()) {
            mContext.registerReceiver(mAutoUpdatePack, ActionIntentFilter.newInstance(iconPack,
                    Intent.ACTION_PACKAGE_CHANGED,
                    Intent.ACTION_PACKAGE_REPLACED,
                    Intent.ACTION_PACKAGE_FULLY_REMOVED),
                    null,
                    new Handler(LauncherModel.getWorkerLooper()));
            mRegistered = true;
        }

        packComponents.clear();
        packCalendars.clear();
        packClocks.clear();
        if (IconUtils.usingValidPack(mContext)) {
            IconUtils.parsePack(this, mContext.getPackageManager(), iconPack);
        }
    }

    public synchronized void ensureInitialLoadComplete() {
        if (waiter != null) {
            waiter.acquireUninterruptibly();
            waiter.release();
            waiter = null;
        }
    }

    @Override
    public FastBitmapDrawable newIcon(Bitmap icon, ItemInfo info) {
        ensureInitialLoadComplete();
        ComponentName componentName = info.getTargetComponent();
        if (packComponents.containsKey(info.getTargetComponent()) &&
                CustomIconProvider.isEnabledForApp(mContext, new ComponentKey(componentName, info.user))) {
            if (Utilities.ATLEAST_OREO &&
                    info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION &&
                    info.user.equals(Process.myUserHandle())) {
                int drawableId = packComponents.get(componentName);
                if (packClocks.containsKey(drawableId)) {
                    Drawable drawable = mContext.getPackageManager().getDrawable(iconPack, drawableId, null);
                    return mCustomClockDrawer.drawIcon(icon, drawable, packClocks.get(drawableId));
                }
            }
            return new FastBitmapDrawable(icon);
        }
        return super.newIcon(icon, info);
    }
}
