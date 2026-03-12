package com.savestatus.pro.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.savestatus.pro.R
import com.savestatus.pro.model.StatusItem
import com.savestatus.pro.model.StatusType
import com.savestatus.pro.utils.FileUtils
import java.io.File

class StatusAdapter(
    private val onItemClick: (StatusItem) -> Unit,
    private val onDownloadClick: (StatusItem, Int) -> Unit
) : ListAdapter<StatusItem, StatusAdapter.StatusViewHolder>(DiffCallback()) {

    init { setHasStableIds(true) }

    override fun getItemId(position: Int): Long = getItem(position).id.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_status, parent, false)
        return StatusViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StatusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivThumbnail: ImageView   = itemView.findViewById(R.id.ivThumbnail)
        private val layoutVideoBadge: LinearLayout = itemView.findViewById(R.id.layoutVideoBadge)
        private val tvDuration: TextView     = itemView.findViewById(R.id.tvDuration)
        private val ivDownload: ImageView    = itemView.findViewById(R.id.ivDownload)

        fun bind(item: StatusItem) {
            // Thumbnail
            Glide.with(itemView.context)
                .load(File(item.filePath))
                .apply(
                    RequestOptions()
                        .transform(RoundedCorners(24))
                        .placeholder(R.color.card_dark_elevated)
                        .error(R.color.card_dark_elevated)
                )
                .into(ivThumbnail)

            // Video badge
            if (item.type == StatusType.VIDEO) {
                layoutVideoBadge.visibility = View.VISIBLE
                tvDuration.text = FileUtils.formatDuration(item.duration)
            } else {
                layoutVideoBadge.visibility = View.GONE
            }

            // Download icon — always check real file existence, never rely on flags
            refreshDownloadIcon(item, ivDownload)

            itemView.setOnClickListener { onItemClick(item) }
            ivDownload.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_ID.toInt()) {
                    onDownloadClick(item, pos)
                }
            }
        }

        /** Update only the download icon of this single ViewHolder without rebinding everything. */
        fun refreshDownloadIcon(item: StatusItem) {
            val iv = itemView.findViewById<ImageView>(R.id.ivDownload)
            refreshDownloadIcon(item, iv)
        }

        private fun refreshDownloadIcon(item: StatusItem, iv: ImageView) {
            val downloaded = FileUtils.isAlreadyDownloaded(File(item.filePath))
            iv.setImageResource(if (downloaded) R.drawable.ic_check else R.drawable.ic_download)
        }
    }

    /** Refresh only the download icon of a single item by position (no full rebind). */
    fun notifyDownloadChanged(position: Int) {
        notifyItemChanged(position, PAYLOAD_DOWNLOAD_STATE)
    }

    override fun onBindViewHolder(
        holder: StatusViewHolder, position: Int, payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_DOWNLOAD_STATE)) {
            // Lightweight partial update: only refresh the download icon
            holder.refreshDownloadIcon(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<StatusItem>() {
        override fun areItemsTheSame(old: StatusItem, new: StatusItem) = old.id == new.id
        override fun areContentsTheSame(old: StatusItem, new: StatusItem) = old == new
    }

    companion object {
        private const val PAYLOAD_DOWNLOAD_STATE = "download_state"
    }
}

