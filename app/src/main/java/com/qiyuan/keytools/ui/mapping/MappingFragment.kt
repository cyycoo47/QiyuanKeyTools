package com.qiyuan.keytools.ui.mapping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.qiyuan.keytools.databinding.FragmentMappingBinding
import com.qiyuan.keytools.model.KeyId
import com.qiyuan.keytools.viewmodel.MappingViewModel

class MappingFragment : Fragment() {

    private var _binding: FragmentMappingBinding? = null
    private val binding get() = _binding!!
    private val vm: MappingViewModel by viewModels()
    private lateinit var adapter: MappingAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMappingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MappingAdapter(
            onEdit   = { keyId -> openEditDialog(keyId) },
            onDelete = { keyId ->
                vm.deleteMapping(keyId)
                Toast.makeText(requireContext(), "已删除映射", Toast.LENGTH_SHORT).show()
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // 已检测码值变化 → 刷新列表
        vm.detectedKeys.observe(viewLifecycleOwner) { keys ->
            refreshList(keys)
        }
        vm.mappings.observe(viewLifecycleOwner) {
            refreshList(vm.detectedKeys.value ?: emptyList())
        }

        binding.btnResetAll.setOnClickListener {
            vm.resetAllMappings()
            Toast.makeText(requireContext(), "已重置全部映射", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshList(keys: List<KeyId>) {
        val mappings = vm.mappings.value ?: emptyList()
        val items = keys.map { keyId ->
            val m = mappings.firstOrNull { it.source == keyId.source && it.rawValue == keyId.rawValue }
            MappingAdapter.Item(keyId, m?.actionSummary() ?: "无操作")
        }
        adapter.submitList(items)
        binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openEditDialog(keyId: KeyId) {
        val existing = vm.getMappingFor(keyId)
        MappingEditDialog.newInstance(existing).apply {
            onSave = { mapping ->
                vm.saveMapping(mapping)
                Toast.makeText(requireContext(), "映射已保存", Toast.LENGTH_SHORT).show()
            }
        }.show(childFragmentManager, "edit_mapping")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
