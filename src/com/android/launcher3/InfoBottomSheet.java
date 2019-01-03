/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.launcher3;

import android.app.AppOpsManager;
import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.android.launcher3.util.PackageManagerHelper;
import com.android.launcher3.widget.WidgetsBottomSheet;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

public class InfoBottomSheet extends WidgetsBottomSheet {

    private static final String PACKAGE_USAGE_STATS = "android.permission.PACKAGE_USAGE_STATS";

    private TextView mTitleTxt;
    private TextView mPackageVerTxt;
    private TextView mPackageNameTxt;
    private ItemInfo mAppInfo;

    private TableLayout mInfoLayout;
    private RelativeLayout mPermLayout;
    private Button mPermButton;
    private Button mUninstallButton;
    private String mPackageName;

    public InfoBottomSheet(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InfoBottomSheet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void populateAndShow(ItemInfo itemInfo) {
        super.populateAndShow(itemInfo);

        mAppInfo = itemInfo;
        mInfoLayout = findViewById(R.id.app_info);
        mPermLayout = findViewById(R.id.app_perm);
        mPermButton = findViewById(R.id.perm_btn);
        mUninstallButton = findViewById(R.id.uninstall_btn);
        mTitleTxt = findViewById(R.id.title);
        mPackageNameTxt = findViewById(R.id.package_name);
        mTitleTxt.setText(itemInfo.title);
        mPackageNameTxt.setText(itemInfo.getTargetComponent().getPackageName());

        ImageView mIcon = findViewById(R.id.icon);
        if (itemInfo instanceof ItemInfoWithIcon) {
            Bitmap bitmap = ((ItemInfoWithIcon) itemInfo).iconBitmap;
            if (bitmap != null)
                mIcon.setImageBitmap(bitmap);
            int color = ((ItemInfoWithIcon) itemInfo).iconColor;
            /*setBackgroundTintList(ColorStateList.valueOf(color));
            mTitleTxt.setTextColor(ColorUtil.manipulateColor(color, .65f));
            mPackageNameTxt.setTextColor(ColorUtil.manipulateColor(color, .75f));*/
        }

        mIcon.setOnClickListener(v -> {
            new PackageManagerHelper(getContext()).startDetailsActivityForInfo(
                    itemInfo, null, null);
        });

        mPackageName = mAppInfo.getTargetComponent().getPackageName();
        fetchAppInfo();

        if (isPermGranted()) {
            mInfoLayout.setVisibility(VISIBLE);
            mUninstallButton.setVisibility(VISIBLE);
            getPackageStats();
        } else {
            mPermLayout.setVisibility(VISIBLE);
            mPermButton.setOnClickListener(v -> mLauncher.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        }

        mUninstallButton.setOnClickListener(v -> {
            try {
                Intent i = Intent.parseUri(mLauncher.getString(R.string.delete_package_intent), 0)
                        .setData(Uri.fromParts("package", mPackageName, itemInfo.getTargetComponent().getClassName()))
                        .putExtra(Intent.EXTRA_USER, itemInfo.user);
                mLauncher.startActivity(i);
                close(true);
            } catch (URISyntaxException ignored) {
            }
        });

    }

    @Override
    protected void onWidgetsBound() {
    }

    private void fetchAppInfo() {

        try {
            PackageManager packageManager = mLauncher.getPackageManager();
            PackageInfo packageInfo =
                    packageManager.getPackageInfo(mPackageName, PackageManager.GET_PERMISSIONS);
            mPackageVerTxt = findViewById(R.id.package_version);
            StringBuilder appInfo = new StringBuilder();
            appInfo.append("v");
            appInfo.append(packageInfo.versionName);
            appInfo.append(" • ");
            appInfo.append(Utilities.isSystemApp(getContext(), mPackageName)
                    ? getResources().getString(R.string.app_system)
                    : getResources().getString(R.string.app_user));
            mPackageVerTxt.setText(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void getPackageStats() {
        mInfoLayout.setVisibility(VISIBLE);
        final StorageStatsManager ssm = (StorageStatsManager)
                getContext().getSystemService(Context.STORAGE_STATS_SERVICE);
        final StorageManager sm = (StorageManager)
                getContext().getSystemService(Context.STORAGE_SERVICE);
        final List<StorageVolume> sv = sm.getStorageVolumes();
        final UserHandle user = getContext().getUser();
        for (StorageVolume storageVolume : sv) {
            try {
                final String uuidStr = storageVolume.getUuid();
                final UUID uuid = uuidStr == null
                        ? StorageManager.UUID_DEFAULT
                        : UUID.fromString(uuidStr);
                final StorageStats storageStats =
                        ssm.queryStatsForPackage(uuid, mPackageName, user);
                ((TextView) findViewById(R.id.AppSize)).setText(
                        Formatter.formatShortFileSize(getContext(), storageStats.getAppBytes())
                );
                ((TextView) findViewById(R.id.UserData)).setText(
                        Formatter.formatShortFileSize(getContext(), storageStats.getDataBytes())
                );
                ((TextView) findViewById(R.id.CacheSize)).setText(
                        Formatter.formatShortFileSize(getContext(), storageStats.getCacheBytes())
                );
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception ignored) {
            }
        }
    }

    private boolean isPermGranted() {
        AppOpsManager appOps = (AppOpsManager) getContext()
                .getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getContext().getPackageName());

        if (mode == AppOpsManager.MODE_DEFAULT) {
            return (getContext().checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            return (mode == AppOpsManager.MODE_ALLOWED);
        }
    }
}
