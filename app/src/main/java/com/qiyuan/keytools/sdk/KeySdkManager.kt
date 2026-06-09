package com.qiyuan.keytools.sdk

import android.content.Context
import android.util.Log
import com.changan.sda.opensdk.client.CaOpenSdkManager
import com.changan.sda.opensdk.client.OpenSdkInitCallback
import com.changan.sda.opensdk.client.callback.CarPropertyEventCallback
import com.changan.sda.opensdk.api.entity.CarPropertyValue
import com.qiyuan.keytools.model.KeyAction
import com.qiyuan.keytools.model.KeyPropIds
import com.qiyuan.keytools.model.KeySource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 封装启源 SDK 的按键属性监听
 * 使用单例模式，全局共享连接状态
 */
class KeySdkManager private constructor(private val context: Context) {

    private val TAG = "KeySdkManager"
    private val sdk = CaOpenSdkManager.getInstance()

    // SDK 连接状态
    private val _connected = MutableSharedFlow<Boolean>(replay = 1)
    val connected: SharedFlow<Boolean> = _connected.asSharedFlow()

    // 按键原始事件流：Triple(propId, rawValue, source)
    private val _keyEvents = MutableSharedFlow<RawKeyEvent>(extraBufferCapacity = 64)
    val keyEvents: SharedFlow<RawKeyEvent> = _keyEvents.asSharedFlow()

    data class RawKeyEvent(
        val source: KeySource,
        val propId: Int,
        val rawValue: Int,
        val action: KeyAction
    )

    // 已注册的 propId 集合
    private val registeredProps = mutableSetOf<Int>()

    // ── 初始化 & 连接 ─────────────────────────────────────

    fun init() {
        sdk.init(context, object : OpenSdkInitCallback {
            override fun onInitSuccess() {
                Log.i(TAG, "SDK 初始化成功")
                _connected.tryEmit(true)
            }
            override fun onInitFailed(code: Int, msg: String?) {
                Log.e(TAG, "SDK 初始化失败: code=$code msg=$msg")
                _connected.tryEmit(false)
            }
        })
    }

    // ── 按需注册/注销属性监听 ─────────────────────────────

    fun registerProp(propId: Int) {
        if (registeredProps.contains(propId)) return
        try {
            sdk.registerPropertyCallback(propId, 0f, buildCallback(propId))
            registeredProps.add(propId)
            Log.i(TAG, "注册属性监听: 0x${propId.toUInt().toString(16).uppercase()}")
        } catch (e: Exception) {
            Log.e(TAG, "注册属性失败: $e")
        }
    }

    fun unregisterProp(propId: Int) {
        if (!registeredProps.contains(propId)) return
        try {
            sdk.unRegisterPropertyCallback(propId, buildCallback(propId))
            registeredProps.remove(propId)
        } catch (e: Exception) {
            Log.e(TAG, "注销属性失败: $e")
        }
    }

    fun registerAll(listenRoty: Boolean, listenMfs: Boolean, listenHw: Boolean) {
        if (listenRoty) registerProp(KeyPropIds.ROTY_BOX)
        if (listenMfs)  registerProp(KeyPropIds.MFS)
        if (listenHw)   registerProp(KeyPropIds.HW_KEY)
    }

    fun unregisterAll() {
        KeyPropIds.ALL.forEach { unregisterProp(it) }
    }

    // ── 内部回调构建 ──────────────────────────────────────

    private val callbackCache = mutableMapOf<Int, CarPropertyEventCallback<*>>()

    @Suppress("UNCHECKED_CAST")
    private fun buildCallback(propId: Int): CarPropertyEventCallback<Int> {
        return callbackCache.getOrPut(propId) {
            object : CarPropertyEventCallback<Int>() {
                override fun onChangeEvent(value: CarPropertyValue<Int>) {
                    val raw = value.value ?: return
                    val source = KeySource.fromPropId(propId)
                    val action = parseAction(propId, raw)
                    Log.d(TAG, "按键事件: source=$source raw=$raw action=$action")
                    _keyEvents.tryEmit(RawKeyEvent(source, propId, raw, action))
                }
                override fun onErrorEvent(propId: Int, areaId: Int) {
                    Log.w(TAG, "属性错误: propId=0x${propId.toUInt().toString(16).uppercase()}")
                }
            }
        } as CarPropertyEventCallback<Int>
    }

    /**
     * 解析动作类型
     * HW_KEY_INPUT 的 value 是 int[3]: [keyCode, action(0=down,1=up), displayId]
     * ROTY_BOX / MFS 返回整型码值，约定最高位或特定值表示抬起（需实车测试后修正）
     */
    private fun parseAction(propId: Int, raw: Int): KeyAction {
        return when (propId) {
            KeyPropIds.HW_KEY -> {
                // Android VHAL HW_KEY_INPUT: bit[16..31]=keyCode, bit[0..15]=action
                val action = (raw shr 16) and 0xFF
                when (action) {
                    0 -> KeyAction.DOWN
                    1 -> KeyAction.UP
                    2 -> KeyAction.LONG_PRESS
                    else -> KeyAction.UNKNOWN
                }
            }
            else -> KeyAction.DOWN  // ROTY/MFS 先统一标为 DOWN，待实车校准
        }
    }

    companion object {
        @Volatile private var instance: KeySdkManager? = null
        fun get(context: Context): KeySdkManager =
            instance ?: synchronized(this) {
                instance ?: KeySdkManager(context.applicationContext).also { instance = it }
            }
    }
}
