package com.qiyuan.keytools.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.*
import com.qiyuan.keytools.App
import com.qiyuan.keytools.sdk.KeySdkManager
import androidx.lifecycle.asLiveData

/**
 * 设置页面 ViewModel
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as App
    private val prefs = app.prefs
    private val sdkManager = KeySdkManager.get(application)

    val sdkConnected: LiveData<Boolean> = sdkManager.connected
        .asLiveData(viewModelScope.coroutineContext)

    private val _serviceRunning = MutableLiveData(prefs.serviceEnabled)
    val serviceRunning: LiveData<Boolean> = _serviceRunning

    val listenRotyBox get() = prefs.listenRotyBox
    val listenMfs     get() = prefs.listenMfs
    val listenHwKey   get() = prefs.listenHwKey

    fun setServiceEnabled(enabled: Boolean) {
        prefs.serviceEnabled = enabled
        _serviceRunning.value = enabled
    }

    fun setListenRotyBox(v: Boolean) { prefs.listenRotyBox = v }
    fun setListenMfs(v: Boolean)     { prefs.listenMfs = v }
    fun setListenHwKey(v: Boolean)   { prefs.listenHwKey = v }

    fun exportConfigJson(): String = prefs.exportJson()

    fun importConfigJson(json: String): Boolean = prefs.importJson(json)

    fun getSdkVersion(): String = "V1.0.0.0"
    fun getAppVersion(): String = try {
        val ctx = getApplication<Application>()
        val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            "v${pi.versionName} (${pi.longVersionCode})"
        else
            @Suppress("DEPRECATION") "v${pi.versionName} (${pi.versionCode})"
    } catch (e: Exception) { "未知" }
}
