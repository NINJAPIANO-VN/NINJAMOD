package com.ninjazalo.mod

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private val savers = ArrayList<() -> Unit>()
    private var pad = 0

    @Suppress("DEPRECATION")
    private fun openPrefs(): SharedPreferences = try {
        getSharedPreferences(Keys.PREFS, Context.MODE_WORLD_READABLE)
    } catch (_: SecurityException) {
        Toast.makeText(this, "Hãy bật module trong LSPosed trước.", Toast.LENGTH_LONG).show()
        getSharedPreferences(Keys.PREFS, Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pad = (16 * resources.displayMetrics.density).toInt()
        prefs = openPrefs()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "NinjaZalo"
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Bật module trong LSPosed, chọn phạm vi Zalo (com.zing.zalo). Sau khi lưu hãy đóng hẳn Zalo rồi mở lại."
            setPadding(0, pad / 2, 0, pad)
        })

        root.addView(section("Chặn request"))
        root.addView(toggle("Chặn endpoint quảng cáo (/group/ads, /zpads/)", Keys.BLOCK_ADS))
        root.addView(toggle("Chặn Zalo QoS (/api/qos/, /zalocloudqos)", Keys.BLOCK_QOS))
        root.addView(toggle("Chặn Zalo tracking (/tracking)", Keys.DISABLE_TRACKING))
        root.addView(toggle("Chặn Firebase / Crashlytics", Keys.DISABLE_FIREBASE))

        root.addView(section("Chống thu hồi / xóa (cần rule antirecall)"))
        root.addView(toggle("Chống thu hồi tin nhắn", Keys.ANTI_RECALL))
        root.addView(toggle("Áp dụng cả tin do mình thu hồi", Keys.ANTI_RECALL_INCLUDE_ME))
        root.addView(toggle("Chống xóa tin nhắn", Keys.ANTI_DELETE))
        root.addView(toggle("Áp dụng cả tin do mình xóa", Keys.ANTI_DELETE_INCLUDE_ME))
        root.addView(toggle("Hiện nội dung tin đã thu hồi/xóa", Keys.SHOW_CONTENT))
        root.addView(field("User ID của bạn", Keys.MY_UID, "", false, true))
        root.addView(TextView(this).apply {
            text = "Lịch sử thu hồi/xóa: Android/data/com.zing.zalo/files/ninjazalo/recalled.txt"
            textSize = 12f
        })

        root.addView(section("Tin nhắn khác (cần rule tương ứng)"))
        root.addView(toggle("Chặn gửi \"Đã xem\"", Keys.BLOCK_SEEN))
        root.addView(toggle("Không gửi \"Đang nhập\"", Keys.SILENT_TYPING))
        root.addView(toggle("Ghi đè TTL tin nhắn", Keys.TTL_ENABLE))
        root.addView(field("Giá trị TTL", Keys.TTL_VALUE, "0", false, true))

        root.addView(section("Nâng cao"))
        root.addView(field("Chặn thêm (mỗi dòng một đoạn URL)", Keys.EXTRA_PATTERNS, "", true, false))
        root.addView(field("rules.json (hook theo class/method của Zalo)", Keys.RULES_JSON, "", true, false))
        root.addView(toggle("Ghi log mọi request vào LSPosed log", Keys.LOG_REQUESTS))
        root.addView(toggle("Quét class/method khi mở Zalo (bật xong nhớ tắt)", Keys.SCAN_NOW))

        root.addView(Button(this).apply {
            text = "Lưu"
            setOnClickListener {
                savers.forEach { it() }
                Toast.makeText(this@SettingsActivity, "Đã lưu. Đóng hẳn Zalo rồi mở lại.", Toast.LENGTH_LONG).show()
            }
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun section(title: String) = TextView(this).apply {
        text = title
        textSize = 16f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, pad, 0, pad / 4)
    }

    private fun toggle(label: String, key: String) = Switch(this).apply {
        text = label
        isChecked = prefs.getBoolean(key, Keys.defaultBool(key))
        setPadding(0, pad / 4, 0, pad / 4)
        setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean(key, checked).apply() }
    }

    private fun field(label: String, key: String, def: String, multiline: Boolean, numeric: Boolean): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, pad / 4, 0, pad / 4)
        }
        box.addView(TextView(this).apply { text = label })
        val edit = EditText(this).apply {
            setText(prefs.getString(key, def))
            gravity = if (multiline) Gravity.TOP else Gravity.CENTER_VERTICAL
            if (multiline) {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                minLines = 4
                typeface = Typeface.MONOSPACE
                textSize = 12f
            } else {
                inputType = if (numeric) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
            }
        }
        box.addView(edit)
        savers.add { prefs.edit().putString(key, edit.text.toString()).apply() }
        return box
    }
}
