package com.qiyuan.keytools.ui.detect

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.qiyuan.keytools.databinding.ItemKeyEventBinding
import com.qiyuan.keytools.model.KeyEventRecord

class KeyEventAdapter : ListAdapter<KeyEventRecord, KeyEventAdapter.VH>(DIFF) {

    inner class VH(private val b: ItemKeyEventBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(r: KeyEventRecord) {
            b.tvTime.text   = r.timeDisplay()
            b.tvSource.text = r.source.label
            b.tvPropId.text = r.propIdHex()
            b.tvValue.text  = r.valueDisplay()
            b.tvAction.text = r.action.label
            b.tvSource.setTextColor(b.root.context.getColor(r.source.colorRes))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemKeyEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<KeyEventRecord>() {
            override fun areItemsTheSame(a: KeyEventRecord, b: KeyEventRecord) = a.id == b.id
            override fun areContentsTheSame(a: KeyEventRecord, b: KeyEventRecord) = a == b
        }
    }
}
