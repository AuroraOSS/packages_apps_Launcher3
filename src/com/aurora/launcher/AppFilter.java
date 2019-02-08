package com.aurora.launcher;

import android.content.Context;

public interface AppFilter {

    boolean shouldShowApp(String packageName, Context context);
}
