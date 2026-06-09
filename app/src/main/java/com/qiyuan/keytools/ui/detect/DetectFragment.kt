package com.qiyuan.keytools.ui.detect

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.qiyuan.keytools.databinding.FragmentDetectBinding
import com.qiyuan.keytools.viewmodel.DetectViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetectFragment : Fragment() {

    private var _binding: FragmentDetectBinding? = null
    private val binding get() = _binding!!
    private val vm: DetectViewModel by viewModels()
    private lateinit var adapter: KeyEventAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = KeyEventAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // 记录列表
        vm.records.observe(viewLifecycleOwner) { records ->
            adapter.submitList(records.toList())
            binding.tvEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
            binding.tvCount.text = "共 ${records.size} 条"
        }

        // SDK 连接状态
        vm.sdkConnected.observe(viewLifecycleOwner) { connected ->
            binding.tvSdkStatus.text = if (connected) "● SDK 已连接" else "● SDK 未连接"
            binding.tvSdkStatus.setTextColor(
                resources.getColor(
                    if (connected) com.qiyuan.keytools.R.color.accent_green
                    else com.qiyuan.keytools.R.color.accent_red, null
                )
            )
        }

        // 暂停/继续
        vm.isPaused.observe(viewLifecycleOwner) { paused ->
            binding.btnPause.text = if (paused) "继续监听" else "暂停监听"
        }

        binding.btnPause.setOnClickListener { vm.togglePause() }

        binding.btnClear.setOnClickListener {
            vm.clearRecords()
            Toast.makeText(requireContext(), "记录已清空", Toast.LENGTH_SHORT).show()
        }

        binding.btnExport.setOnClickListener { exportRecords() }
    }

    private fun exportRecords() {
        val text = vm.buildExportText()
        if (text.isBlank()) {
            Toast.makeText(requireContext(), "暂无记录可导出", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: requireContext().filesDir
            dir.mkdirs()
            val file = File(dir, "key_records_$ts.txt")
            file.writeText(text, Charsets.UTF_8)
            Toast.makeText(requireContext(), "已导出到：${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
