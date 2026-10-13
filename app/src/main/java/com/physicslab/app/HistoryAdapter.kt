package com.physicslab.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.physicslab.app.data.ReadingEntity
import com.physicslab.app.databinding.ItemReadingBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var items: List<ReadingEntity> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault())

    fun submitList(newItems: List<ReadingEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemReadingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReadingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.txtLabel.text = item.label
        holder.binding.txtTime.text = dateFormat.format(Date(item.timestamp))
        holder.binding.txtValue.text = String.format("%.2f %s", item.value, item.unit)
    }

    override fun getItemCount() = items.size
}
