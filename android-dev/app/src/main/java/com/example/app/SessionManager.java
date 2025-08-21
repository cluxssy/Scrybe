package com.example.app;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "ScrybeSession";
    private static final String KEY_AUTH_TOKEN = "auth_token";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveAuthToken(String token) {
        editor.putString(KEY_AUTH_TOKEN, token);
        editor.apply();
    }

    public String fetchAuthToken() {
        return prefs.getString(KEY_AUTH_TOKEN, null);
    }

    public void clearAuthToken() {
        editor.remove(KEY_AUTH_TOKEN);
        editor.apply();
    }
}