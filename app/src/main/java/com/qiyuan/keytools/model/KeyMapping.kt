package com.qiyuan.keytools.model

import java.io.Serializable

/**
 * 支持的功能动作类型
 */
enum class ActionType(val label: String) {
    NONE("无操作"),
    BROADCAST("发送广播"),
    LAUNCH_APP("启动应用"),
    KEYEVENT("模拟系统按键"),
    SHELL("执行Shell命令")
}

/**
 * 系统按键选项
 */
enum class SystemKeyOption(val label: String, val keyCode: Int) {
    VOLUME_UP("音量+", android.view.KeyEvent.KEYCODE_VOLUME_UP),
    VOLUME_DOWN("音量-", android.view.KeyEvent.KEYCODE_VOLUME_DOWN),
    MEDIA_PLAY_PAUSE("播放/暂停", android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE),
    MEDIA_NEXT("下一曲", android.view.KeyEvent.KEYCODE_MEDIA_NEXT),
    MEDIA_PREVIOUS("上一曲", android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS),
    BACK("返回", android.view.KeyEvent.KEYCODE_BACK),
    HOME("Home", android.view.KeyEvent.KEYCODE_HOME);

    companion object {
        fun labels() = values().map { it.label }.toTypedArray()
        fun fromLabel(label: String) = values().firstOrNull { it.label == label }
    }
}

/**
 * 按键映射配置
 * source + rawValue → 一个 ActionConfig
 */
data class KeyMapping(
    val source: KeySource,
    val rawValue: Int,
    val actionType: ActionType = ActionType.NONE,
    val broadcastAction: String = "",
    val launchPackage: String = "",
    val launchAppName: String = "",
    val keyEventLabel: String = "",
    val shellCommand: String = ""
) : Serializable {
    fun keyId() = KeyId(source, rawValue)

    fun actionSummary(): String = when (actionType) {
        ActionType.NONE        -> "无操作"
        ActionType.BROADCAST   -> "广播: $broadcastAction"
        ActionType.LAUNCH_APP  -> "启动: $launchAppName"
        ActionType.KEYEVENT    -> "按键: $keyEventLabel"
        ActionType.SHELL       -> "Shell: $shellCommand"
    }
}
