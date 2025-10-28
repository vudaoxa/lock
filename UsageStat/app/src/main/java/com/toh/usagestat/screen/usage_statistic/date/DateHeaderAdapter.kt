package com.toh.usagestat.screen.usage_statistic.date

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.toh.usagestat.databinding.ItemDateBinding
import java.text.SimpleDateFormat
import java.util.Calendar

class DateHeaderAdapter constructor(
    private val onDateClick: (Calendar) -> Unit
) : ListAdapter<DateHeaderItem, DateHeaderAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(private val binding: ItemDateBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DateHeaderItem, onClick: (Calendar) -> Unit) {
            binding.tvDayOfWeek.text = SimpleDateFormat("EEE").format(item.date.time).uppercase()
            binding.tvDay.text = item.date.get(Calendar.DAY_OF_MONTH).toString()
            binding.indicator.visibility = if (item.isSelected) View.VISIBLE else View.GONE
            binding.root.setOnClickListener { onClick(item.date) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onDateClick)
    }

    class DiffCallback : DiffUtil.ItemCallback<DateHeaderItem>() {
        override fun areItemsTheSame(old: DateHeaderItem, new: DateHeaderItem) =
            old.date.timeInMillis == new.date.timeInMillis
        override fun areContentsTheSame(old: DateHeaderItem, new: DateHeaderItem) = old == new
    }
}