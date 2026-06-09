package com.qiyuan.keytools.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.*
import com.qiyuan.keytools.App
import com.qiyuan.keytools.model.*
import com.qiyuan.keytools.sdk.KeySdkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * 映射页面 ViewModel
 * 维护已检测码值列表 + 持久化映射配置
 */
class MappingViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as App
    private val sdkManager = KeySdkManager.get(application)

    // 已检测到的唯一码值（来源+值）
    private val _detectedKeys = MutableLiveData<List<KeyId>>(emptyList())
    val detectedKeys: LiveData<List<KeyId>> = _detectedKeys

    // 当前所有映射配置
    private val _mappings = MutableLiveData<List<KeyMapping>>(emptyList())
    val mappings: LiveData<List<KeyMapping>> = _mappings

    // 已安装应用列表（供选择）
    private val _installedApps = MutableLiveData<List<AppInfo>>(emptyList())
    val installedApps: LiveData<List<AppInfo>> = _installedApps

    data class AppInfo(val packageName: String, val label: String)

    init {
        loadMappings()
        loadInstalledApps()
        // 监听 SDK 事件，自动收集新码值
        viewModelScope.launch {
            sdkManager.keyEvents.collect { raw ->
                val keyId = KeyId(raw.source, raw.rawValue)
                val current = _detectedKeys.value?.toMutableList() ?: mutableListOf()
                if (current.none { it.source == keyId.source && it.rawValue == keyId.rawValue }) {
                    current.add(keyId)
                    _detectedKeys.postValue(current)
                }
            }
        }
    }

    private fun loadMappings() {
        _mappings.value = app.prefs.loadMappings()
    }

    fun getMappingFor(keyId: KeyId): KeyMapping {
        return _mappings.value?.firstOrNull {
            it.source == keyId.source && it.rawValue == keyId.rawValue
        } ?: KeyMapping(source = keyId.source, rawValue = keyId.rawValue)
    }

    fun saveMapping(mapping: KeyMapping) {
        val list = app.prefs.loadMappings().toMutableList()
        val idx = list.indexOfFirst { it.source == mapping.source && it.rawValue == mapping.rawValue }
        if (idx >= 0) list[idx] = mapping else list.add(mapping)
        app.prefs.saveMappings(list)
        _mappings.postValue(list)
    }

    fun deleteMapping(keyId: KeyId) {
        val list = app.prefs.loadMappings().toMutableList()
        list.removeAll { it.source == keyId.source && it.rawValue == keyId.rawValue }
        app.prefs.saveMappings(list)
        _mappings.postValue(list)
    }

    fun resetAllMappings() {
        app.prefs.saveMappings(emptyList())
        _mappings.value = emptyList()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val apps = pm.getInstalledApplications(0)
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                .map { AppInfo(it.packageName, pm.getApplicationLabel(it).toString()) }
                .sortedBy { it.label }
            _installedApps.postValue(apps)
        }
    }
}
