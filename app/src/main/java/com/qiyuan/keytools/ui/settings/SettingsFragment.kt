package com.qiyuan.keytools.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.qiyuan.keytools.databinding.FragmentSettingsBinding
import com.qiyuan.keytools.service.KeyListenerService
import com.qiyuan.keytools.viewmodel.SettingsViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val vm: SettingsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── SDK 状态 ──────────────────────────────────────
        vm.sdkConnected.observe(viewLifecycleOwner) { connected ->
            binding.tvSdkStatus.text = if (connected) "● SDK 已连接" else "● SDK 未连接"
            binding.tvSdkStatus.setTextColor(
                resources.getColor(
                    if (connected) com.qiyuan.keytools.R.color.accent_green
                    else com.qiyuan.keytools.R.color.accent_red, null
                )
            )
        }

        // ── 版本信息 ──────────────────────────────────────
        binding.tvAppVersion.text = "应用版本：${vm.getAppVersion()}"
        binding.tvSdkVersion.text = "SDK 版本：${vm.getSdkVersion()}"

        // ── 服务开关 ──────────────────────────────────────
        binding.switchService.isChecked = vm.serviceRunning.value ?: false
        vm.serviceRunning.observe(viewLifecycleOwner) { running ->
            binding.switchService.isChecked = running
        }
        binding.switchService.setOnCheckedChangeListener { _, isChecked ->
            vm.setServiceEnabled(isChecked)
            toggleService(isChecked)
        }

        // ── 属性监听开关 ──────────────────────────────────
        binding.switchRoty.isChecked = vm.listenRotyBox
        binding.switchMfs.isChecked  = vm.listenMfs
        binding.switchHw.isChecked   = vm.listenHwKey

        binding.switchRoty.setOnCheckedChangeListener { _, v -> vm.setListenRotyBox(v) }
        binding.switchMfs.setOnCheckedChangeListener  { _, v -> vm.setListenMfs(v) }
        binding.switchHw.setOnCheckedChangeListener   { _, v -> vm.setListenHwKey(v) }

        // ── 导出配置 ──────────────────────────────────────
        binding.btnExportConfig.setOnClickListener {
            val json = vm.exportConfigJson()
            if (json == "[]") {
                Toast.makeText(requireContext(), "当前无映射配置", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 复制到剪贴板
            val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("qy_config", json))
            // 弹窗显示
            AlertDialog.Builder(requireContext())
                .setTitle("配置 JSON（已复制到剪贴板）")
                .setMessage(json)
                .setPositiveButton("确定", null)
                .show()
        }

        // ── 导入配置 ──────────────────────────────────────
        binding.btnImportConfig.setOnClickListener {
            showImportDialog()
        }

        // ── 属性ID参考 ───────────────────────────────────
        binding.tvPropRef.text = buildPropRef()
    }

    private fun toggleService(enable: Boolean) {
        val ctx = requireContext()
        val intent = Intent(ctx, KeyListenerService::class.java)
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
            Toast.makeText(ctx, "监听服务已启动", Toast.LENGTH_SHORT).show()
        } else {
            ctx.stopService(intent)
            Toast.makeText(ctx, "监听服务已停止", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImportDialog() {
        val etJson = android.widget.EditText(requireContext()).apply {
            hint = "粘贴配置 JSON 到这里"
            minLines = 4
            maxLines = 10
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
        }
        AlertDialog.Builder(requireContext())
            .setTitle("导入映射配置")
            .setView(etJson)
            .setPositiveButton("导入") { _, _ ->
                val json = etJson.text.toString().trim()
                if (vm.importConfigJson(json)) {
                    Toast.makeText(requireContext(), "导入成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "导入失败：JSON 格式错误", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun buildPropRef(): String = """
属性 ID 参考（hex）
─────────────────────────────
ROTY_BOX  0x4131EC00
MFS       0x4131EE00
HW_KEY    0x11410A10
    """.trimIndent()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
