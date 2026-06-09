package com.qiyuan.keytools.ui.mapping

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.qiyuan.keytools.R
import com.qiyuan.keytools.databinding.ItemMappingBinding
import com.qiyuan.keytools.model.KeyId
import com.qiyuan.keytools.model.KeySource

class MappingAdapter(
    private val onEdit: (KeyId) -> Unit,
    private val onDelete: (KeyId) -> Unit
) : ListAdapter<MappingAdapter.Item, MappingAdapter.VH>(DIFF) {

    data class Item(val keyId: KeyId, val actionSummary: String)

    inner class VH(private val b: ItemMappingBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: Item) {
            b.tvKeyId.text      = item.keyId.displayName()
            b.tvAction.text     = item.actionSummary
            b.tvSource.text     = item.keyId.source.label
            b.tvSource.setTextColor(
                b.root.context.getColor(
                    when (item.keyId.source) {
                        KeySource.ROTY_BOX -> R.color.tag_roty
                        KeySource.MFS      -> R.color.tag_mfs
                        KeySource.HW_KEY   -> R.color.tag_hw
                        else               -> R.color.on_surface_dim
                    }
                )
            )
            b.btnEdit.setOnClickListener   { onEdit(item.keyId) }
            b.btnDelete.setOnClickListener { onDelete(item.keyId) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemMappingBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(a: Item, b: Item) =
                a.keyId.source == b.keyId.source && a.keyId.rawValue == b.keyId.rawValue
            override fun areContentsTheSame(a: Item, b: Item) = a == b
        }
    }
}
