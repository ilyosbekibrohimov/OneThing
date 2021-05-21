package com.weeknday.onething

import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

object RecycleUtils {
    @JvmStatic
    fun recursiveRecycle(root: View?) {
        if (root == null) return
        root.setBackgroundDrawable(null)
        if (root is ViewGroup) {
            val group = root
            val count = group.childCount
            for (i in 0 until count) {
                if (root !is SwipeRefreshLayout) recursiveRecycle(group.getChildAt(i))
            }
            if (root !is AdapterView<*>) {
                group.removeAllViews()
            }
        }
        if (root is ImageView) {
            root.setImageDrawable(null)
        }
    }
}