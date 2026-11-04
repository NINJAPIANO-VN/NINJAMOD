package com.ninjazalo.mod

import android.app.AndroidAppHelper
import de.robv.android.xposed.XposedBridge
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale

object AntiRecall {
    private const val SIGNATURE =
        "{\"properties\":{\"color\":0,\"size\":0,\"type\":0,\"subType\":0," +
            "\"ext\":\"{\\\"shouldParseLinkOrContact\\\":0}\"},\"msgBubbleLayoutType\":0," +
            "\"generatedBy\":\"AntiRecallDeleteHook\"}"

    private const val MAX_CACHE = 3000
    private val ID_KEYS = arrayOf("cliMsgId", "globalMsgId", "msgId", "realMsgId", "clientDelMsgId", "globalDelMsgId")

    private class Entry(val text: String, val from: String, val ts: String)

    private val cache = object : LinkedHashMap<String, Entry>(256, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?) = size > MAX_CACHE
    }
    private var loaded = false
    private var dirty = false
    private var dir: File? = null

    private fun dataDir(): File? {
        dir?.let { return it }
        val app = AndroidAppHelper.currentApplication() ?: return null
        val base = app.getExternalFilesDir(null) ?: app.filesDir
        return File(base, "ninjazalo").also { it.mkdirs(); dir = it }
    }

    fun historyFile(): File? = dataDir()?.let { File(it, "recalled.txt") }
    private fun cacheFile(): File? = dataDir()?.let { File(it, "msgcache.json") }

    private fun ensureLoaded() {
        if (loaded) return
        val f = cacheFile() ?: return
        loaded = true
        try {
            if (!f.exists()) return
            val j = JSONObject(f.readText())
            val map = j.optJSONObject("map") ?: return
            val order = j.optJSONArray("order") ?: return
            for (i in 0 until order.length()) {
                val id = order.optString(i)
                val e = map.optJSONObject(id) ?: continue
                cache[id] = Entry(e.optString("text"), e.optString("from"), e.optString("ts"))
            }
        } catch (_: Throwable) {
        }
    }

    private fun flushSoon() {
        if (dirty) return
        dirty = true
        Thread {
            try {
                Thread.sleep(5000)
                val snapshot = JSONObject()
                synchronized(cache) {
                    dirty = false
                    val map = JSONObject()
                    val order = JSONArray()
                    for ((k, v) in cache) {
                        order.put(k)
                        map.put(k, JSONObject().put("text", v.text).put("from", v.from).put("ts", v.ts))
                    }
                    snapshot.put("map", map).put("order", order)
                }
                cacheFile()?.writeText(snapshot.toString())
            } catch (_: Throwable) {
                dirty = false
            }
        }.apply { isDaemon = true }.start()
    }

    private fun opt(o: JSONObject, k: String): Any? = o.opt(k).takeIf { it != JSONObject.NULL }

    private fun idStr(v: Any?): String? = when {
        v is Number -> v.toLong().toString()
        v is String && v.isNotEmpty() -> v
        else -> null
    }

    private fun idsFrom(d: JSONObject): List<String> {
        val ids = ArrayList<String>()
        for (k in ID_KEYS) {
            val v = idStr(opt(d, k))
            if (v != null && v != "0" && !ids.contains(v)) ids.add(v)
        }
        return ids
    }

    private fun cacheMessage(data: JSONObject) {
        val text = opt(data, "msg") as? String ?: return
        if (text.isEmpty() || text.length > 4000) return
        val ids = idsFrom(data)
        if (ids.isEmpty()) return
        val entry = Entry(text, idStr(opt(data, "fromU") ?: opt(data, "uidFrom")) ?: "", idStr(opt(data, "ts")) ?: "")
        synchronized(cache) {
            ensureLoaded()
            for (i in ids) cache[i] = entry
        }
        flushSoon()
    }

    private fun cacheLookup(source: JSONObject): Entry? = synchronized(cache) {
        ensureLoaded()
        for (i in idsFrom(source)) cache[i]?.let { return it }
        null
    }

    private fun recordHistory(kind: String, from: String?, text: String?) {
        try {
            val f = historyFile() ?: return
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val line = "$ts | $kind | từ: ${if (from.isNullOrEmpty()) "?" else from} | " +
                (if (text.isNullOrEmpty()) "(không có nội dung trong bộ nhớ đệm)" else text) + "\n"
            FileWriter(f, true).use { it.write(line) }
        } catch (_: Throwable) {
        }
    }

    private fun notice(base: String, text: String?): String =
        if (!HookConfig.bool(Keys.SHOW_CONTENT) || text.isNullOrEmpty()) base else "$base: $text"

    private fun paramsExt() = JSONObject()
        .put("notifyActionCate", 0).put("containType", 0).put("forceNotify", 0).put("notifyTTL", 0)
        .put("countUnread", 1).put("platformType", 0).put("refactorType", 0)

    private fun isMe(uid: Any?): Boolean {
        val me = HookConfig.str(Keys.MY_UID).trim()
        if (me.isEmpty()) return false
        val n = when (uid) {
            is Number -> uid.toLong().toString()
            is String -> uid.toLongOrNull()?.toString() ?: return false
            else -> return false
        }
        return n == me
    }

    private fun fakeQuote(ownerId: Any?, cliMsgId: Any?, globalMsgId: Any?, ts: Any?, text: String) = JSONObject()
        .put("ownerId", ownerId ?: 0)
        .put("gOwnerId", JSONObject.NULL)
        .put("cliMsgId", cliMsgId ?: 0)
        .put("globalMsgId", globalMsgId ?: 0)
        .put("cliMsgType", 1)
        .put("ts", ts ?: 0)
        .put("msg", text)
        .put("attach", SIGNATURE)
        .put("fromD", "ZaloXposed by ElectroHeavenVN")
        .put("ttl", 0)

    private fun processMsgPayload(payload: JSONObject): Boolean {
        var modified = false
        val msgs = payload.optJSONArray("msg") ?: return false

        for (i in 0 until msgs.length()) {
            val entry = msgs.optJSONObject(i) ?: continue
            val msgObject = entry.optJSONObject("text") ?: continue
            val type = opt(msgObject, "type") as? String ?: continue
            val data = msgObject.optJSONObject("data") ?: continue

            if (type != "chat.undo" && type != "chat.delete") {
                cacheMessage(data)
                continue
            }

            if (type == "chat.undo" && HookConfig.bool(Keys.ANTI_RECALL)) {
                if (!HookConfig.bool(Keys.ANTI_RECALL_INCLUDE_ME) && isMe(opt(data, "fromU"))) continue
                val attach = data.optJSONObject("attach") ?: continue
                val old = cacheLookup(attach)
                recordHistory("THU HỒI", idStr(opt(data, "fromU")), old?.text)
                msgObject.put("type", "webchat")
                data.put("msg", notice("Tin nhắn đã thu hồi", old?.text))
                data.put("attach", "")
                data.put("quote", fakeQuote(opt(data, "fromU"), opt(attach, "cliMsgId"), opt(attach, "globalMsgId"),
                    opt(data, "ts"), "Jump to message"))
                data.put("paramsExt", paramsExt())
                modified = true
            } else if (type == "chat.delete" && HookConfig.bool(Keys.ANTI_DELETE)) {
                val contents = data.optJSONObject("attach")?.optJSONArray("contents") ?: continue
                if (contents.length() == 0) continue
                val content = contents.optJSONObject(contents.length() - 1) ?: continue
                if (!HookConfig.bool(Keys.ANTI_DELETE_INCLUDE_ME) && isMe(opt(data, "fromU"))) continue
                val many = contents.length() > 1
                val texts = ArrayList<String>()
                for (j in 0 until contents.length()) {
                    val c = contents.optJSONObject(j) ?: continue
                    cacheLookup(c)?.let { texts.add(it.text) }
                    if (texts.size >= 5) break
                }
                val joined = texts.joinToString(" | ")
                recordHistory("XÓA", idStr(opt(content, "uidFrom")), joined)
                msgObject.put("type", "webchat")
                data.put("msg", notice(if (many) "${contents.length()} tin nhắn đã xóa" else "Tin nhắn đã xóa", joined))
                data.put("attach", "")
                data.put("quote", fakeQuote(opt(content, "uidFrom"), opt(content, "clientDelMsgId"),
                    opt(content, "globalDelMsgId"), opt(content, "clientDelMsgId"),
                    if (many) "Jump to first deleted message" else "Jump to message"))
                data.put("paramsExt", paramsExt())
                modified = true
            }
        }
        return modified
    }

    private fun walk(node: Any?): Boolean {
        var modified = false
        when (node) {
            is JSONObject -> {
                if (node.opt("msg") is JSONArray) modified = processMsgPayload(node) || modified
                val keys = ArrayList<String>()
                val it = node.keys()
                while (it.hasNext()) keys.add(it.next())
                for (k in keys) {
                    val v = node.opt(k)
                    if (v is JSONObject || v is JSONArray) modified = walk(v) || modified
                }
            }
            is JSONArray -> for (i in 0 until node.length()) modified = walk(node.opt(i)) || modified
        }
        return modified
    }

    private val NEEDLES = arrayOf("chat.undo", "chat.delete", "webchat")

    private fun parse(text: String): Any? {
        val t = text.trim()
        return when {
            t.startsWith("{") -> JSONObject(t)
            t.startsWith("[") -> JSONArray(t)
            else -> null
        }
    }

    fun processObject(obj: Any?): Any? {
        if (obj == null) return null
        if (!HookConfig.bool(Keys.ANTI_RECALL) && !HookConfig.bool(Keys.ANTI_DELETE)) return null
        return try {
            when (obj) {
                is JSONObject, is JSONArray -> if (walk(obj)) obj else null
                is String -> {
                    if (NEEDLES.none { obj.contains(it) }) return null
                    val tree = parse(obj) ?: return null
                    if (walk(tree)) tree.toString() else null
                }
                is ByteArray -> {
                    if (obj.size > 4 * 1024 * 1024) return null
                    val s = String(obj, Charsets.UTF_8)
                    if (NEEDLES.none { s.contains(it) }) return null
                    val tree = parse(s) ?: return null
                    if (walk(tree)) tree.toString().toByteArray(Charsets.UTF_8) else null
                }
                else -> null
            }
        } catch (t: Throwable) {
            XposedBridge.log("[NINJAMOD] AntiRecall exception: $t")
            null
        }
    }
}
