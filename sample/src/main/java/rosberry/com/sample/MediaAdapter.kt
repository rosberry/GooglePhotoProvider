/*
 *
 *  * Copyright (c) 2020 Rosberry. All rights reserved.
 *
 */

package rosberry.com.sample

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rosberry.android.googlephotoprovider.CloudMedia

/**
 * @author Evgeniy Nagibin on 12/03/2020.
 */
class MediaAdapter: RecyclerView.Adapter<MediaViewHolder>() {

    private var mediaItems: List<CloudMedia> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        return MediaViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.i_media, parent, false))
    }

    override fun getItemCount(): Int {
        return mediaItems.size
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        //holder.bind(mediaItems[position])
    }
}