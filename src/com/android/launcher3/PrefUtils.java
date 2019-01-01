package com.android.launcher3;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class PrefUtils {

    public static void saveMap(String prefKey, Context context, Map<String, String> inputMap) {
        SharedPreferences mPrefs = Utilities.getPrefs(context);
        if (mPrefs != null) {
            JSONObject jsonObject = new JSONObject(inputMap);
            String jsonString = jsonObject.toString();
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.remove(prefKey).apply();
            editor.putString(prefKey, jsonString);
            editor.apply();
        }
    }

    public static Map<String, String> getMap(String prefKey, Context context) {
        Map<String, String> outputMap = new HashMap<String, String>();
        SharedPreferences mPrefs = Utilities.getPrefs(context);
        try {
            if (mPrefs != null) {
                String jsonString = mPrefs.getString(prefKey, (new JSONObject()).toString());
                JSONObject jsonObject = new JSONObject(jsonString);
                Iterator<String> keysItr = jsonObject.keys();
                while (keysItr.hasNext()) {
                    String key = keysItr.next();
                    String value = (String) jsonObject.get(key);
                    outputMap.put(key, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outputMap;
    }
}
