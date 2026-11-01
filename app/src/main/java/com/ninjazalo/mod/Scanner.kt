package com.ninjazalo.mod

import android.app.AndroidAppHelper
import dalvik.system.DexFile
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.io.File

/**
 * Quét class/method có tên chứa recall/undo/seen/typing/ttl... để tìm target hook (giống ZXScanner bên iOS).
 * Zalo bị obfuscate nên nhiều method sẽ không có tên dễ đọc; khi đó dùng jadx/DexKit trên APK Zalo.
 * Kết quả ghi vào Android/data/com.zing.zalo/files/ninjazalo-scan.txt
 */
object Scanner {
    private val NAME_RE = Regex("recall|undo|revoke|seen|typing|ttl|delete|receipt", RegexOption.IGNORE_CASE)
    private val PKG_PREFIXES = arrayOf("com.zing", "com.zalo", "vn.com.vng", "com.vng")
    private const val MAX_CLASSES = 40000

    fun runAsync(cl: ClassLoader) {
        Thread {
            try {
                var app = AndroidAppHelper.currentApplication()
                var waited = 0
                while (app == null && waited < 20) {
                    Thread.sleep(500); waited++
                    app = AndroidAppHelper.currentApplication()
                }
                val dir = app?.getExternalFilesDir(null) ?: app?.filesDir ?: return@Thread
                val out = File(dir, "ninjazalo-scan.txt")
                val sb = StringBuilder()
                var count = 0

                val pathList = XposedHelpers.getObjectField(cl, "pathList")
                @Suppress("UNCHECKED_CAST")
                val elements = XposedHelpers.getObjectField(pathList, "dexElements") as Array<Any>
                for (e in elements) {
                    val df = XposedHelpers.getObjectField(e, "dexFile") as? DexFile ?: continue
                    val names = df.entries()
                    while (names.hasMoreElements() && count < MAX_CLASSES) {
                        val name = names.nextElement()
                        if (PKG_PREFIXES.none { name.startsWith(it) }) continue
                        count++
                        try {
                            val c = Class.forName(name, false, cl)
                            val classHit = NAME_RE.containsMatchIn(name)
                            for (m in c.declaredMethods) {
                                if (classHit || NAME_RE.containsMatchIn(m.name)) {
                                    sb.append(name).append('#').append(m.name).append('(')
                                        .append(m.parameterTypes.joinToString(",") { it.simpleName })
                                        .append("):").append(m.returnType.simpleName).append('\n')
                                }
                            }
                        } catch (_: Throwable) {
                        }
                    }
                }
                out.writeText(sb.toString())
                XposedBridge.log("[NinjaZaloModded] scan xong ($count class) -> ${out.absolutePath}")
            } catch (t: Throwable) {
                XposedBridge.log("[NinjaZaloModded] scan lỗi: $t")
            }
        }.start()
    }
}
