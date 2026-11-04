package com.ninjazalo.mod

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Method

object HookEngine {

    fun install(cl: ClassLoader) {

        val raw = HookConfig.str(Keys.RULES_JSON, "").lines()
            .filterNot { it.trim().startsWith("//") }
            .joinToString("\n").trim()
        if (raw.isEmpty()) {
            XposedBridge.log("[NINJAMOD] rules.json trống, bỏ qua HookEngine")
            return
        }
        val rules: JSONArray = if (raw.startsWith("[")) JSONArray(raw) else JSONObject(raw).optJSONArray("rules") ?: return
        for (i in 0 until rules.length()) {
            val r = rules.optJSONObject(i) ?: continue
            if (!r.optBoolean("enabled", true)) continue
            val cls = r.optString("class")
            val method = r.optString("method").ifEmpty { r.optString("selector") }
            val action = r.optString("action", "skip")
            val id = r.optString("id", "$cls#$method")
            try {
                val clazz = XposedHelpers.findClass(cls, cl)
                val hooks = XposedBridge.hookAllMethods(
                    clazz, method,
                    RuleHook(
                        action,
                        r.optInt("argIndex", 1),
                        r.optString("configKey").ifEmpty { null },
                        r.optLong("returnValue", 0L)
                    )
                )
                if (hooks.isEmpty()) XposedBridge.log("[NINJAMOD] [$id] không thấy method $cls#$method")
                else XposedBridge.log("[NINJAMOD] [$id] hook OK $cls#$method ($action) x${hooks.size}")
            } catch (t: Throwable) {
                XposedBridge.log("[NINJAMOD] [$id] hook FAIL $cls#$method: $t")
            }
        }
    }

    private fun defaultFor(type: Class<*>?): Any? = when (type) {
        java.lang.Boolean.TYPE -> false
        java.lang.Integer.TYPE -> 0
        java.lang.Long.TYPE -> 0L
        java.lang.Short.TYPE -> 0.toShort()
        java.lang.Byte.TYPE -> 0.toByte()
        java.lang.Character.TYPE -> 0.toChar()
        java.lang.Float.TYPE -> 0f
        java.lang.Double.TYPE -> 0.0
        else -> null
    }

    private fun numberFor(type: Class<*>?, v: Long): Any? = when (type) {
        java.lang.Integer.TYPE, java.lang.Integer::class.java -> v.toInt()
        java.lang.Long.TYPE, java.lang.Long::class.java -> v
        else -> null
    }

    private class RuleHook(
        private val action: String,
        private val argIndex: Int,
        private val configKey: String?,
        private val returnValue: Long
    ) : XC_MethodHook() {

        override fun beforeHookedMethod(param: MethodHookParam) {
            try {
                if (configKey != null && !HookConfig.bool(configKey)) return
                val rt = (param.method as? Method)?.returnType
                when (action) {
                    "skip" -> param.result = numberFor(rt, returnValue) ?: defaultFor(rt)
                    "antirecall" -> {
                        val idx = argIndex - 1
                        if (idx !in param.args.indices) return
                        AntiRecall.processObject(param.args[idx])?.let { param.args[idx] = it }
                    }
                    "log" -> XposedBridge.log(
                        "[NINJAMOD][log] ${param.method} args=" +
                            param.args.joinToString { it?.toString()?.take(300) ?: "null" }
                    )
                }
            } catch (t: Throwable) {
                XposedBridge.log("[NINJAMOD] rule error: $t")
            }
        }

        override fun afterHookedMethod(param: MethodHookParam) {
            if (action != "ttl") return
            try {
                if (!HookConfig.bool(configKey ?: Keys.TTL_ENABLE)) return
                val rt = (param.method as? Method)?.returnType
                numberFor(rt, HookConfig.long(Keys.TTL_VALUE, 0L))?.let { param.result = it }
            } catch (t: Throwable) {
                XposedBridge.log("[NINJAMOD] ttl error: $t")
            }
        }
    }
}
