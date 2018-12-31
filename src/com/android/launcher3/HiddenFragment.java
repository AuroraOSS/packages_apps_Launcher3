/*
 * Copyright (C) 2017 Paranoid Android
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

import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;
import java.util.Set;


public class HiddenFragment extends PreferenceFragment implements MultiSelectRecyclerViewAdapter.ItemClickListener {

    boolean itemClicked = true;
    private ActionBar mActionBar;
    private MultiSelectRecyclerViewAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.hide_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (!itemClicked) {
            menu.findItem(R.id.reset).setVisible(false);
        } else {
            menu.findItem(R.id.reset).setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.reset) {
            unhideHiddenApps();
            itemClicked = false;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateHiddenApps() {
        mAdapter.addSelectionsToHideList(getActivity().getApplicationContext());
        LauncherAppState appState = LauncherAppState.getInstanceNoCreate();
        if (appState != null) {
            appState.getModel().forceReload();
        }
    }

    private void unhideHiddenApps() {
        mAdapter.removeSelectionsToHideList(getActivity().getApplicationContext());
        mAdapter.notifyDataSetChanged();
        mActionBar.setTitle(getString(R.string.hidden_app));
        LauncherAppState appState = LauncherAppState.getInstanceNoCreate();
        if (appState != null) {
            appState.getModel().forceReload();
        }
        Toast.makeText(getActivity().getApplicationContext(), getString(R.string.reset_hidden_apps_done),
                Toast.LENGTH_LONG).show();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        return inflater.inflate(R.layout.activity_multiselect, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mActionBar = getActivity().getActionBar();

        Set<String> hiddenApps = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getStringSet(Utilities.KEY_HIDDEN_APPS_SET, null);
        if (hiddenApps != null) {
            if (!hiddenApps.isEmpty()) {
                mActionBar.setTitle(String.valueOf(hiddenApps.size()) + getString(R.string.hide_app_selected));
                itemClicked = true;
            } else {
                mActionBar.setTitle(getString(R.string.hidden_app));
                itemClicked = false;
            }
        }

        List<ResolveInfo> mInstalledPackages = getInstalledApps();
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext(), RecyclerView.VERTICAL, false));
        DividerItemDecoration itemDecorator = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        itemDecorator.setDrawable(getResources().getDrawable(R.drawable.list_divider));
        recyclerView.addItemDecoration(itemDecorator);
        mAdapter = new MultiSelectRecyclerViewAdapter(getActivity().getApplicationContext(), mInstalledPackages, this);
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onItemClicked(int position) {
        mAdapter.toggleSelection(mActionBar, position);
        updateHiddenApps();
    }

    @Override
    public void onDestroy() {
        if (mActionBar != null)
            mActionBar.setTitle(getString(R.string.settings_button_text));
        super.onDestroy();
    }

    private List<ResolveInfo> getInstalledApps() {
        //get a list of installed apps.
        PackageManager packageManager = getActivity().getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> installedApps = packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA);
        Collections.sort(installedApps, new ResolveInfo.DisplayNameComparator(packageManager));
        return installedApps;
    }
}
