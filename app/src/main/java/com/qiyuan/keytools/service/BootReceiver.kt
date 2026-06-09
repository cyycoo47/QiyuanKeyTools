package com.qiyuan.keytools.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.qiyuan.keytools.App

/**
 * 开机自启：若用户曾开启服务，开机后自动启动
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = (context.applicationContext as App).prefs
        if (prefs.serviceEnabled) {
            val serviceIntent = Intent(context, KeyListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
