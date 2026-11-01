package com.ninjazalo.mod

import de.robv.android.xposed.XSharedPreferences

object HookConfig {
    private var prefs: XSharedPreferences? = null
    private var lastReload = 0L

    private fun p(): XSharedPreferences {
        val existing = prefs
        val xp = existing ?: XSharedPreferences(Keys.MODULE_PKG, Keys.PREFS).also {
            it.makeWorldReadable()
            prefs = it
        }
        val now = System.currentTimeMillis()
        if (now - lastReload > 3000) {
            xp.reload()
            lastReload = now
        }
        return xp
    }

    fun bool(key: String): Boolean =
        try { p().getBoolean(key, Keys.defaultBool(key)) } catch (_: Throwable) { Keys.defaultBool(key) }

    fun str(key: String, def: String = ""): String =
        try { p().getString(key, def) ?: def } catch (_: Throwable) { def }

    fun long(key: String, def: Long = 0L): Long = str(key, def.toString()).trim().toLongOrNull() ?: def
}
