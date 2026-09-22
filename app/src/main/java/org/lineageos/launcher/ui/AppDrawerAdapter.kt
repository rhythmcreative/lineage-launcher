package org.lineageos.launcher.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.launcher.R
import org.lineageos.launcher.model.AppInfo

/**
 * Modern AppDrawerAdapter supporting search generation filtering (as in GrapheneOS Launcher3),
 * profile tabs, and label visibility preference.
 */
class AppDrawerAdapter(
    private val context: Context,
    private val onAppClicked: (AppInfo) -> Unit,
    private val onAppLongClicked: (AppInfo, View) -> Unit
) : RecyclerView.Adapter<AppDrawerAdapter.AppViewHolder>() {

    private val allApps = mutableListOf<AppInfo>()
    private val currentList = mutableListOf<AppInfo>()
    private var filterGeneration = 0
    private var activeFilterType = FILTER_ALL

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private var showLabels = prefs.getBoolean("pref_drawer_show_labels", true)

    fun refreshPreferences() {
        showLabels = prefs.getBoolean("pref_drawer_show_labels", true)
        notifyDataSetChanged()
    }

    fun setApps(apps: List<AppInfo>) {
        allApps.clear()
        allApps.addAll(apps)
        applyFilters("", activeFilterType)
    }

    fun setFilterType(filterType: Int) {
        if (activeFilterType != filterType) {
            activeFilterType = filterType
            applyFilters("", activeFilterType)
        }
    }

    /**
     * GrapheneOS pattern: Use search generation counter to cancel/ignore stale search results.
     */
    fun filter(query: String) {
        applyFilters(query, activeFilterType)
    }

    private fun applyFilters(query: String, filterType: Int) {
        val currentGen = ++filterGeneration
        val q = query.trim().lowercase()

        val filtered = allApps.filter { app ->
            val matchesProfile = when (filterType) {
                FILTER_WORK -> app.isWorkProfile
                FILTER_PRIVATE -> app.isPrivateSpace
                FILTER_PERSONAL -> !app.isWorkProfile && !app.isPrivateSpace
                else -> true
            }

            val matchesQuery = if (q.isEmpty()) true else app.label.lowercase().contains(q)
            matchesProfile && matchesQuery
        }

        if (currentGen == filterGeneration) {
            currentList.clear()
            currentList.addAll(filtered)
            notifyDataSetChanged()
        }
    }

    fun getDisplayedApps(): List<AppInfo> = currentList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_app_icon, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = currentList[position]
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

    override fun getItemCount(): Int = currentList.size

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconView: ImageView = itemView.findViewById(R.id.app_icon)
        val labelView: TextView = itemView.findViewById(R.id.app_label)
    }

    companion object {
        const val FILTER_ALL = 0
        const val FILTER_PERSONAL = 1
        const val FILTER_WORK = 2
        const val FILTER_PRIVATE = 3
    }
}
