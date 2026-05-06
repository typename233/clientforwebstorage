package com.example.clientforwebstorage.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LanguageUtil {

    private const val PREF_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "language_code"
    private const val LANG_CHINESE = "zh"
    private const val LANG_ENGLISH = "en"

    fun setLanguage(context: Context, languageCode: String) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(KEY_LANGUAGE, languageCode).apply()
        applyLanguage(context, languageCode)
    }

    fun getLanguageCode(context: Context): String {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_LANGUAGE, null) ?: getDefaultLanguageCode(context)
    }

    private fun getDefaultLanguageCode(context: Context): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        return locale.language
    }

    fun applyLanguage(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
    }
}
