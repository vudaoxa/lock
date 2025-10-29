package com.toh.usagestat.screen.usage_statistic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.toh.usagestat.R
import com.toh.usagestat.databinding.ItemAppUsageBinding
import com.toh.usagestat.util.formatDuration

class AppUsageAdapter(
    private val onAppClick: (String) -> Unit
) : ListAdapter<AppUsageData, AppUsageAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(private val binding: ItemAppUsageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AppUsageData, onClick: (String) -> Unit) {
            binding.appIcon.setImageDrawable(item.appIcon)
            binding.appName.text = item.appName
            binding.timeUsed.text = formatDuration(item.timeUsed)
            binding.percentage.text = "${item.percentage.toInt()}%"
            binding.moreThanYesterdayContainer.visibility = if (item.moreThanYesterday) {
                binding.moreThanYesterdayText.text =
                    "${formatDuration(item.diffWithYesterday)} more than yesterday"
                View.VISIBLE
            } else View.GONE
            binding.root.setOnClickListener { onClick(item.packageName) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemAppUsageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onAppClick)
    }

    class DiffCallback : DiffUtil.ItemCallback<AppUsageData>() {
        override fun areItemsTheSame(old: AppUsageData, new: AppUsageData) =
            old.packageName == new.packageName

        override fun areContentsTheSame(old: AppUsageData, new: AppUsageData) = old == new
    }
}