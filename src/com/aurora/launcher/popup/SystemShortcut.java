package com.aurora.launcher.popup;

import android.content.Intent;
import android.view.View;

import com.aurora.launcher.AbstractFloatingView;
import com.aurora.launcher.BaseDraggingActivity;
import com.aurora.launcher.EditShortcut;
import com.aurora.launcher.InfoBottomSheet;
import com.aurora.launcher.ItemInfo;
import com.aurora.launcher.Launcher;
import com.aurora.launcher.R;
import com.aurora.launcher.ShortcutInfo;
import com.aurora.launcher.model.WidgetItem;
import com.aurora.launcher.util.InstantAppResolver;
import com.aurora.launcher.util.PackageManagerHelper;
import com.aurora.launcher.util.PackageUserKey;
import com.aurora.launcher.widget.WidgetsBottomSheet;

import java.util.List;

import static com.aurora.launcher.userevent.nano.LauncherLogProto.Action;
import static com.aurora.launcher.userevent.nano.LauncherLogProto.ControlType;

/**
 * Represents a system shortcut for a given app. The shortcut should have a static label and
 * icon, and an onClickListener that depends on the item that the shortcut services.
 * <p>
 * Example system shortcuts, defined as inner classes, include Widgets and AppInfo.
 */
public abstract class SystemShortcut<T extends BaseDraggingActivity> extends ItemInfo {
    public final int iconResId;
    public final int labelResId;

    public SystemShortcut(int iconResId, int labelResId) {
        this.iconResId = iconResId;
        this.labelResId = labelResId;
    }

    public abstract View.OnClickListener getOnClickListener(T activity, ItemInfo itemInfo);

    public static class Edit extends EditShortcut {
    }

    public static class Widgets extends SystemShortcut<Launcher> {

        public Widgets() {
            super(R.drawable.ic_widget, R.string.widget_button_text);
        }

        @Override
        public View.OnClickListener getOnClickListener(final Launcher launcher,
                                                       final ItemInfo itemInfo) {
            final List<WidgetItem> widgets =
                    launcher.getPopupDataProvider().getWidgetsForPackageUser(new PackageUserKey(
                            itemInfo.getTargetComponent().getPackageName(), itemInfo.user));
            if (widgets == null) {
                return null;
            }
            return (view) -> {
                AbstractFloatingView.closeAllOpenViews(launcher);
                WidgetsBottomSheet widgetsBottomSheet =
                        (WidgetsBottomSheet) launcher.getLayoutInflater().inflate(
                                R.layout.widgets_bottom_sheet, launcher.getDragLayer(), false);
                widgetsBottomSheet.populateAndShow(itemInfo);
                launcher.getUserEventDispatcher().logActionOnControl(Action.Touch.TAP,
                        ControlType.WIDGETS_BUTTON, view);
            };
        }
    }

    public static class AppInfo extends SystemShortcut {
        public AppInfo() {
            super(R.drawable.ic_info_no_shadow, R.string.app_info_drop_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(
                BaseDraggingActivity activity, ItemInfo itemInfo) {
            return (view) -> {
                AbstractFloatingView.closeAllOpenViews(activity);
                InfoBottomSheet infoBottomSheet = (InfoBottomSheet) activity.getLayoutInflater()
                        .inflate(R.layout.info_layout, activity.getDragLayer(), false);
                infoBottomSheet.populateAndShow(itemInfo);
            };
        }
    }

    public static class Install extends SystemShortcut {
        public Install() {
            super(R.drawable.ic_install_no_shadow, R.string.install_drop_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(
                BaseDraggingActivity activity, ItemInfo itemInfo) {
            boolean supportsWebUI = (itemInfo instanceof ShortcutInfo) &&
                    ((ShortcutInfo) itemInfo).hasStatusFlag(ShortcutInfo.FLAG_SUPPORTS_WEB_UI);
            boolean isInstantApp = false;
            if (itemInfo instanceof com.aurora.launcher.AppInfo) {
                com.aurora.launcher.AppInfo appInfo = (com.aurora.launcher.AppInfo) itemInfo;
                isInstantApp = InstantAppResolver.newInstance(activity).isInstantApp(appInfo);
            }
            boolean enabled = supportsWebUI || isInstantApp;
            if (!enabled) {
                return null;
            }
            return createOnClickListener(activity, itemInfo);
        }

        public View.OnClickListener createOnClickListener(
                BaseDraggingActivity activity, ItemInfo itemInfo) {
            return view -> {
                Intent intent = new PackageManagerHelper(view.getContext()).getMarketIntent(
                        itemInfo.getTargetComponent().getPackageName());
                activity.startActivitySafely(view, intent, itemInfo);
                AbstractFloatingView.closeAllOpenViews(activity);
            };
        }
    }
}
