package com.toh.usagestat.screen.usage_statistic

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.toh.usagestat.R
import com.toh.usagestat.databinding.FragmentUsageStatisticBinding
import com.toh.usagestat.screen.usage_statistic.adapter.AppUsageAdapter
import com.toh.usagestat.screen.usage_statistic.date.DateHeaderAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UsageStatisticFragment : Fragment() {
    private var _binding: FragmentUsageStatisticBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UsageStatisticViewModel by viewModels()
    private lateinit var appUsageAdapter: AppUsageAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsageStatisticBinding.inflate(inflater, container, false)
        setupInlineDatePicker()
        setupRecyclerView()
        observeViewModel()
        setupScrollListener()
        setupDateSeek()
        checkUsageStatsPermission()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (hasUsageStatsPermission()) {
            viewModel.loadInitialData()
        }
    }

    private fun checkUsageStatsPermission() {
        if (!hasUsageStatsPermission()) {
            showPermissionRequiredState()
        } else {
            viewModel.loadInitialData()
        }
        viewModel.loadInitialDate()
    }

    private var granted = false
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            requireContext().packageName
        )
        granted = mode == AppOpsManager.MODE_ALLOWED
        return granted
    }

    private fun showPermissionRequiredState() {
        binding.emptyStateContainer.visibility = View.VISIBLE
        binding.rvApps.visibility = View.GONE
        binding.rvDateHeader.visibility = View.GONE
        binding.tvDate.visibility = View.GONE
        binding.tvTotalTime.visibility = View.GONE
        binding.tvCompare.visibility = View.GONE
        binding.btnGrantPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            })
        }
    }

    private lateinit var dateHeaderAdapter: DateHeaderAdapter
    private fun setupInlineDatePicker() {
        dateHeaderAdapter = DateHeaderAdapter(viewModel::selectDate)
        binding.rvDateHeader.apply {
            adapter = dateHeaderAdapter
        }
    }

    private var hasData = false
    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state.appList.isEmpty()) {
                binding.emptyStateContainer.visibility = View.VISIBLE
                binding.rvApps.visibility = View.GONE
                if (granted) {
                    binding.btnGrantPermission.visibility = View.GONE
                    if (!hasData) {
                        binding.tvEmptyMessage.text = "No usage data yet. Try using some apps!"
                    } else binding.emptyStateContainer.visibility = View.GONE
                }
            } else {
                hasData = true
                binding.emptyStateContainer.visibility = View.GONE
                binding.rvApps.visibility = View.VISIBLE
                appUsageAdapter.submitList(state.appList)
            }

            binding.tvDate.text = state.date
            //smoothScrollToPosition()
            binding.tvTotalTime.text = "Total usage: ${state.totalTime}"
            binding.tvCompare.text = state.compareText
        }

        viewModel.dateList.observe(viewLifecycleOwner) { dates ->
            dateHeaderAdapter.submitList(dates)
            binding.rvDateHeader.visibility = View.VISIBLE
            binding.tvDate.visibility = View.VISIBLE
            binding.tvTotalTime.visibility = View.VISIBLE
            binding.tvCompare.visibility = View.VISIBLE
        }
    }

    //private fun smoothScrollToPosition() {
    //    binding.rvDateHeader.post {
    //        val target = viewModel.dateList.value.map { it.date }.indexOf(viewModel.selectedDate)
    //        Log.d("TAG", "smoothScrollToPosition: $target")
    //        if (target >= 0)
    //            binding.rvDateHeader.smoothScrollToPosition(target)
    //    }
    //}

    private fun setupDateSeek() {
        binding.btnPrevWeek.setOnClickListener { viewModel.selectDateOffset(-1) }
        binding.btnPrevWeek.setOnLongClickListener { viewModel.selectFirstDate() }
        binding.btnNextWeek.setOnClickListener { viewModel.selectDateOffset(1) }
        binding.btnNextWeek.setOnLongClickListener { viewModel.selectLastDate() }
    }

    private fun setupScrollListener() {
        binding.rvDateHeader.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!recyclerView.canScrollHorizontally(-1) && dx < 0) {
                    viewModel.loadPreviousWeek()
                }
                //if (!recyclerView.canScrollHorizontally(1) && dx > 0) {
                //    viewModel.loadNextWeek()
                //}
            }
        })
    }

    private fun setupRecyclerView() {
        appUsageAdapter = AppUsageAdapter { packageName ->
            findNavController().navigate(
                R.id.action_usageStatistic_to_appDetail,
                bundleOf("packageName" to packageName)
            )
        }
        binding.rvApps.apply {
            adapter = this@UsageStatisticFragment.appUsageAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}