package com.qiyuan.keytools.service

import android.app.*
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent as AndroidKeyEvent
import com.qiyuan.keytools.App
import com.qiyuan.keytools.R
import com.qiyuan.keytools.model.*
import com.qiyuan.keytools.sdk.KeySdkManager
import com.qiyuan.keytools.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * 前台 Service：后台持续监听按键事件，执行映射动作
 */
class KeyListenerService : Service() {

    private val TAG = "KeyListenerService"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var sdkManager: KeySdkManager

    override fun onCreate() {
        super.onCreate()
        sdkManager = KeySdkManager.get(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as App
        val prefs = app.prefs

        // 注册属性监听
        sdkManager.registerAll(
            listenRoty = prefs.listenRotyBox,
            listenMfs  = prefs.listenMfs,
            listenHw   = prefs.listenHwKey
        )

        // 收集按键事件 → 执行映射
        scope.launch {
            sdkManager.keyEvents.collectLatest { raw ->
                val mappings = prefs.loadMappings()
                val mapping = mappings.firstOrNull {
                    it.source == raw.source && it.rawValue == raw.rawValue
                } ?: return@collectLatest

                if (raw.action == KeyAction.UP) return@collectLatest  // 只在 DOWN/LONG_PRESS 执行

                executeAction(mapping)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        sdkManager.unregisterAll()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── 执行动作 ──────────────────────────────────────────

    private fun executeAction(mapping: KeyMapping) {
        Log.i(TAG, "执行动作: ${mapping.actionSummary()}")
        when (mapping.actionType) {
            ActionType.NONE -> {}

            ActionType.BROADCAST -> {
                if (mapping.broadcastAction.isNotBlank()) {
                    sendBroadcast(Intent(mapping.broadcastAction))
                }
            }

            ActionType.LAUNCH_APP -> {
                if (mapping.launchPackage.isNotBlank()) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(mapping.launchPackage)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(launchIntent)
                    } else {
                        Log.w(TAG, "找不到应用: ${mapping.launchPackage}")
                    }
                }
            }

            ActionType.KEYEVENT -> {
                val option = SystemKeyOption.fromLabel(mapping.keyEventLabel)
                if (option != null) {
                    simulateKey(option.keyCode)
                }
            }

            ActionType.SHELL -> {
                if (mapping.shellCommand.isNotBlank()) {
                    try {
                        Runtime.getRuntime().exec(mapping.shellCommand)
                    } catch (e: Exception) {
                        Log.e(TAG, "Shell 执行失败: $e")
                    }
                }
            }
        }
    }

    private fun simulateKey(keyCode: Int) {
        try {
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            when (keyCode) {
                AndroidKeyEvent.KEYCODE_VOLUME_UP   -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                AndroidKeyEvent.KEYCODE_VOLUME_DOWN -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                else -> {
                    am.dispatchMediaKeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, keyCode))
                    am.dispatchMediaKeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_UP, keyCode))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "模拟按键失败: $e")
        }
    }

    // ── 通知 ──────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "物理按键监听服务" }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "key_listener_channel"
        const val NOTIFICATION_ID = 1001
    }
}
