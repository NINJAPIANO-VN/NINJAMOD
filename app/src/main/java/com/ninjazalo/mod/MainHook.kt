package com.ninjazalo.mod

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != Keys.TARGET_PKG) return
        XposedBridge.log("[NinjaZaloModded] loaded into ${lpparam.processName}")

        runCatching { NetBlocker.install(lpparam.classLoader) }
            .onFailure { XposedBridge.log("[NinjaZaloModded] NetBlocker failed: $it") }

        runCatching { HookEngine.install(lpparam.classLoader) }
            .onFailure { XposedBridge.log("[NinjaZaloModded] HookEngine failed: $it") }

        if (HookConfig.bool(Keys.SCAN_NOW)) {
            Scanner.runAsync(lpparam.classLoader)
        }
    }
}
