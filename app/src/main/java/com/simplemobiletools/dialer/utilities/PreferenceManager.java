package com.simplemobiletools.dialer.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;


public class PreferenceManager {

    private SharedPreferences sharedPreferences;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.KEY_PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    public void putBoolean(String key, Boolean value){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();

    }

    public Boolean getBoolean (String key) {
        return sharedPreferences.getBoolean(key, false);
    }

    public Set<String> getStringSet(String key){
        Log.d("inytecancel", "sharedPrefernces");
        Log.d("inytecancel", key);
        return sharedPreferences.getStringSet(key, null);
    }

    public void putString(String key, String value){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public void putStringSet(String key, Set<String> value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(key, value);
        editor.apply();
    }

    public void removeStringKey(String key){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
        Log.d("inytecancel", key);
        Log.d("inytecancel", "removedKey");
    }

    public String getString (String key){
        return sharedPreferences.getString(key, "");
    }

    public void clearPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}
