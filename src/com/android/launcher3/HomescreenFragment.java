package com.android.launcher3;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Adapter;
import android.widget.ListView;
import android.widget.NumberPicker;

import com.android.launcher3.notification.NotificationListener;
import com.android.launcher3.util.ListViewHighlighter;
import com.android.launcher3.util.SettingsObserver;
import com.android.launcher3.views.ButtonPreference;

import java.util.Objects;

import static com.android.launcher3.SettingsActivity.DELAY_HIGHLIGHT_DURATION_MILLIS;
import static com.android.launcher3.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY;
import static com.android.launcher3.SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGS;
import static com.android.launcher3.SettingsActivity.ICON_BADGING_PREFERENCE_KEY;
import static com.android.launcher3.SettingsActivity.KEY_GRID_SIZE;
import static com.android.launcher3.SettingsActivity.KEY_SHOW_DESKTOP_LABELS;
import static com.android.launcher3.SettingsActivity.KEY_SHOW_DRAWER_LABELS;
import static com.android.launcher3.SettingsActivity.NOTIFICATION_BADGING;
import static com.android.launcher3.SettingsActivity.NOTIFICATION_ENABLED_LISTENERS;
import static com.android.launcher3.SettingsActivity.SAVE_HIGHLIGHTED_KEY;
import static com.android.launcher3.Utilities.PREF_NOTIFICATIONS_GESTURE;

