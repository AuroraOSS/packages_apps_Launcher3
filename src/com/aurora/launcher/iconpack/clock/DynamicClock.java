package com.aurora.launcher.iconpack.clock;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.aurora.launcher.LauncherAppState;
import com.aurora.launcher.LauncherModel;
import com.aurora.launcher.MainThreadExecutor;
import com.aurora.launcher.Utilities;
import com.aurora.launcher.config.FeatureFlags;
import com.aurora.launcher.graphics.IconNormalizer;
import com.aurora.launcher.iconpack.utils.ActionIntentFilter;
import com.aurora.launcher.util.Preconditions;

import java.util.Collections;
import java.util.Set;
import java.util.TimeZone;
import java.util.WeakHashMap;

public class DynamicClock extends BroadcastReceiver {
    public static final ComponentName DESK_CLOCK = new ComponentName("com.google.android.deskclock", "com.android.deskclock.DeskClock");
    private final Set<AutoUpdateClock> mUpdaters;
    private final Context mContext;
    private ClockLayers mLayers;

    public DynamicClock(Context context) {
        mUpdaters = Collections.newSetFromMap(new WeakHashMap<AutoUpdateClock, Boolean>());
        mLayers = new ClockLayers();
        mContext = context;
        final Handler handler = new Handler(LauncherModel.getWorkerLooper());
        mContext.registerReceiver(this,
                ActionIntentFilter.newInstance("com.google.android.deskclock",
                        Intent.ACTION_PACKAGE_ADDED,
                        Intent.ACTION_PACKAGE_CHANGED),
                null, handler);
        handler.post(new Runnable() {
            @Override
            public void run() {
                updateMainThread();
            }
        });

        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadTimeZone(intent.getStringExtra("time-zone"));
            }
        }, new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED), null, new Handler(Looper.getMainLooper()));
    }

    public static Drawable getClock(Context context, int iconDpi) {
        ClockLayers clone = getClockLayers(context, iconDpi, false).clone();
        if (clone != null) {
            clone.updateAngles();
            return clone.mDrawable;
        }
        return null;
    }

    private static ClockLayers getClockLayers(Context context, int iconDpi, boolean normalizeIcon) {
        Preconditions.assertWorkerThread();
        ClockLayers layers = new ClockLayers();
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo("com.google.android.deskclock", PackageManager.GET_META_DATA | PackageManager.GET_UNINSTALLED_PACKAGES);
            Bundle metaData = applicationInfo.metaData;
            if (metaData != null) {
                int levelPerTickIcon = metaData.getInt("com.aurora.launcher.LEVEL_PER_TICK_ICON_ROUND", 0);
                if (levelPerTickIcon != 0) {
                    Drawable drawableForDensity = packageManager.getResourcesForApplication(applicationInfo).getDrawableForDensity(levelPerTickIcon, iconDpi);
                    layers.mDrawable = drawableForDensity.mutate();
                    layers.mHourIndex = metaData.getInt("hourIndex", -1);
                    layers.mMinuteIndex = metaData.getInt("minuteIndex", -1);
                    layers.mSecondIndex = metaData.getInt("secondIndex", -1);
                    layers.mDefaultHour = metaData.getInt("defaultHour", 0);
                    layers.mDefaultMinute = metaData.getInt("defaultMinute", 0);
                    layers.mDefaultSecond = metaData.getInt("defaultSecond", 0);
                    if (normalizeIcon) {
                        layers.scale = IconNormalizer.getInstance(context).getScale(layers.mDrawable, null, null, null);
                    }

                    LayerDrawable layerDrawable = layers.getLayerDrawable();
                    int numberOfLayers = layerDrawable.getNumberOfLayers();

                    if (layers.mHourIndex < 0 || layers.mHourIndex >= numberOfLayers) {
                        layers.mHourIndex = -1;
                    }
                    if (layers.mMinuteIndex < 0 || layers.mMinuteIndex >= numberOfLayers) {
                        layers.mMinuteIndex = -1;
                    }
                    if (layers.mSecondIndex < 0 || layers.mSecondIndex >= numberOfLayers) {
                        layers.mSecondIndex = -1;
                    } else if (Utilities.ATLEAST_MARSHMALLOW) {
                        layerDrawable.setDrawable(layers.mSecondIndex, null);
                        layers.mSecondIndex = -1;
                    }
                }
            }
        } catch (Exception e) {
            layers.mDrawable = null;
        }
        return layers;
    }

    private void loadTimeZone(String timeZoneId) {
        TimeZone timeZone = timeZoneId == null ?
                TimeZone.getDefault() :
                TimeZone.getTimeZone(timeZoneId);

        for (AutoUpdateClock a : mUpdaters) {
            a.setTimeZone(timeZone);
        }
    }

    private void updateMainThread() {
        new MainThreadExecutor().execute(() -> updateWrapper(getClockLayers(mContext,
                LauncherAppState.getIDP(mContext).fillResIconDpi,
                !FeatureFlags.LAUNCHER3_DISABLE_ICON_NORMALIZATION)));
    }

    private void updateWrapper(ClockLayers wrapper) {
        this.mLayers = wrapper;
        for (AutoUpdateClock updater : mUpdaters) {
            updater.updateLayers(wrapper.clone());
        }
    }

    public AutoUpdateClock drawIcon(Bitmap bitmap) {
        final AutoUpdateClock updater = new AutoUpdateClock(bitmap, mLayers.clone());
        mUpdaters.add(updater);
        return updater;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        updateMainThread();
    }
}
