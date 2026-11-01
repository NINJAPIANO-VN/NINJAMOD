package com.ninjazalo.mod

import android.net.Uri
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.io.IOException
import java.net.URL

object NetBlocker {
    private val ADS = listOf("/group/ads", "/zpads/")
    private val QOS = listOf("/api/qos/", "/zalocloudqos")
    private val TRACKING_PATH = listOf("/tracking")
    private val FIREBASE_HOSTS = listOf("firebaselogging", "app-measurement.com", "crashlytics.com")

    private fun containsAny(s: String, needles: List<String>) = needles.any { s.contains(it, ignoreCase = true) }

    fun shouldBlock(url: String): Boolean {
        if (url.isEmpty()) return false
        val uri = Uri.parse(url)
        val host = uri.host ?: ""
        val path = uri.path ?: ""
        if (HookConfig.bool(Keys.BLOCK_ADS) && containsAny(url, ADS)) return true
        if (HookConfig.bool(Keys.BLOCK_QOS) && containsAny(url, QOS)) return true
        if (HookConfig.bool(Keys.DISABLE_TRACKING) && containsAny(path, TRACKING_PATH)) return true
        if (HookConfig.bool(Keys.DISABLE_FIREBASE) && containsAny(host, FIREBASE_HOSTS)) return true
        val extra = HookConfig.str(Keys.EXTRA_PATTERNS).lines().map { it.trim() }.filter { it.isNotEmpty() }
        return extra.isNotEmpty() && containsAny(url, extra)
    }

    private fun urlOf(target: Any?): String? = when (target) {
        null -> null
        is URL -> target.toString()
        else -> try {
            target.javaClass.getMethod("url").invoke(target)?.toString()
        } catch (_: Throwable) {
            null
        }
    }

    private val netHook = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            try {
                val url = if (param.thisObject is URL) param.thisObject.toString() else urlOf(param.args.getOrNull(0))
                if (url == null) return
                if (HookConfig.bool(Keys.LOG_REQUESTS)) XposedBridge.log("[NinjaZaloModded][net] $url")
                if (shouldBlock(url)) {
                    XposedBridge.log("[NinjaZaloModded] blocked $url")
                    param.throwable = IOException("Blocked by NinjaZalo")
                }
            } catch (_: Throwable) {
            }
        }
    }

    private val firebaseHook = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            if (HookConfig.bool(Keys.DISABLE_FIREBASE)) param.result = null
        }
    }

    fun install(cl: ClassLoader) {
        XposedBridge.hookAllMethods(URL::class.java, "openConnection", netHook)

        XposedHelpers.findClassIfExists("okhttp3.OkHttpClient", cl)?.let {
            XposedBridge.hookAllMethods(it, "newCall", netHook)
            XposedBridge.log("[NinjaZaloModded] hooked OkHttp")
        }

        XposedHelpers.findClassIfExists("com.google.firebase.analytics.FirebaseAnalytics", cl)?.let {
            XposedBridge.hookAllMethods(it, "logEvent", firebaseHook)
            XposedBridge.log("[NinjaZaloModded] hooked FirebaseAnalytics.logEvent")
        }
    }
}
