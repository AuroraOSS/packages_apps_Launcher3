/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aurora.launcher.compat;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.annotation.Nullable;

import com.aurora.launcher.LauncherAppWidgetInfo;
import com.aurora.launcher.LauncherAppWidgetProviderInfo;
import com.aurora.launcher.Utilities;
import com.aurora.launcher.config.FeatureFlags;
import com.aurora.launcher.util.ComponentKey;
import com.aurora.launcher.util.PackageUserKey;
import com.aurora.launcher.widget.custom.CustomWidgetParser;

import java.util.HashMap;
import java.util.List;

public abstract class AppWidgetManagerCompat {

    private static final Object sInstanceLock = new Object();
    private static AppWidgetManagerCompat sInstance;
    final AppWidgetManager mAppWidgetManager;
    final Context mContext;

    AppWidgetManagerCompat(Context context) {
        mContext = context;
        mAppWidgetManager = AppWidgetManager.getInstance(context);
    }

    public static AppWidgetManagerCompat getInstance(Context context) {
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                if (Utilities.ATLEAST_OREO) {
                    sInstance = new AppWidgetManagerCompatVO(context.getApplicationContext());
                } else {
                    sInstance = new AppWidgetManagerCompatVL(context.getApplicationContext());
                }
            }
            return sInstance;
        }
    }

    public LauncherAppWidgetProviderInfo getLauncherAppWidgetInfo(int appWidgetId) {
        if (FeatureFlags.ENABLE_CUSTOM_WIDGETS
                && appWidgetId <= LauncherAppWidgetInfo.CUSTOM_WIDGET_ID) {
            return CustomWidgetParser.getWidgetProvider(mContext, appWidgetId);
        }

        AppWidgetProviderInfo info = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        return info == null ? null : LauncherAppWidgetProviderInfo.fromProviderInfo(mContext, info);
    }

    public abstract List<AppWidgetProviderInfo> getAllProviders(
            @Nullable PackageUserKey packageUser);

    public abstract boolean bindAppWidgetIdIfAllowed(
            int appWidgetId, AppWidgetProviderInfo info, Bundle options);

    public abstract LauncherAppWidgetProviderInfo findProvider(
            ComponentName provider, UserHandle user);

    public abstract HashMap<ComponentKey, AppWidgetProviderInfo> getAllProvidersMap();
}
