package com.physicslab.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.physicslab.app.data.Quantity
import com.physicslab.app.data.SensorReading
import com.physicslab.app.databinding.ItemReadingModernBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReadingAdapter(
    private val onItemClick: (SensorReading) -> Unit,
    private val onRenameClick: (SensorReading) -> Unit,
    private val onDeleteClick: (SensorReading) -> Unit
) : ListAdapter<SensorReading, ReadingAdapter.ViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReadingModernBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemReadingModernBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(reading: SensorReading) {
            binding.tvReadingName.text = reading.name
            binding.tvReadingTimestamp.text = dateFormat.format(Date(reading.timestamp))

            val quantity = try {
                Quantity.valueOf(reading.quantity)
            } catch (e: Exception) {
                null
            }
            val unit = quantity?.unit ?: ""

            // Use same formatting logic as QuantityViewModel
            val formattedValue = when (quantity) {
                Quantity.LIGHT -> formatLight(reading.value)
                Quantity.MAGNETIC_FIELD -> String.format(Locale.US, "%.1f", reading.value)
                Quantity.ACCELERATION -> String.format(Locale.US, "%.2f", reading.value)
                null -> String.format(Locale.US, "%.2f", reading.value)
            }
            binding.tvReadingValue.text = "$formattedValue $unit"

            if (reading.x != null && reading.y != null && reading.z != null) {
                binding.tvReadingComponents.visibility = View.VISIBLE
                binding.tvReadingComponents.text = String.format(
                    Locale.US,
                    "X: %.1f  Y: %.1f  Z: %.1f",
                    reading.x, reading.y, reading.z
                )
            } else {
                binding.tvReadingComponents.visibility = View.GONE
            }

            binding.root.setOnClickListener { onItemClick(reading) }
            binding.btnEditReading.setOnClickListener { onRenameClick(reading) }
            binding.btnDeleteReading.setOnClickListener { onDeleteClick(reading) }
        }

        private fun formatLight(value: Double): String {
            return when {
                value < 10.0  -> String.format(Locale.US, "%.2f", value)
                value < 1000.0 -> String.format(Locale.US, "%.1f", value)
                else           -> String.format(Locale.US, "%,d", value.toLong())
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<SensorReading>() {
        override fun areItemsTheSame(oldItem: SensorReading, newItem: SensorReading): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SensorReading, newItem: SensorReading): Boolean =
            oldItem == newItem
    }
}
