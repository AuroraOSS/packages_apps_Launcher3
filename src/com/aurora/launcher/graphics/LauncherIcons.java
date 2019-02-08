/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.aurora.launcher.graphics;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;

import androidx.annotation.Nullable;

import com.aurora.launcher.AppInfo;
import com.aurora.launcher.ColorUtil;
import com.aurora.launcher.FastBitmapDrawable;
import com.aurora.launcher.IconCache;
import com.aurora.launcher.InvariantDeviceProfile;
import com.aurora.launcher.ItemInfoWithIcon;
import com.aurora.launcher.LauncherAppState;
import com.aurora.launcher.R;
import com.aurora.launcher.Utilities;
import com.aurora.launcher.model.PackageItemInfo;
import com.aurora.launcher.shortcuts.DeepShortcutManager;
import com.aurora.launcher.shortcuts.ShortcutInfoCompat;
import com.aurora.launcher.util.Provider;
import com.aurora.launcher.util.Themes;

import static android.graphics.Paint.DITHER_FLAG;
import static android.graphics.Paint.FILTER_BITMAP_FLAG;
import static com.aurora.launcher.graphics.ShadowGenerator.BLUR_FACTOR;

/**
 * Helper methods for generating various launcher icons
 */
public class LauncherIcons implements AutoCloseable {

    public static final Object sPoolSync = new Object();
    private static final int DEFAULT_WRAPPER_BACKGROUND = Color.WHITE;
    private static LauncherIcons sPool;
    private final Rect mOldBounds = new Rect();
    private final Context mContext;
    private final Canvas mCanvas;
    private final PackageManager mPm;
    private final int mFillResIconDpi;
    private final int mIconBitmapSize;
    private IconNormalizer mNormalizer;
    private ShadowGenerator mShadowGenerator;
    private Drawable mWrapperIcon;
    private int mWrapperBackgroundColor = DEFAULT_WRAPPER_BACKGROUND;
    // sometimes we store linked lists of these things
    private LauncherIcons next;

    private LauncherIcons(Context context) {
        mContext = context.getApplicationContext();
        mPm = mContext.getPackageManager();

        InvariantDeviceProfile idp = LauncherAppState.getIDP(mContext);
        mFillResIconDpi = idp.fillResIconDpi;
        mIconBitmapSize = idp.iconBitmapSize;

        mCanvas = new Canvas();
        mCanvas.setDrawFilter(new PaintFlagsDrawFilter(DITHER_FLAG, FILTER_BITMAP_FLAG));
    }

