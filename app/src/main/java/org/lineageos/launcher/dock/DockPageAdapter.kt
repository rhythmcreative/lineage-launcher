package org.lineageos.launcher.dock

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.launcher.R
import org.lineageos.launcher.model.AppInfo

/**
 * Adapter for the multi-page scrollable dock (ViewPager2).
 * Each dock page can hold up to 5 items.
 */
class DockPageAdapter(
    private val context: Context,
    private val onAppClicked: (AppInfo) -> Unit,
    private val onAppLongClicked: (AppInfo, View) -> Unit
) : RecyclerView.Adapter<DockPageAdapter.DockPageViewHolder>() {

    private val pages = mutableListOf<MutableList<AppInfo>>()

    fun setDockPages(newPages: List<List<AppInfo>>) {
        pages.clear()
        for (page in newPages) {
            pages.add(page.toMutableList())
        }
        if (pages.isEmpty()) {
            pages.add(mutableListOf())
        }
        notifyDataSetChanged()
    }

    fun getPageCount(): Int = pages.size

    fun addEmptyPage(): Int {
        pages.add(mutableListOf())
        val newIndex = pages.size - 1
        notifyItemInserted(newIndex)
        return newIndex
    }

    fun addItemToPage(pageIndex: Int, app: AppInfo): Boolean {
        if (pageIndex !in 0 until pages.size) return false
        val page = pages[pageIndex]
        if (page.size >= 5) return false
        page.add(app)
        notifyItemChanged(pageIndex)
        return true
    }

    fun addAppToCurrentPage(pageIndex: Int, app: AppInfo): Boolean {
        if (pageIndex in 0 until pages.size && pages[pageIndex].size < 5) {
            pages[pageIndex].add(app)
            notifyItemChanged(pageIndex)
            return true
        }
        val newPage = addEmptyPage()
        pages[newPage].add(app)
        notifyItemChanged(newPage)
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DockPageViewHolder {
        val layout = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.HORIZONTAL
            weightSum = 5f
        }
        return DockPageViewHolder(layout)
    }

    override fun onBindViewHolder(holder: DockPageViewHolder, position: Int) {
        val container = holder.itemView as LinearLayout
        container.removeAllViews()

        val items = pages.getOrElse(position) { mutableListOf() }
        val inflater = LayoutInflater.from(context)

        for (i in 0 until 5) {
            val cellView = inflater.inflate(R.layout.item_app_icon, container, false).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val iconView = cellView.findViewById<ImageView>(R.id.app_icon)
            val labelView = cellView.findViewById<TextView>(R.id.app_label)
            // Dock icons hide textual labels for a clean, minimal look
            labelView.visibility = View.GONE

            if (i < items.size) {
                val app = items[i]
                iconView.setImageDrawable(app.icon)
                cellView.contentDescription = app.label
                cellView.setOnClickListener { onAppClicked(app) }
                cellView.setOnLongClickListener { v ->
                    onAppLongClicked(app, v)
                    true
                }
            } else {
                iconView.setImageDrawable(null)
                cellView.isClickable = false
            }

            container.addView(cellView)
        }
    }

    override fun getItemCount(): Int = pages.size

    class DockPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
