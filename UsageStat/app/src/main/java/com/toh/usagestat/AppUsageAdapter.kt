package com.toh.usagestat

import android.util.Log
import android.view.animation.Animation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppUsageAdapter(
    private var data: List<AppUsageData>,
    private val onSetLimit: (String, Long) -> Unit
) : RecyclerView.Adapter<AppUsageAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.appIcon)
        val appName: TextView = itemView.findViewById(R.id.appName)
        val timeUsed: TextView = itemView.findViewById(R.id.timeUsed)
        val numberPicker: NumberPicker = itemView.findViewById(R.id.numberPicker)
        val setLimitButton: Button = itemView.findViewById(R.id.setLimitButton)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        val toggleSwitch: Switch = itemView.findViewById(R.id.toggleSwitch)
        val moreThanYesterdayText: TextView = itemView.findViewById(R.id.moreThanYesterdayText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_usage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.appName.text = item.appName
        holder.timeUsed.text = "Time: ${formatDuration(item.timeUsed)} (${item.percentage}%)"
        if (item.icon != null) {
            holder.icon.setImageDrawable(item.icon as android.graphics.drawable.Drawable)
        }
        holder.numberPicker.minValue = 0
        holder.numberPicker.maxValue = 120
        holder.numberPicker.value = 0
        holder.setLimitButton.setOnClickListener {
            val animation = AlphaAnimation(1.0f, 0.5f).apply {
                duration = 200
                repeatCount = 1
                repeatMode = Animation.REVERSE
            }
            holder.setLimitButton.startAnimation(animation)
            val minutes = holder.numberPicker.value.toLong()
            if (minutes > 0) {
                onSetLimit(item.packageName, minutes * 60 * 1000)
                holder.progressBar.max = 100
                holder.progressBar.progress = (item.timeUsed * 100 / (minutes * 60 * 1000)).toInt().coerceAtMost(100)
            }
        }
        holder.toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.d("AppLock2", "Toggle for ${item.appName}: $isChecked")
        }
        holder.progressBar.progress = (item.timeUsed * 100 / (60 * 1000)).toInt().coerceAtMost(100) // Giả sử limit mặc định 1 giờ

        // Hiển thị "more than yesterday" cho 3 items hàng đầu
        if (item.isTop3 && item.isMoreThanYesterday) {
            holder.moreThanYesterdayText.visibility = View.VISIBLE
        } else {
            holder.moreThanYesterdayText.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = data.size

    fun updateData(newData: List<AppUsageData>) {
        data = newData
        notifyDataSetChanged()
    }
}