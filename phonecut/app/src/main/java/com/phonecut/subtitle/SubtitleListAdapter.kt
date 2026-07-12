package com.phonecut.subtitle

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.phonecut.app.databinding.ItemSubtitleBinding

class SubtitleListAdapter(
    private val subtitles: List<SubtitleItem>
) : RecyclerView.Adapter<SubtitleListAdapter.SubtitleViewHolder>() {

    var onItemClick: ((SubtitleItem) -> Unit)? = null
    var onItemDelete: ((SubtitleItem) -> Unit)? = null

    inner class SubtitleViewHolder(
        private val binding: ItemSubtitleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(subtitle: SubtitleItem) {
            binding.tvSubtitleText.text = subtitle.text
            binding.tvTimeRange.text = formatTime(subtitle.startTimeMs) + " → " + formatTime(subtitle.endTimeMs)
            
            val typeText = when (subtitle.type) {
                SubtitleType.STATIC -> "静态"
                SubtitleType.SCROLL_VERTICAL -> "上下滚动"
                SubtitleType.SCROLL_HORIZONTAL -> "左右滚动"
            }
            binding.tvSubtitleType.text = typeText

            binding.root.setOnClickListener {
                onItemClick?.invoke(subtitle)
            }

            binding.btnDelete.setOnClickListener {
                onItemDelete?.invoke(subtitle)
            }
        }

        private fun formatTime(ms: Long): String {
            val seconds = (ms / 1000) % 60
            val minutes = (ms / (1000 * 60)) % 60
            val hours = ms / (1000 * 60 * 60)
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubtitleViewHolder {
        val binding = ItemSubtitleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SubtitleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubtitleViewHolder, position: Int) {
        holder.bind(subtitles[position])
    }

    override fun getItemCount(): Int = subtitles.size
}
