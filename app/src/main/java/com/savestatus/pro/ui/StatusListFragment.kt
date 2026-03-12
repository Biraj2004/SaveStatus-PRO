package com.savestatus.pro.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.savestatus.pro.R
import kotlinx.coroutines.launch
import com.savestatus.pro.adapter.StatusAdapter
import com.savestatus.pro.model.StatusItem
import com.savestatus.pro.model.StatusType
import com.savestatus.pro.utils.AppToast
import com.savestatus.pro.utils.FileUtils
import java.io.File

class StatusListFragment : Fragment() {

    companion object {
        private const val ARG_TAB_TYPE = "tab_type"
        const val TAB_RECENT     = 0
        const val TAB_IMAGES     = 1
        const val TAB_VIDEOS     = 2
        const val TAB_DOWNLOADED = 3

        fun newInstance(tabType: Int) = StatusListFragment().apply {
            arguments = Bundle().apply { putInt(ARG_TAB_TYPE, tabType) }
        }
    }

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: StatusAdapter
    private var tabType: Int = TAB_RECENT

    // Download guard — prevents rapid re-taps while a coroutine is in flight
    private var isDownloading = false

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvEmptyIcon: TextView
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptyMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabType = arguments?.getInt(ARG_TAB_TYPE, TAB_RECENT) ?: TAB_RECENT
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_status_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        recyclerView       = view.findViewById(R.id.recyclerView)
        progressBar        = view.findViewById(R.id.progressBar)
        layoutEmpty        = view.findViewById(R.id.layoutEmpty)
        tvEmptyIcon        = view.findViewById(R.id.tvEmptyIcon)
        tvEmptyTitle       = view.findViewById(R.id.tvEmptyTitle)
        tvEmptyMessage     = view.findViewById(R.id.tvEmptyMessage)

        setupSwipeRefresh()
        setupRecyclerView()
        observeData()

        swipeRefreshLayout.setOnRefreshListener {
            if (tabType == TAB_DOWNLOADED) viewModel.loadDownloadedStatuses()
            else viewModel.loadStatuses()
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-evaluate download icons for all visible items (e.g. user deleted files externally,
        // or another tab triggered a download while this tab was in the background).
        if (!::adapter.isInitialized) return
        val count = adapter.itemCount
        if (count > 0) adapter.notifyItemRangeChanged(0, count, "download_state")
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.green_accent)
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.card_dark)
    }

    private fun setupRecyclerView() {
        adapter = StatusAdapter(
            onItemClick     = { item -> openViewer(item) },
            onDownloadClick = { item, position -> handleDownload(item, position) }
        )
        recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            this.adapter  = this@StatusListFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun observeData() {
        val liveData: LiveData<List<StatusItem>> = when (tabType) {
            TAB_IMAGES     -> viewModel.imageStatuses
            TAB_VIDEOS     -> viewModel.videoStatuses
            TAB_DOWNLOADED -> viewModel.downloadedStatuses
            else           -> viewModel.allStatuses
        }

        liveData.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            swipeRefreshLayout.isRefreshing = false
            updateEmptyState(items)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (tabType != TAB_DOWNLOADED) {
                progressBar.visibility =
                    if (isLoading && adapter.itemCount == 0) View.VISIBLE else View.GONE
                if (isLoading) swipeRefreshLayout.isRefreshing = false
            }
        }

        // downloadResult is handled per-fragment inside handleDownload() via lifecycleScope.
    }

    private fun handleDownload(item: StatusItem, position: Int) {
        val ctx = context ?: return
        if (isDownloading) return                                    // rapid-click guard
        // Real file check — no coroutine needed for an already-saved file
        if (FileUtils.isAlreadyDownloaded(File(item.filePath))) {
            AppToast.plain(ctx, getString(R.string.already_downloaded))
            adapter.notifyDownloadChanged(position)                  // ensure tick is shown
            return
        }
        isDownloading = true
        lifecycleScope.launch {
            val success = viewModel.downloadStatus(item)             // suspend, runs on IO
            isDownloading = false
            if (!isAdded) return@launch                              // fragment detached
            val context = context ?: return@launch
            if (success) {
                AppToast.plain(context, getString(R.string.download_success))
                adapter.notifyDownloadChanged(position)              // update only this row
            } else {
                AppToast.plain(context, getString(R.string.download_failed))
            }
        }
    }

    private fun updateEmptyState(items: List<StatusItem>) {
        val isEmpty = items.isEmpty()
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        progressBar.visibility  = View.GONE

        if (isEmpty) {
            layoutEmpty.visibility = View.VISIBLE
            when (tabType) {
                TAB_DOWNLOADED -> {
                    tvEmptyIcon.text    = "📥"
                    tvEmptyTitle.text   = getString(R.string.empty_title_downloads)
                    tvEmptyMessage.text = getString(R.string.no_downloads)
                }
                TAB_VIDEOS -> {
                    tvEmptyIcon.text    = "🎬"
                    tvEmptyTitle.text   = getString(R.string.empty_title_videos)
                    tvEmptyMessage.text = getString(R.string.empty_msg_videos)
                }
                TAB_IMAGES -> {
                    tvEmptyIcon.text    = "🖼️"
                    tvEmptyTitle.text   = getString(R.string.empty_title_images)
                    tvEmptyMessage.text = getString(R.string.empty_msg_images)
                }
                else -> {
                    tvEmptyIcon.text    = "📭"
                    tvEmptyTitle.text   = getString(R.string.empty_title_status)
                    tvEmptyMessage.text = getString(R.string.no_statuses_found)
                }
            }
            try {
                tvEmptyIcon.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.empty_state_enter))
                tvEmptyTitle.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.fade_slide_up))
                tvEmptyMessage.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.fade_slide_up))
            } catch (_: Exception) {}
        } else {
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun openViewer(item: StatusItem) {
        startActivity(
            (if (item.type == StatusType.VIDEO)
                Intent(requireContext(), VideoPlayerActivity::class.java)
            else
                Intent(requireContext(), ImageViewerActivity::class.java)
            ).apply { putExtra("file_path", item.filePath) }
        )
    }
}
