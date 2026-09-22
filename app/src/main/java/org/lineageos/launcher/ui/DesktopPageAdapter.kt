package org.lineageos.launcher.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.launcher.R
import org.lineageos.launcher.model.AppInfo

/**
 * Adapter for the multi-page desktop workspace (ViewPager2) matching Nova Launcher and Trebuchet.
 * Each desktop page contains a grid of app shortcuts with persistent positioning.
 */
class DesktopPageAdapter(
    private val context: Context,
    private val onAppClicked: (AppInfo) -> Unit,
    private val onAppLongClicked: (AppInfo, View) -> Unit,
    private val onEmptySpaceLongClicked: () -> Unit
) : RecyclerView.Adapter<DesktopPageAdapter.DesktopPageViewHolder>() {

    private val pages = mutableListOf<MutableList<AppInfo>>()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private var showLabels = prefs.getBoolean("pref_desktop_show_labels", true)

    fun refreshPreferences() {
        showLabels = prefs.getBoolean("pref_desktop_show_labels", true)
        notifyDataSetChanged()
    }

    fun setPages(newPages: List<List<AppInfo>>) {
        pages.clear()
        for (page in newPages) {
            pages.add(page.toMutableList())
        }
        if (pages.isEmpty()) {
            pages.add(mutableListOf())
        }
        notifyDataSetChanged()
    }

    fun getPages(): List<List<AppInfo>> = pages

    fun addAppToPage(pageIndex: Int, app: AppInfo): Boolean {
        val targetIndex = if (pageIndex in 0 until pages.size) pageIndex else 0
        val page = pages[targetIndex]
        if (!page.any { it.packageName == app.packageName }) {
            page.add(app)
            notifyItemChanged(targetIndex)
            return true
        }
        return false
    }

    fun removeAppFromPage(pageIndex: Int, app: AppInfo): Boolean {
        if (pageIndex in 0 until pages.size) {
            val removed = pages[pageIndex].removeAll { it.packageName == app.packageName }
            if (removed) {
                notifyItemChanged(pageIndex)
                return true
            }
        }
        return false
    }

    fun addEmptyPage(): Int {
        pages.add(mutableListOf())
        val newIndex = pages.size - 1
        notifyItemInserted(newIndex)
        return newIndex
    }

    override fun getItemCount(): Int = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DesktopPageViewHolder {
        val recyclerView = RecyclerView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val columns = when (prefs.getString("pref_grid_size", "4x5")) {
                "5x5" -> 5
                else -> 4
            }
            layoutManager = GridLayoutManager(context, columns)
            clipToPadding = false
            setPadding(16, 16, 16, 16)
        }
        return DesktopPageViewHolder(recyclerView)
    }

    override fun onBindViewHolder(holder: DesktopPageViewHolder, position: Int) {
        val apps = pages[position]
        val adapter = DesktopGridAdapter(
            context = context,
            apps = apps,
            showLabels = showLabels,
            onAppClicked = onAppClicked,
            onAppLongClicked = onAppLongClicked,
            onEmptySpaceLongClicked = onEmptySpaceLongClicked
        )
        holder.recyclerView.adapter = adapter
    }

    class DesktopPageViewHolder(val recyclerView: RecyclerView) : RecyclerView.ViewHolder(recyclerView)

    private class DesktopGridAdapter(
        private val context: Context,
        private val apps: List<AppInfo>,
        private val showLabels: Boolean,
        private val onAppClicked: (AppInfo) -> Unit,
        private val onAppLongClicked: (AppInfo, View) -> Unit,
        private val onEmptySpaceLongClicked: () -> Unit
    ) : RecyclerView.Adapter<DesktopGridAdapter.AppViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_app_icon, parent, false)
            return AppViewHolder(view)
        }

        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            val app = apps[position]
            holder.iconView.setImageDrawable(app.icon)

            if (showLabels) {
                holder.labelView.text = app.label
                holder.labelView.visibility = View.VISIBLE
            } else {
                holder.labelView.visibility = View.GONE
            }

            holder.itemView.setOnClickListener { onAppClicked(app) }
            holder.itemView.setOnLongClickListener { v ->
                onAppLongClicked(app, v)
                true
            }
        }

        override fun getItemCount(): Int = apps.size

        class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val iconView: ImageView = itemView.findViewById(R.id.app_icon)
            val labelView: TextView = itemView.findViewById(R.id.app_label)
        }
    }
}
