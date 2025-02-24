package com.example.newsreporter

import android.content.Context
import android.content.SharedPreferences

object UserSessionManager {
    private const val PREFS_NAME = "user_session"
    private const val KEY_USER_ID = "user_id"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveUserId(userId: Long) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): Long {
        return prefs.getLong(KEY_USER_ID, -1)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
