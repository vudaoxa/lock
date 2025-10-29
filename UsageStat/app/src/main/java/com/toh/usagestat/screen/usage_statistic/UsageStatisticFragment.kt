package com.toh.usagestat.screen.usage_statistic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
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
    private lateinit var adapter: AppUsageAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsageStatisticBinding.inflate(inflater, container, false)

        setupInlineDatePicker()
        setupRecyclerView()
        observeViewModel()
        setupScrollListener()
        return binding.root
    }

    private lateinit var dateHeaderAdapter: DateHeaderAdapter
    private fun setupInlineDatePicker() {
        dateHeaderAdapter = DateHeaderAdapter(viewModel::selectDate)

        binding.rvDateHeader.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = dateHeaderAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.tvDate.text = state.date
            binding.tvTotalTime.text = "Total usage: ${state.totalTime}"
            binding.tvCompare.text = state.compareText
            adapter.submitList(state.appList)
        }

        viewModel.dateList.observe(viewLifecycleOwner) { dates ->
            dateHeaderAdapter.submitList(dates)
        }
    }

    private fun setupScrollListener() {
        binding.rvDateHeader.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!recyclerView.canScrollHorizontally(-1) && dx < 0) {
                    viewModel.loadPreviousWeek()
                }
                if (!recyclerView.canScrollHorizontally(1) && dx > 0) {
                    viewModel.loadNextWeek()
                }
            }
        })
    }

    private fun setupRecyclerView() {
        adapter = AppUsageAdapter { packageName ->
            //(activity as? MainActivity)?.openAppDetail(packageName)
            // TODO:
            findNavController().navigate(
                R.id.action_usageStatistic_to_appDetail,
                bundleOf("packageName" to packageName)
            )
        }
        binding.rvApps.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@UsageStatisticFragment.adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}