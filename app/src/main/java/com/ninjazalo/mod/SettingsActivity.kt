package com.ninjazalo.mod

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SettingsActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private val savers = ArrayList<() -> Unit>()
    private var dp8 = 0
    private var dp16 = 0
    private var dp24 = 0

    @Suppress("DEPRECATION")
    private fun openPrefs(): SharedPreferences = try {
        getSharedPreferences(Keys.PREFS, Context.MODE_WORLD_READABLE)
    } catch (_: SecurityException) {
        Toast.makeText(this, "Hãy bật module trong LSPosed trước.", Toast.LENGTH_LONG).show()
        getSharedPreferences(Keys.PREFS, Context.MODE_PRIVATE)
    }

    private fun gold() = resources.getColor(R.color.nz_gold, theme)
    private fun bg() = resources.getColor(R.color.nz_bg, theme)
    private fun surface() = resources.getColor(R.color.nz_surface, theme)
    private fun textPrimary() = resources.getColor(R.color.nz_text_primary, theme)
    private fun textSecondary() = resources.getColor(R.color.nz_text_secondary, theme)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val density = resources.displayMetrics.density
        dp8 = (8 * density).toInt()
        dp16 = (16 * density).toInt()
        dp24 = (24 * density).toInt()
        prefs = openPrefs()
        window.setBackgroundDrawable(GradientDrawable().apply { setColor(bg()) })

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp16, dp24, dp16, dp24)
        }

        root.addView(header())

        root.addView(section("CHẶN REQUEST") {
            it.addView(toggle("Chặn endpoint quảng cáo", "/group/ads, /zpads/", Keys.BLOCK_ADS))
            it.addView(toggle("Chặn Zalo QoS", "/api/qos/, /zalocloudqos", Keys.BLOCK_QOS))
            it.addView(toggle("Chặn Zalo tracking", "/tracking", Keys.DISABLE_TRACKING))
            it.addView(toggle("Chặn Firebase / Crashlytics", null, Keys.DISABLE_FIREBASE))
        })

        root.addView(section("CHỐNG THU HỒI / XÓA") {
            it.addView(toggle("Chống thu hồi tin nhắn", "Cần rule antirecall", Keys.ANTI_RECALL))
            it.addView(toggle("Áp dụng cả tin do mình thu hồi", null, Keys.ANTI_RECALL_INCLUDE_ME))
            it.addView(toggle("Chống xóa tin nhắn", null, Keys.ANTI_DELETE))
            it.addView(toggle("Áp dụng cả tin do mình xóa", null, Keys.ANTI_DELETE_INCLUDE_ME))
            it.addView(toggle("Hiện nội dung tin đã thu hồi/xóa", null, Keys.SHOW_CONTENT))
            it.addView(field("User ID của bạn", "Không bắt buộc, chỉ nhận dãy số UID Zalo", Keys.MY_UID, "", false, true))
            it.addView(hint("Lịch sử thu hồi/xóa: Android/data/com.zing.zalo/files/ninjazalo/recalled.txt"))
        })

        root.addView(section("TIN NHẮN KHÁC") {
            it.addView(toggle("Chặn gửi \"Đã xem\"", "Cần rule skip", Keys.BLOCK_SEEN))
            it.addView(toggle("Không gửi \"Đang nhập\"", "Cần rule skip", Keys.SILENT_TYPING))
            it.addView(toggle("Ghi đè TTL tin nhắn", "Cần rule ttl", Keys.TTL_ENABLE))
            it.addView(field("Giá trị TTL", null, Keys.TTL_VALUE, "0", false, true))
        })

        root.addView(section("NÂNG CAO") {
            it.addView(field("Chặn thêm", "Mỗi dòng một đoạn URL", Keys.EXTRA_PATTERNS, "", true, false))
            it.addView(field("rules.json", "Hook theo class/method của Zalo", Keys.RULES_JSON, "", true, false))
            it.addView(toggle("Ghi log mọi request", "Xem trong LSPosed log", Keys.LOG_REQUESTS))
            it.addView(toggle("Quét class/method khi mở Zalo", "Bật xong nhớ tắt", Keys.SCAN_NOW))
        })

        root.addView(MaterialButton(this).apply {
            text = "LƯU THAY ĐỔI"
            setTextColor(bg())
            backgroundTintList = android.content.res.ColorStateList.valueOf(gold())
            cornerRadius = dp8
            setPadding(0, dp16, 0, dp16)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp24
                bottomMargin = dp8
            }
            setOnClickListener {
                savers.forEach { s -> s() }
                Toast.makeText(this@SettingsActivity, "Đã lưu. Đóng hẳn Zalo rồi mở lại.", Toast.LENGTH_LONG).show()
            }
        })

        root.addView(TextView(this).apply {
            text = "Bật module trong LSPosed, chọn phạm vi Zalo (com.zing.zalo), rồi đóng hẳn Zalo và mở lại để áp dụng."
            setTextColor(textSecondary())
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(dp16, dp8, dp16, 0)
        })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(bg())
            addView(root)
        })
    }

    private fun header(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, 0, 0, dp24)
        addView(TextView(this@SettingsActivity).apply {
            text = "NINJAZALO"
            setTextColor(gold())
            textSize = 28f
            letterSpacing = 0.08f
            setTypeface(Typeface.DEFAULT_BOLD)
        })
        addView(TextView(this@SettingsActivity).apply {
            text = "MODDED"
            setTextColor(textSecondary())
            textSize = 13f
            letterSpacing = 0.3f
            setPadding(0, dp8 / 4, 0, 0)
        })
        addView(View(this@SettingsActivity).apply {
            setBackgroundColor(gold())
            layoutParams = LinearLayout.LayoutParams(dp16 * 2, (2 * resources.displayMetrics.density).toInt()).apply {
                topMargin = dp8
            }
        })
    }

    private fun card(): MaterialCardView = MaterialCardView(this).apply {
        radius = dp16.toFloat()
        cardElevation = 0f
        setCardBackgroundColor(surface())
        strokeColor = resources.getColor(R.color.nz_divider, theme)
        strokeWidth = 1
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp16
        }
    }

    private fun section(title: String, build: (LinearLayout) -> Unit): View {
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp16, dp16, dp16, dp8)
        }
        inner.addView(TextView(this).apply {
            text = title
            setTextColor(gold())
            textSize = 13f
            letterSpacing = 0.1f
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(0, 0, 0, dp8)
        })
        build(inner)
        return card().apply { addView(inner) }
    }

    private fun toggle(label: String, desc: String?, key: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp8, 0, dp8)
        }
        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        textCol.addView(TextView(this).apply {
            text = label
            setTextColor(textPrimary())
            textSize = 15f
        })
        if (desc != null) {
            textCol.addView(TextView(this).apply {
                text = desc
                setTextColor(textSecondary())
                textSize = 12f
            })
        }
        row.addView(textCol)
        row.addView(MaterialSwitch(this).apply {
            isChecked = prefs.getBoolean(key, Keys.defaultBool(key))
            trackTintList = android.content.res.ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
                intArrayOf(gold(), resources.getColor(R.color.nz_divider, theme))
            )
            thumbTintList = android.content.res.ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
                intArrayOf(bg(), textSecondary())
            )
            setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean(key, checked).apply() }
        })
        return row
    }

    private fun field(label: String, desc: String?, key: String, def: String, multiline: Boolean, numeric: Boolean): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp8, 0, dp8)
        }
        if (desc != null) {
            box.addView(TextView(this).apply {
                text = desc
                setTextColor(textSecondary())
                textSize = 12f
                setPadding(0, 0, 0, dp8 / 2)
            })
        }
        val til = TextInputLayout(this).apply {
            hint = label
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxStrokeColor = gold()
            setBoxCornerRadii(dp8.toFloat(), dp8.toFloat(), dp8.toFloat(), dp8.toFloat())
            defaultHintTextColor = android.content.res.ColorStateList.valueOf(textSecondary())
        }
        val edit = TextInputEditText(til.context).apply {
            setText(prefs.getString(key, def))
            setTextColor(textPrimary())
            if (multiline) {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                minLines = 3
                typeface = Typeface.MONOSPACE
                textSize = 12f
                gravity = Gravity.TOP or Gravity.START
            } else {
                inputType = if (numeric) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
            }
        }
        til.addView(edit)
        box.addView(til)
        savers.add { prefs.edit().putString(key, edit.text.toString()).apply() }
        return box
    }

    private fun hint(text: String): View = TextView(this).apply {
        this.text = text
        setTextColor(textSecondary())
        textSize = 11f
        setPadding(0, dp8 / 2, 0, 0)
    }
}
