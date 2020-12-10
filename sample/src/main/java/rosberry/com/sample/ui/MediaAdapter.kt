/*
 *
 *  * Copyright (c) 2020 Rosberry. All rights reserved.
 *
 */

package rosberry.com.sample.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import rosberry.com.sample.R
import rosberry.com.sample.entity.Media

/**
 * @author Evgeniy Nagibin on 12/03/2020.
 */
class MediaAdapter(
        private val itemSize: Int,
        private val click: (Media) -> Unit
) : RecyclerView.Adapter<MediaViewHolder>() {

    private var items: List<Media> = listOf()

    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.i_media, parent, false)
        val params = view.layoutParams
        params.width = itemSize
        params.height = itemSize
        return MediaViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val media = items[position]
        holder.bind(media)
        holder.itemView.setOnLongClickListener {
            click.invoke(media)
            true
        }
    }

    fun showData(newItems: List<Media>) {
        DiffUtil.calculateDiff(
                object : DiffUtil.Callback() {
                    override fun getOldListSize() = items.size
                    override fun getNewListSize() = newItems.size

                    override fun areItemsTheSame(
                            oldItemPosition: Int,
                            newItemPosition: Int
                    ) =
                            items[oldItemPosition].id ==
                                    newItems[newItemPosition].id

                    override fun areContentsTheSame(
                            oldItemPosition: Int,
                            newItemPosition: Int
                    ) = true
                }
        )
            .also { diffResult ->
                items = newItems
                diffResult.dispatchUpdatesTo(this)
            }
    }
}