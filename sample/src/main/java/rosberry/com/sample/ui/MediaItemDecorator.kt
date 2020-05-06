/*
 *
 *  * Copyright (c) 2020 Rosberry. All rights reserved.
 *
 */

package rosberry.com.sample.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * @author Evgeniy Nagibin on 29/04/2020.
 */
class MediaItemDecorator(
        private val spacing: Int,
        private val spanCount: Int
) : RecyclerView.ItemDecoration() {

    private var needLeftSpacing = false

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val frameWidth = ((parent.width - spacing * (spanCount - 1)) / spanCount)
        val padding = parent.width / spanCount - frameWidth
        val itemPosition = (view.layoutParams as RecyclerView.LayoutParams).viewAdapterPosition

        outRect.top = spacing / 2

        if (itemPosition % spanCount == 0) {
            outRect.left = 0
            outRect.right = padding
            needLeftSpacing = true
        } else if ((itemPosition + 1) % spanCount == 0) {
            needLeftSpacing = false
            outRect.right = 0
            outRect.left = padding
        } else if (needLeftSpacing) {
            needLeftSpacing = true
            outRect.left = spacing - padding
            if ((itemPosition + 2) % spanCount == 0) {
                outRect.right = spacing - padding
            } else {
                outRect.right = spacing / 2
            }
        } else if ((itemPosition + 2) % spanCount == 0) {
            needLeftSpacing = false
            outRect.left = spacing - padding
            outRect.right = spacing / 2
        } else {
            needLeftSpacing = false
            outRect.left = spacing / 2
            outRect.right = spacing / 2
        }
        outRect.bottom = 0
    }
}