public class HomescreenFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private IconBadgingObserver mIconBadgingObserver;

    private String mPreferenceKey;
    private boolean mPreferenceHighlighted = false;
    private SharedPreferences mPrefs;
    private Preference mGridPref;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mPreferenceHighlighted = savedInstanceState.getBoolean(SAVE_HIGHLIGHTED_KEY);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
        addPreferencesFromResource(R.xml.homescreen_preferences);
        view.setBackgroundColor(Utilities.getBackgroundColor(getActivity()));
        mPrefs = Utilities.getPrefs(getActivity().getApplicationContext());
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        ContentResolver resolver = getActivity().getContentResolver();

        ButtonPreference iconBadgingPref =
                (ButtonPreference) findPreference(ICON_BADGING_PREFERENCE_KEY);
        if (!Utilities.ATLEAST_OREO) {
            getPreferenceScreen().removePreference(
                    findPreference(SessionCommitReceiver.ADD_ICON_PREFERENCE_KEY));
            getPreferenceScreen().removePreference(iconBadgingPref);
        } else if (!getResources().getBoolean(R.bool.notification_badging_enabled)) {
            getPreferenceScreen().removePreference(iconBadgingPref);
        } else {
            // Listen to system notification badge settings while this UI is active.
            mIconBadgingObserver = new IconBadgingObserver(
                    iconBadgingPref, resolver, getFragmentManager());
            mIconBadgingObserver.register(NOTIFICATION_BADGING, NOTIFICATION_ENABLED_LISTENERS);
        }

        mGridPref = findPreference(KEY_GRID_SIZE);
        if (mGridPref != null) {
            mGridPref.setOnPreferenceClickListener(preference -> {
                setCustomGridSize();
                return true;
            });

            mGridPref.setSummary(mPrefs.getString(KEY_GRID_SIZE, getDefaultGridSize()));
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVE_HIGHLIGHTED_KEY, mPreferenceHighlighted);
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = getActivity().getIntent();
        mPreferenceKey = intent.getStringExtra(EXTRA_FRAGMENT_ARG_KEY);
        if (isAdded() && !mPreferenceHighlighted && !TextUtils.isEmpty(mPreferenceKey)) {
            getView().postDelayed(this::highlightPreference, DELAY_HIGHLIGHT_DURATION_MILLIS);
        }
    }

    private void highlightPreference() {
        Preference pref = findPreference(mPreferenceKey);
        if (pref == null || getPreferenceScreen() == null) {
            return;
        }
        PreferenceScreen screen = getPreferenceScreen();
        if (Utilities.ATLEAST_OREO) {
            screen = selectPreferenceRecursive(pref, screen);
        }
        if (screen == null) {
            return;
        }

        View root = screen.getDialog() != null
                ? screen.getDialog().getWindow().getDecorView() : getView();
        ListView list = root.findViewById(android.R.id.list);
        if (list == null || list.getAdapter() == null) {
            return;
        }
        Adapter adapter = list.getAdapter();

        // Find the position
        int position = -1;
        for (int i = adapter.getCount() - 1; i >= 0; i--) {
            if (pref == adapter.getItem(i)) {
                position = i;
                break;
            }
        }
        new ListViewHighlighter(list, position);
        mPreferenceHighlighted = true;
    }

    @Override
    public void onDestroy() {
        if (mIconBadgingObserver != null) {
            mIconBadgingObserver.unregister();
            mIconBadgingObserver = null;
        }
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        switch (key) {
            case KEY_GRID_SIZE:
                mGridPref.setSummary(mPrefs.getString(KEY_GRID_SIZE, getDefaultGridSize()));
                SettingsActivity.mShouldRestart = true;
                break;
            case KEY_SHOW_DESKTOP_LABELS:
            case KEY_SHOW_DRAWER_LABELS:
            case PREF_NOTIFICATIONS_GESTURE:
                SettingsActivity.mShouldRestart = true;
                break;
        }
    }

    private void setCustomGridSize() {
        int minValue = 3;
        int maxValue = 9;

        String storedValue = mPrefs.getString(KEY_GRID_SIZE, "4x4");
        Pair<Integer, Integer> currentValues = Utilities.extractCustomGrid(storedValue);

        LayoutInflater inflater = (LayoutInflater)
                getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            return;
        }
        View contentView = inflater.inflate(R.layout.dialog_custom_grid, null);
        NumberPicker columnPicker = contentView.findViewById(R.id.dialog_grid_column);
        NumberPicker rowPicker = contentView.findViewById(R.id.dialog_grid_row);

        columnPicker.setMinValue(minValue);
        rowPicker.setMinValue(minValue);
        columnPicker.setMaxValue(maxValue);
        rowPicker.setMaxValue(maxValue);
        columnPicker.setValue(currentValues.first);
        rowPicker.setValue(currentValues.second);

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.grid_size_text)
                .setMessage(R.string.grid_size_custom_message)
                .setView(contentView)
                .setPositiveButton(R.string.grid_size_custom_positive, (dialog, i) -> {
                    String newValues = Utilities.getGridValue(columnPicker.getValue(),
                            rowPicker.getValue());
                    mPrefs.edit().putString(KEY_GRID_SIZE, newValues).apply();
                })
                .show();
    }

    private String getDefaultGridSize() {
        InvariantDeviceProfile profile = new InvariantDeviceProfile(getActivity());
        return Utilities.getGridValue(profile.numColumns, profile.numRows);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private PreferenceScreen selectPreferenceRecursive(
            Preference pref, PreferenceScreen topParent) {
        if (!(pref.getParent() instanceof PreferenceScreen)) {
            return null;
        }

        PreferenceScreen parent = (PreferenceScreen) pref.getParent();
        if (Objects.equals(parent.getKey(), topParent.getKey())) {
            return parent;
        } else if (selectPreferenceRecursive(parent, topParent) != null) {
            ((PreferenceScreen) parent.getParent())
                    .onItemClick(null, null, parent.getOrder(), 0);
            return parent;
        } else {
            return null;
        }
    }

    public static class IconBadgingObserver extends SettingsObserver.Secure
            implements Preference.OnPreferenceClickListener {

        private final ButtonPreference mBadgingPref;
        private final ContentResolver mResolver;
        private final FragmentManager mFragmentManager;

        public IconBadgingObserver(ButtonPreference badgingPref, ContentResolver resolver,
                                   FragmentManager fragmentManager) {
            super(resolver);
            mBadgingPref = badgingPref;
            mResolver = resolver;
            mFragmentManager = fragmentManager;
        }

        @Override
        public void onSettingChanged(boolean enabled) {
            int summary = enabled ? R.string.icon_badging_desc_on : R.string.icon_badging_desc_off;

            boolean serviceEnabled = true;
            if (enabled) {
                // Check if the listener is enabled or not.
                String enabledListeners =
                        Settings.Secure.getString(mResolver, NOTIFICATION_ENABLED_LISTENERS);
                ComponentName myListener =
                        new ComponentName(mBadgingPref.getContext(), NotificationListener.class);
                serviceEnabled = enabledListeners != null &&
                        (enabledListeners.contains(myListener.flattenToString()) ||
                                enabledListeners.contains(myListener.flattenToShortString()));
                if (!serviceEnabled) {
                    summary = R.string.title_missing_notification_access;
                }
            }
            mBadgingPref.setWidgetFrameVisible(!serviceEnabled);
            mBadgingPref.setOnPreferenceClickListener(serviceEnabled ? null : this);
            mBadgingPref.setSummary(summary);

        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            new NotificationAccessConfirmation().show(mFragmentManager, "notification_access");
            return true;
        }
    }

    public static class NotificationAccessConfirmation
            extends DialogFragment implements DialogInterface.OnClickListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            String msg = context.getString(R.string.msg_missing_notification_access,
                    context.getString(R.string.derived_app_name));
            return new AlertDialog.Builder(context)
                    .setTitle(R.string.title_missing_notification_access)
                    .setMessage(msg)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.title_change_settings, this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            ComponentName cn = new ComponentName(getActivity(), NotificationListener.class);
            Bundle showFragmentArgs = new Bundle();
            showFragmentArgs.putString(EXTRA_FRAGMENT_ARG_KEY, cn.flattenToString());

            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(EXTRA_FRAGMENT_ARG_KEY, cn.flattenToString())
                    .putExtra(EXTRA_SHOW_FRAGMENT_ARGS, showFragmentArgs);
            getActivity().startActivity(intent);
        }
    }
}
