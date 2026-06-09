package com.qiyuan.keytools.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.qiyuan.keytools.App
import com.qiyuan.keytools.model.*
import com.qiyuan.keytools.sdk.KeySdkManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 检测页面 ViewModel
 * 收集按键事件，维护记录列表
 */
class DetectViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as App
    private val sdkManager = KeySdkManager.get(application)

    private val _records = MutableLiveData<List<KeyEventRecord>>(emptyList())
    val records: LiveData<List<KeyEventRecord>> = _records

    private val _isPaused = MutableLiveData(false)
    val isPaused: LiveData<Boolean> = _isPaused

    val sdkConnected: LiveData<Boolean> = sdkManager.connected
        .asLiveData(viewModelScope.coroutineContext)

    init {
        // 注册所有属性监听（检测页面总是监听全部）
        sdkManager.registerAll(
            listenRoty = true,
            listenMfs  = true,
            listenHw   = true
        )
        // 订阅原始事件
        viewModelScope.launch {
            sdkManager.keyEvents.collect { raw ->
                if (_isPaused.value == true) return@collect
                val record = KeyEventRecord(
                    source   = raw.source,
                    propId   = raw.propId,
                    rawValue = raw.rawValue,
                    action   = raw.action
                )
                val current = _records.value?.toMutableList() ?: mutableListOf()
                current.add(0, record)           // 倒序：最新在顶部
                if (current.size > MAX_RECORDS) current.removeAt(current.lastIndex)
                _records.postValue(current)
            }
        }
    }

    fun togglePause() {
        _isPaused.value = _isPaused.value?.not() ?: false
    }

    fun clearRecords() {
        _records.value = emptyList()
    }

    /** 导出记录为纯文本，返回文本内容 */
    fun buildExportText(): String {
        val sb = StringBuilder()
        sb.appendLine("启源按键调试工具 - 按键记录")
        sb.appendLine("导出时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        sb.appendLine("=" .repeat(60))
        _records.value?.forEach { r ->
            sb.appendLine("[${r.timeDisplay()}] 来源:${r.source.label}  属性ID:${r.propIdHex()}  码值:${r.valueDisplay()}  动作:${r.action.label}")
        }
        return sb.toString()
    }

    companion object {
        private const val MAX_RECORDS = 500
    }
}
