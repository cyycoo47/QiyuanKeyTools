package com.qiyuan.keytools.model

/**
 * 按键事件记录（检测页面用）
 */
data class KeyEventRecord(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val source: KeySource,
    val propId: Int,
    val rawValue: Int,
    val action: KeyAction = KeyAction.UNKNOWN
) {
    fun propIdHex(): String = "0x${propId.toUInt().toString(16).uppercase().padStart(8, '0')}"
    fun valueHex(): String = "0x${rawValue.toUInt().toString(16).uppercase().padStart(4, '0')}"
    fun valueDisplay(): String = "$rawValue (${valueHex()})"
    fun timeDisplay(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}

enum class KeySource(val label: String, val colorRes: Int) {
    ROTY_BOX("ROTY_BOX", com.qiyuan.keytools.R.color.tag_roty),
    MFS("MFS", com.qiyuan.keytools.R.color.tag_mfs),
    HW_KEY("HW_KEY", com.qiyuan.keytools.R.color.tag_hw),
    UNKNOWN("UNKNOWN", com.qiyuan.keytools.R.color.on_surface_dim);

    companion object {
        fun fromPropId(propId: Int): KeySource = when (propId) {
            KeyPropIds.ROTY_BOX -> ROTY_BOX
            KeyPropIds.MFS      -> MFS
            KeyPropIds.HW_KEY   -> HW_KEY
            else                -> UNKNOWN
        }
    }
}

enum class KeyAction(val label: String) {
    DOWN("按下"),
    UP("抬起"),
    LONG_PRESS("长按"),
    UNKNOWN("未知")
}

/**
 * 属性 ID 常量
 */
object KeyPropIds {
    const val ROTY_BOX = 0x4131EC00.toInt()  // ID_OTHER_ROTY_BOX_SWT_STATUS
    const val MFS      = 0x4131EE00.toInt()  // ID_OTHER_MFS_SWT_STATUS
    const val HW_KEY   = 0x11410A10          // HW_KEY_INPUT

    val ALL = listOf(ROTY_BOX, MFS, HW_KEY)
}

/**
 * 唯一按键标识（来源 + 码值）
 */
data class KeyId(val source: KeySource, val rawValue: Int) {
    fun displayName(): String = "${source.label}  ${rawValue} (0x${rawValue.toUInt().toString(16).uppercase()})"
}