    /**
     * Return a new Message instance from the global pool. Allows us to
     * avoid allocating new objects in many cases.
     */
    public static LauncherIcons obtain(Context context) {
        synchronized (sPoolSync) {
            if (sPool != null) {
                LauncherIcons m = sPool;
                sPool = m.next;
                m.next = null;
                return m;
            }
        }
        return new LauncherIcons(context);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    /**
     * Recycles a LauncherIcons that may be in-use.
     */
    public void recycle() {
        synchronized (sPoolSync) {
            // Clear any temporary state variables
            mWrapperBackgroundColor = DEFAULT_WRAPPER_BACKGROUND;

            next = sPool;
            sPool = this;
        }
    }

    @Override
    public void close() {
        recycle();
    }

    public ShadowGenerator getShadowGenerator() {
        if (mShadowGenerator == null) {
            mShadowGenerator = new ShadowGenerator(mContext);
        }
        return mShadowGenerator;
    }

    public IconNormalizer getNormalizer() {
        if (mNormalizer == null) {
            mNormalizer = new IconNormalizer(mContext);
        }
        return mNormalizer;
    }

    /**
     * Returns a bitmap suitable for the all apps view. If the package or the resource do not
     * exist, it returns null.
     */
    public BitmapInfo createIconBitmap(ShortcutIconResource iconRes) {
        try {
            Resources resources = mPm.getResourcesForApplication(iconRes.packageName);
            if (resources != null) {
                final int id = resources.getIdentifier(iconRes.resourceName, null, null);
                // do not stamp old legacy shortcuts as the app may have already forgotten about it
                return createBadgedIconBitmap(
                        resources.getDrawableForDensity(id, mFillResIconDpi),
                        Process.myUserHandle() /* only available on primary user */,
                        0 /* do not apply legacy treatment */);
            }
        } catch (Exception e) {
            // Icon not found.
        }
        return null;
    }

    /**
     * Returns a bitmap which is of the appropriate size to be displayed as an icon
     */
    public BitmapInfo createIconBitmap(Bitmap icon) {
        if (mIconBitmapSize == icon.getWidth() && mIconBitmapSize == icon.getHeight()) {
            return BitmapInfo.fromBitmap(icon);
        }
        return BitmapInfo.fromBitmap(
                createIconBitmap(new BitmapDrawable(mContext.getResources(), icon), 1f));
    }

    /**
     * Returns a bitmap suitable for displaying as an icon at various launcher UIs like all apps
     * view or workspace. The icon is badged for {@param user}.
     * The bitmap is also visually normalized with other icons.
     */
    public BitmapInfo createBadgedIconBitmap(Drawable icon, UserHandle user, int iconAppTargetSdk) {
        return createBadgedIconBitmap(icon, user, iconAppTargetSdk, false);
    }

    public BitmapInfo createBadgedIconBitmap(Drawable icon, UserHandle user, int iconAppTargetSdk,
                                             boolean isInstantApp) {
        return createBadgedIconBitmap(icon, user, iconAppTargetSdk, isInstantApp, null);
    }

    /**
     * Returns a bitmap suitable for displaying as an icon at various launcher UIs like all apps
     * view or workspace. The icon is badged for {@param user}.
     * The bitmap is also visually normalized with other icons.
     */
    public BitmapInfo createBadgedIconBitmap(Drawable icon, UserHandle user, int iconAppTargetSdk,
                                             boolean isInstantApp, float[] scale) {
        if (scale == null) {
            scale = new float[1];
        }
        icon = normalizeAndWrapToAdaptiveIcon(icon, iconAppTargetSdk, null, scale, user);
        Bitmap bitmap = createIconBitmap(icon, scale[0]);
        if (Utilities.ATLEAST_OREO && icon instanceof AdaptiveIconDrawable) {
            mCanvas.setBitmap(bitmap);
            getShadowGenerator().recreateIcon(Bitmap.createBitmap(bitmap), mCanvas);
            mCanvas.setBitmap(null);
        }

        final Bitmap result;
        if (user != null && !Process.myUserHandle().equals(user)) {
            BitmapDrawable drawable = new FixedSizeBitmapDrawable(bitmap);
            Drawable badged = mPm.getUserBadgedIcon(drawable, user);
            if (badged instanceof BitmapDrawable) {
                result = ((BitmapDrawable) badged).getBitmap();
            } else {
                result = createIconBitmap(badged, 1f);
            }
        } else if (isInstantApp) {
            badgeWithDrawable(bitmap, mContext.getDrawable(R.drawable.ic_instant_app_badge));
            result = bitmap;
        } else {
            result = bitmap;
        }

        return BitmapInfo.fromBitmap(result);
    }

    /**
     * Creates a normalized bitmap suitable for the all apps view. The bitmap is also visually
     * normalized with other icons and has enough spacing to add shadow.
     */
    public Bitmap createScaledBitmapWithoutShadow(Drawable icon, int iconAppTargetSdk) {
        RectF iconBounds = new RectF();
        float[] scale = new float[1];
        icon = normalizeAndWrapToAdaptiveIcon(icon, iconAppTargetSdk, iconBounds, scale, null);
        return createIconBitmap(icon,
                Math.min(scale[0], ShadowGenerator.getScaleForBounds(iconBounds)));
    }

    /**
     * Sets the background color used for wrapped adaptive icon
     */
    public void setWrapperBackgroundColor(int color) {
        mWrapperBackgroundColor = (Color.alpha(color) < 255) ? DEFAULT_WRAPPER_BACKGROUND : color;
    }

    private Drawable normalizeAndWrapToAdaptiveIcon(Drawable icon, int iconAppTargetSdk,
                                                    RectF outIconBounds, float[] outScale, UserHandle user) {
        float scale = 1f;
        if (Utilities.isAdaptiveLegacyEnabled(mContext)) {
            BitmapInfo mBitmapInfo = null;
            int colorBackground = Color.WHITE;

            if (user != null) {
                BitmapDrawable drawable = new FixedSizeBitmapDrawable(drawableToBitmap(icon));
                Drawable badged = mPm.getUserBadgedIcon(drawable, user);
                Bitmap result = ((BitmapDrawable) badged).getBitmap();
                mBitmapInfo = BitmapInfo.fromBitmap(result);
            }

            if ((Utilities.ATLEAST_OREO && iconAppTargetSdk >= Build.VERSION_CODES.O) ||
                    Utilities.ATLEAST_P) {
                boolean[] outShape = new boolean[1];
                if (mWrapperIcon == null) {
                    mWrapperIcon = mContext.getDrawable(R.drawable.adaptive_icon_drawable_wrapper)
                            .mutate();
                }
                AdaptiveIconDrawable dr = (AdaptiveIconDrawable) mWrapperIcon;
                dr.setBounds(0, 0, 1, 1);
                scale = getNormalizer().getScale(icon, outIconBounds, dr.getIconMask(), outShape);
                if (Utilities.ATLEAST_OREO && !outShape[0] && !(icon instanceof AdaptiveIconDrawable)) {
                    FixedScaleDrawable fsd = ((FixedScaleDrawable) dr.getForeground());
                    fsd.setDrawable(icon);
                    fsd.setScale(scale);
                    icon = dr;
                    scale = getNormalizer().getScale(icon, outIconBounds, null, null);

                    if (Utilities.isAdaptiveBackground(mContext) && mBitmapInfo != null) {
                        colorBackground = getAdaptiveBgColor(mBitmapInfo);
                    }

                    ((ColorDrawable) dr.getBackground()).setColor(mBitmapInfo == null
                            ? mWrapperBackgroundColor
                            : colorBackground);
                }
            } else {
                scale = getNormalizer().getScale(icon, outIconBounds, null, null);
            }
        }
        outScale[0] = scale;
        return icon;
    }

    private int getAdaptiveBgColor(BitmapInfo mBitmapInfo) {
        int colorBackground = Color.WHITE;
        if (ColorUtil.isDark(mContext))
            colorBackground = IconPalette.getMutedColor(mBitmapInfo.color, 0.5f);
        else {
            colorBackground = IconPalette.getMutedColor(mBitmapInfo.color, 0.1f);
            colorBackground = ColorUtil.manipulateColor(colorBackground, 0.5f);
        }
        return colorBackground;
    }

    /**
     * Adds the {@param badge} on top of {@param target} using the badge dimensions.
     */
    public void badgeWithDrawable(Bitmap target, Drawable badge) {
        mCanvas.setBitmap(target);
        badgeWithDrawable(mCanvas, badge);
        mCanvas.setBitmap(null);
    }

    /**
     * Adds the {@param badge} on top of {@param target} using the badge dimensions.
     */
    private void badgeWithDrawable(Canvas target, Drawable badge) {
        int badgeSize = mContext.getResources().getDimensionPixelSize(R.dimen.profile_badge_size);
        badge.setBounds(mIconBitmapSize - badgeSize, mIconBitmapSize - badgeSize,
                mIconBitmapSize, mIconBitmapSize);
        badge.draw(target);
    }

    /**
     * @param scale the scale to apply before drawing {@param icon} on the canvas
     */
    private Bitmap createIconBitmap(Drawable icon, float scale) {
        int width = mIconBitmapSize;
        int height = mIconBitmapSize;

        if (icon instanceof PaintDrawable) {
            PaintDrawable painter = (PaintDrawable) icon;
            painter.setIntrinsicWidth(width);
            painter.setIntrinsicHeight(height);
        } else if (icon instanceof BitmapDrawable) {
            // Ensure the bitmap has a density.
            BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            if (bitmap != null && bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                bitmapDrawable.setTargetDensity(mContext.getResources().getDisplayMetrics());
            }
        }

        int sourceWidth = icon.getIntrinsicWidth();
        int sourceHeight = icon.getIntrinsicHeight();
        if (sourceWidth > 0 && sourceHeight > 0) {
            // Scale the icon proportionally to the icon dimensions
            final float ratio = (float) sourceWidth / sourceHeight;
            if (sourceWidth > sourceHeight) {
                height = (int) (width / ratio);
            } else if (sourceHeight > sourceWidth) {
                width = (int) (height * ratio);
            }
        }
        // no intrinsic size --> use default size
        int textureWidth = mIconBitmapSize;
        int textureHeight = mIconBitmapSize;

        Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight,
                Bitmap.Config.ARGB_8888);
        mCanvas.setBitmap(bitmap);

        final int left = (textureWidth - width) / 2;
        final int top = (textureHeight - height) / 2;

        mOldBounds.set(icon.getBounds());
        if (Utilities.ATLEAST_OREO && icon instanceof AdaptiveIconDrawable) {
            int offset = Math.max((int) Math.ceil(BLUR_FACTOR * textureWidth), Math.max(left, top));
            int size = Math.max(width, height);
            icon.setBounds(offset, offset, size - offset, size - offset);
        } else {
            icon.setBounds(left, top, left + width, top + height);
        }
        mCanvas.save();
        mCanvas.scale(scale, scale, textureWidth / 2, textureHeight / 2);
        icon.draw(mCanvas);
        mCanvas.restore();
        icon.setBounds(mOldBounds);
        mCanvas.setBitmap(null);

        return bitmap;
    }

