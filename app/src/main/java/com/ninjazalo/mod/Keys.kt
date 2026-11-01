package com.ninjazalo.mod

object Keys {
    const val MODULE_PKG = "com.ninjazalo.mod"
    const val TARGET_PKG = "com.zing.zalo"
    const val PREFS = "ninjazalo_prefs"

    const val ANTI_RECALL = "enable_anti_recall"
    const val ANTI_DELETE = "enable_anti_delete"
    const val ANTI_RECALL_INCLUDE_ME = "anti_recall_include_me"
    const val ANTI_DELETE_INCLUDE_ME = "anti_delete_include_my_deletion"
    const val SHOW_CONTENT = "show_recalled_content"
    const val BLOCK_SEEN = "block_send_seen"
    const val SILENT_TYPING = "enable_silent_typing"
    const val BLOCK_ADS = "block_ads_endpoints"
    const val DISABLE_TRACKING = "logging_disable_zalo_tracking"
    const val BLOCK_QOS = "logging_block_zalo_qos"
    const val DISABLE_FIREBASE = "logging_disable_firebase"
    const val TTL_ENABLE = "enable_msg_ttl_override"
    const val TTL_VALUE = "msg_ttl_value"
    const val MY_UID = "current_user_id"

    const val EXTRA_PATTERNS = "extra_block_patterns"
    const val RULES_JSON = "rules_json"
    const val LOG_REQUESTS = "log_requests"
    const val SCAN_NOW = "scan_now"

    fun defaultBool(key: String): Boolean = when (key) {
        ANTI_RECALL, ANTI_DELETE, SHOW_CONTENT,
        BLOCK_ADS, DISABLE_TRACKING, BLOCK_QOS, DISABLE_FIREBASE -> true
        else -> false
    }
}
