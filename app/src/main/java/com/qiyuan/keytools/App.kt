package com.qiyuan.keytools

import android.app.Application
import com.qiyuan.keytools.data.KeyPreferences
import com.qiyuan.keytools.sdk.KeySdkManager

class App : Application() {
    lateinit var prefs: KeyPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = KeyPreferences(this)
        // 初始化 SDK 连接（全局共享）
        KeySdkManager.get(this).init()
    }
}