    public BitmapInfo createShortcutIcon(ShortcutInfoCompat shortcutInfo) {
        return createShortcutIcon(shortcutInfo, true /* badged */);
    }

    public BitmapInfo createShortcutIcon(ShortcutInfoCompat shortcutInfo, boolean badged) {
        return createShortcutIcon(shortcutInfo, badged, null);
    }

    public BitmapInfo createShortcutIcon(ShortcutInfoCompat shortcutInfo,
                                         boolean badged, @Nullable Provider<Bitmap> fallbackIconProvider) {
        Drawable unbadgedDrawable = DeepShortcutManager.getInstance(mContext)
                .getShortcutIconDrawable(shortcutInfo, mFillResIconDpi);
        IconCache cache = LauncherAppState.getInstance(mContext).getIconCache();

        final Bitmap unbadgedBitmap;
        if (unbadgedDrawable != null) {
            unbadgedBitmap = createScaledBitmapWithoutShadow(unbadgedDrawable, 0);
        } else {
            if (fallbackIconProvider != null) {
                // Fallback icons are already badged and with appropriate shadow
                Bitmap fullIcon = fallbackIconProvider.get();
                if (fullIcon != null) {
                    return createIconBitmap(fullIcon);
                }
            }
            unbadgedBitmap = cache.getDefaultIcon(Process.myUserHandle()).icon;
        }

        BitmapInfo result = new BitmapInfo();
        if (!badged) {
            result.color = Themes.getColorAccent(mContext);
            result.icon = unbadgedBitmap;
            return result;
        }

        final Bitmap unbadgedfinal = unbadgedBitmap;
        final ItemInfoWithIcon badge = getShortcutInfoBadge(shortcutInfo, cache);

        result.color = badge.iconColor;
        result.icon = BitmapRenderer.createHardwareBitmap(mIconBitmapSize, mIconBitmapSize, (c) -> {
            getShadowGenerator().recreateIcon(unbadgedfinal, c);
            badgeWithDrawable(c, new FastBitmapDrawable(badge));
        });
        return result;
    }

