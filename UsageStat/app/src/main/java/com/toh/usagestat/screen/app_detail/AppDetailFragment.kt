package com.toh.usagestat.screen.app_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.toh.usagestat.databinding.FragmentAppDetailBinding
import com.toh.usagestat.util.formatDuration
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppDetailFragment : Fragment() {

    private var _binding: FragmentAppDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppDetailViewModel by viewModels()
    private var packageName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        packageName = arguments?.getString("packageName") ?: return
        // Load dữ liệu
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppDetailBinding.inflate(inflater, container, false)
        setupUI()
        observeViewModel()
        return binding.root
    }

    private fun setupUI() {
        // Icon + tên app
        //val packageName = viewModel.packageName
        try {
            val appInfo = requireContext().packageManager.getApplicationInfo(packageName, 0)
            val icon = requireContext().packageManager.getApplicationIcon(appInfo)
            val name = requireContext().packageManager.getApplicationLabel(appInfo).toString()

            binding.appIcon.setImageDrawable(icon)
            binding.appName.text = name
        } catch (e: Exception) {
            binding.appName.text = "Unknown App"
        }
    }

    private fun observeViewModel() {
        viewModel.appInfo.observe(viewLifecycleOwner) { info ->
            binding.tvTodayUsage.text = formatDuration(info.todayUsage)
            binding.tvSessions.text = info.sessionCount.toString()
            binding.tvStreak.text = "${info.streak} day${if (info.streak > 1) "s" else ""}"
            binding.tvLongest.text = formatDuration(info.longestSession)
            binding.tvInstallDate.text = info.installDate
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}