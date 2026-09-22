package org.lineageos.launcher.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.launcher.R
import org.lineageos.launcher.model.AppInfo

/**
 * Adapter for the All Apps Drawer grid.
 */
class AppDrawerAdapter(
    private val context: Context,
    private val onAppClicked: (AppInfo) -> Unit,
    private val onAppLongClicked: (AppInfo, View) -> Unit
) : RecyclerView.Adapter<AppDrawerAdapter.AppViewHolder>() {

    private val allApps = mutableListOf<AppInfo>()
    private val filteredApps = mutableListOf<AppInfo>()

    fun setApps(apps: List<AppInfo>) {
        allApps.clear()
        allApps.addAll(apps)
        filteredApps.clear()
        filteredApps.addAll(apps)
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val q = query.trim().lowercase()
        filteredApps.clear()
        if (q.isEmpty()) {
            filteredApps.addAll(allApps)
        } else {
            filteredApps.addAll(allApps.filter { it.label.lowercase().contains(q) })
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_app_icon, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = filteredApps[position]
        holder.iconView.setImageDrawable(app.icon)
        holder.labelView.text = app.label
        holder.labelView.visibility = View.VISIBLE

        holder.itemView.setOnClickListener { onAppClicked(app) }
        holder.itemView.setOnLongClickListener { v ->
            onAppLongClicked(app, v)
            true
        }
    }

    override fun getItemCount(): Int = filteredApps.size

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconView: ImageView = itemView.findViewById(R.id.app_icon)
        val labelView: TextView = itemView.findViewById(R.id.app_label)
    }
}