    public ItemInfoWithIcon getShortcutInfoBadge(ShortcutInfoCompat shortcutInfo, IconCache cache) {
        ComponentName cn = shortcutInfo.getActivity();
        String badgePkg = shortcutInfo.getBadgePackage(mContext);
        boolean hasBadgePkgSet = !badgePkg.equals(shortcutInfo.getPackage());
        if (cn != null && !hasBadgePkgSet) {
            // Get the app info for the source activity.
            AppInfo appInfo = new AppInfo();
            appInfo.user = shortcutInfo.getUserHandle();
            appInfo.componentName = cn;
            appInfo.intent = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(cn);
            cache.getTitleAndIcon(appInfo, false);
            return appInfo;
        } else {
            PackageItemInfo pkgInfo = new PackageItemInfo(badgePkg);
            cache.getTitleAndIconForApp(pkgInfo, false);
            return pkgInfo;
        }
    }

    /**
     * An extension of {@link BitmapDrawable} which returns the bitmap pixel size as intrinsic size.
     * This allows the badging to be done based on the action bitmap size rather than
     * the scaled bitmap size.
     */
    private static class FixedSizeBitmapDrawable extends BitmapDrawable {

        public FixedSizeBitmapDrawable(Bitmap bitmap) {
            super(null, bitmap);
        }

        @Override
        public int getIntrinsicHeight() {
            return getBitmap().getWidth();
        }

        @Override
        public int getIntrinsicWidth() {
            return getBitmap().getWidth();
        }
    }
}
