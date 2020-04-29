/*
 *
 *  * Copyright (c) 2020 Rosberry. All rights reserved.
 *
 */

package rosberry.com.sample.ui

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import kotlinx.android.synthetic.main.i_media.view.*
import rosberry.com.sample.entity.Media

/**
 * @author Evgeniy Nagibin on 12/03/2020.
 */
class MediaViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    fun bind(media: Media) {
        Glide.with(itemView.context)
            .load(media.previewUri)
            .centerCrop()
            .transition(withCrossFade(DrawableCrossFadeFactory.Builder()
                .setCrossFadeEnabled(true)
                .build()))
            .into(itemView.mediaThumbnail)
    }
}