package com.qiyuan.keytools.ui.mapping

import android.app.Dialog
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.qiyuan.keytools.R
import com.qiyuan.keytools.model.*

/**
 * 配置按键映射的对话框
 * 支持：无操作 / 发送广播 / 启动应用 / 模拟系统按键 / Shell命令
 */
class MappingEditDialog : DialogFragment() {

    var onSave: ((KeyMapping) -> Unit)? = null

    private lateinit var mapping: KeyMapping

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mapping = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(ARG_MAPPING, KeyMapping::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable(ARG_MAPPING) as? KeyMapping
        } ?: return super.onCreateDialog(savedInstanceState)

        val ctx = requireContext()
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_mapping_edit, null)

        // ── 标题展示 ──────────────────────────────────────
        view.findViewById<TextView>(R.id.tv_key_id).text = mapping.keyId().displayName()

        // ── 动作类型 Spinner ─────────────────────────────
        val spinnerType = view.findViewById<Spinner>(R.id.spinner_action_type)
        val typeLabels  = ActionType.values().map { it.label }.toTypedArray()
        spinnerType.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, typeLabels)
        spinnerType.setSelection(ActionType.values().indexOf(mapping.actionType))

        // ── 各类型参数面板 ────────────────────────────────
        val panelBroadcast  = view.findViewById<View>(R.id.panel_broadcast)
        val panelLaunchApp  = view.findViewById<View>(R.id.panel_launch_app)
        val panelKeyEvent   = view.findViewById<View>(R.id.panel_keyevent)
        val panelShell      = view.findViewById<View>(R.id.panel_shell)

        val etBroadcastAction = view.findViewById<EditText>(R.id.et_broadcast_action)
        val spinnerKeyEvent   = view.findViewById<Spinner>(R.id.spinner_keyevent)
        val spinnerApp        = view.findViewById<Spinner>(R.id.spinner_app)
        val etShell           = view.findViewById<EditText>(R.id.et_shell)

        // 初始值回填
        etBroadcastAction.setText(mapping.broadcastAction)
        etShell.setText(mapping.shellCommand)

        // 系统按键选项
        val keyOptions = SystemKeyOption.labels()
        spinnerKeyEvent.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, keyOptions)
        SystemKeyOption.fromLabel(mapping.keyEventLabel)?.let {
            spinnerKeyEvent.setSelection(SystemKeyOption.values().indexOf(it))
        }

        // 已安装应用列表
        val pm = ctx.packageManager
        val apps = pm.getInstalledApplications(0)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { Pair(it.packageName, pm.getApplicationLabel(it).toString()) }
            .sortedBy { it.second }
        val appLabels = apps.map { it.second }.toTypedArray()
        spinnerApp.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, appLabels)
        val appIdx = apps.indexOfFirst { it.first == mapping.launchPackage }
        if (appIdx >= 0) spinnerApp.setSelection(appIdx)

        fun showPanel(type: ActionType) {
            panelBroadcast.visibility = if (type == ActionType.BROADCAST)  View.VISIBLE else View.GONE
            panelLaunchApp.visibility = if (type == ActionType.LAUNCH_APP) View.VISIBLE else View.GONE
            panelKeyEvent.visibility  = if (type == ActionType.KEYEVENT)   View.VISIBLE else View.GONE
            panelShell.visibility     = if (type == ActionType.SHELL)      View.VISIBLE else View.GONE
        }
        showPanel(mapping.actionType)

        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, position: Int, id: Long) {
                showPanel(ActionType.values()[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        return AlertDialog.Builder(ctx)
            .setView(view)
            .setTitle("配置按键映射")
            .setPositiveButton("保存") { _, _ ->
                val selectedType = ActionType.values()[spinnerType.selectedItemPosition]
                val selectedApp  = if (apps.isNotEmpty() && spinnerApp.selectedItemPosition in apps.indices)
                    apps[spinnerApp.selectedItemPosition] else Pair("", "")
                val selectedKey  = if (keyOptions.isNotEmpty())
                    keyOptions[spinnerKeyEvent.selectedItemPosition] else ""

                val updated = mapping.copy(
                    actionType      = selectedType,
                    broadcastAction = etBroadcastAction.text.toString().trim(),
                    launchPackage   = selectedApp.first,
                    launchAppName   = selectedApp.second,
                    keyEventLabel   = selectedKey,
                    shellCommand    = etShell.text.toString().trim()
                )
                onSave?.invoke(updated)
            }
            .setNegativeButton("取消", null)
            .create()
    }

    companion object {
        private const val ARG_MAPPING = "mapping"
        fun newInstance(mapping: KeyMapping): MappingEditDialog {
            // KeyMapping 需实现 Serializable
            val args = Bundle().apply { putSerializable(ARG_MAPPING, mapping) }
            return MappingEditDialog().apply { arguments = args }
        }
    }
}
