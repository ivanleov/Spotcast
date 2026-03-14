package com.example.spotcast.data.preferences

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

class LocaleManager(context: Context) {

    private val prefs = context.getSharedPreferences("spotcast_prefs", Context.MODE_PRIVATE)

    fun getLocale(): String = prefs.getString(KEY_LOCALE, "en") ?: "en"

    fun setLocale(languageCode: String) {
        prefs.edit().putString(KEY_LOCALE, languageCode).apply()
    }

    fun applyLocale(context: Context): Context {
        val locale = Locale(getLocale())
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    companion object {
        private const val KEY_LOCALE = "app_locale"
    }
}
