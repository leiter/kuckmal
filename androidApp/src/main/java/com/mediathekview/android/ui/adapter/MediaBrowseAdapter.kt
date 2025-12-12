package com.mediathekview.android.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mediathekview.android.R
import com.mediathekview.shared.database.MediaEntry

/**
 * RecyclerView adapter for browsing media entries by theme or title
 * Supports two item types with different display logic
 * Uses DiffUtil for smooth animated updates
 */
class MediaBrowseAdapter(
    private val onItemClick: (MediaEntryItem, Int) -> Unit
) : RecyclerView.Adapter<MediaBrowseAdapter.ViewHolder>() {


    /**
     * Sealed class representing different item types
     */
    sealed class MediaEntryItem {
        /**
         * Theme item - displays mediaEntry.theme
         */
        data class Theme(val mediaEntry: MediaEntry) : MediaEntryItem() {
            val theme: String get() = mediaEntry.theme
        }

        /**
         * Title item - displays mediaEntry.title
         */
        data class Title(val mediaEntry: MediaEntry) : MediaEntryItem() {
            val title: String get() = mediaEntry.title
        }

        /**
         * More item - displays pagination button
         */
        object More : MediaEntryItem()
    }

    private val items = mutableListOf<MediaEntryItem>()
    private var selectedPosition = RecyclerView.NO_POSITION

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textView)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is MediaEntryItem.Theme -> VIEW_TYPE_THEME
            is MediaEntryItem.Title -> VIEW_TYPE_TITLE
            is MediaEntryItem.More -> VIEW_TYPE_MORE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_MORE -> R.layout.item_more
            else -> R.layout.item_simple_text
        }
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // Set text based on item type (More items have text in layout)
        when (item) {
            is MediaEntryItem.Theme -> holder.textView.text = item.theme
            is MediaEntryItem.Title -> holder.textView.text = item.title
            is MediaEntryItem.More -> { /* Text is hardcoded in layout */ }
        }

        // Set selection state for visual feedback (not for More items)
        holder.itemView.isSelected = (position == selectedPosition) && item !is MediaEntryItem.More

        holder.itemView.setOnClickListener {
            // Get current position at click time (not captured position parameter)
            val clickPosition = holder.bindingAdapterPosition

            // Verify position is valid
            if (clickPosition == RecyclerView.NO_POSITION) {
                return@setOnClickListener
            }

            val clickedItem = items.getOrNull(clickPosition) ?: return@setOnClickListener

            // Update selection
            val previousPosition = selectedPosition
            selectedPosition = clickPosition

            // Notify adapter to refresh visual states
            if (previousPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousPosition)
            }
            notifyItemChanged(clickPosition)

            // Trigger callback
            onItemClick(clickedItem, clickPosition)
        }
    }

    override fun getItemCount(): Int = items.size

    /**
     * Update the list of items with smooth animations
     * Resets selection when new list is submitted
     */
    fun submitList(newItems: List<MediaEntryItem>) {
        val oldItems = items.toList()
        val diffCallback = MediaEntryItemDiffCallback(oldItems, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        items.clear()
        items.addAll(newItems)
        selectedPosition = RecyclerView.NO_POSITION

        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * Update the list and preserve selection based on item
     * Uses DiffUtil for smooth animations
     */
    fun submitListWithSelection(newItems: List<MediaEntryItem>, selectedItem: MediaEntryItem?) {
        val oldItems = items.toList()
        val diffCallback = MediaEntryItemDiffCallback(oldItems, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        // Track old selection BEFORE updating
        val oldSelectedPosition = selectedPosition

        items.clear()
        items.addAll(newItems)

        // Find and restore selection position based on item
        selectedPosition = if (selectedItem != null) {
            items.indexOfFirst { it == selectedItem }
        } else {
            RecyclerView.NO_POSITION
        }

        diffResult.dispatchUpdatesTo(this)

        // Manually notify selection state changes
        // DiffUtil only notifies content changes, not selection state changes
        // This ensures proper selection update even if item content didn't change
        if (oldSelectedPosition != selectedPosition) {
            // Deselect old position
            if (oldSelectedPosition != RecyclerView.NO_POSITION && oldSelectedPosition < items.size) {
                notifyItemChanged(oldSelectedPosition)
            }
            // Select new position
            if (selectedPosition != RecyclerView.NO_POSITION && selectedPosition < items.size) {
                notifyItemChanged(selectedPosition)
            }
        }
    }

    /**
     * Clear all items with animation
     */
    fun clear() {
        val oldSize = items.size
        items.clear()
        selectedPosition = RecyclerView.NO_POSITION
        notifyItemRangeRemoved(0, oldSize)
    }

    /**
     * Clear selection (useful when navigating away)
     */
    fun clearSelection() {
        val previousPosition = selectedPosition
        selectedPosition = RecyclerView.NO_POSITION
        if (previousPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousPosition)
        }
    }


    /**
     * DiffUtil callback for calculating differences between two MediaEntryItem lists
     */
    private class MediaEntryItemDiffCallback(
        private val oldList: List<MediaEntryItem>,
        private val newList: List<MediaEntryItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            // Items are same if they're the same type and have the same URL
            // URLs are unique identifiers for media entries
            return when {
                oldItem is MediaEntryItem.Theme && newItem is MediaEntryItem.Theme ->
                    oldItem.mediaEntry.url == newItem.mediaEntry.url
                oldItem is MediaEntryItem.Title && newItem is MediaEntryItem.Title ->
                    oldItem.mediaEntry.url == newItem.mediaEntry.url
                oldItem is MediaEntryItem.More && newItem is MediaEntryItem.More ->
                    true // More is a singleton, always the same item
                else -> false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            // Contents are same if the displayed text is the same
            return when {
                oldItem is MediaEntryItem.Theme && newItem is MediaEntryItem.Theme ->
                    oldItem.theme == newItem.theme
                oldItem is MediaEntryItem.Title && newItem is MediaEntryItem.Title ->
                    oldItem.title == newItem.title
                oldItem is MediaEntryItem.More && newItem is MediaEntryItem.More ->
                    true // More is a singleton, content never changes
                else -> false
            }
        }
    }
}

private const val VIEW_TYPE_THEME = 0
private const val VIEW_TYPE_TITLE = 1
private const val VIEW_TYPE_MORE = 2