package com.qiyuan.keytools.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.qiyuan.keytools.model.KeyMapping
import com.qiyuan.keytools.model.KeySource

/**
 * 持久化存储：映射配置 + 应用设置
 */
class KeyPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // ── 映射配置 ─────────────────────────────────────────

    fun saveMappings(mappings: List<KeyMapping>) {
        val json = gson.toJson(mappings)
        prefs.edit().putString(KEY_MAPPINGS, json).apply()
    }

    fun loadMappings(): MutableList<KeyMapping> {
        val json = prefs.getString(KEY_MAPPINGS, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<KeyMapping>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    // ── 应用设置 ─────────────────────────────────────────

    var serviceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, false)
        set(v) = prefs.edit().putBoolean(KEY_SERVICE_ENABLED, v).apply()

    var listenRotyBox: Boolean
        get() = prefs.getBoolean(KEY_LISTEN_ROTY, true)
        set(v) = prefs.edit().putBoolean(KEY_LISTEN_ROTY, v).apply()

    var listenMfs: Boolean
        get() = prefs.getBoolean(KEY_LISTEN_MFS, true)
        set(v) = prefs.edit().putBoolean(KEY_LISTEN_MFS, v).apply()

    var listenHwKey: Boolean
        get() = prefs.getBoolean(KEY_LISTEN_HW, true)
        set(v) = prefs.edit().putBoolean(KEY_LISTEN_HW, v).apply()

    // ── 导出/导入（JSON 字符串） ──────────────────────────

    fun exportJson(): String = gson.toJson(loadMappings())

    fun importJson(json: String): Boolean {
        return try {
            val type = object : TypeToken<MutableList<KeyMapping>>() {}.type
            val list: MutableList<KeyMapping> = gson.fromJson(json, type)
            saveMappings(list)
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val PREF_NAME = "qy_key_tools"
        private const val KEY_MAPPINGS = "mappings"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_LISTEN_ROTY = "listen_roty"
        private const val KEY_LISTEN_MFS = "listen_mfs"
        private const val KEY_LISTEN_HW = "listen_hw"
    }
}
