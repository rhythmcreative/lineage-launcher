package org.lineageos.launcher.model

import android.content.Context
import androidx.preference.PreferenceManager

/**
 * Manages persistence of desktop workspace shortcuts and dock pages.
 */
class WorkspaceManager(private val context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun loadDesktopPages(allApps: List<AppInfo>): List<List<AppInfo>> {
        val appMap = allApps.associateBy { it.packageName }
        val rawConfig = prefs.getString(KEY_DESKTOP_PAGES, null)

        if (rawConfig != null && rawConfig.isNotEmpty()) {
            val pages = mutableListOf<List<AppInfo>>()
            val pageTokens = rawConfig.split(";")
            for (pageToken in pageTokens) {
                if (pageToken.isEmpty()) continue
                val pkgList = pageToken.split(",")
                val pageApps = pkgList.mapNotNull { appMap[it] }
                pages.add(pageApps)
            }
            if (pages.isNotEmpty()) return pages
        }

        // Default initial workspace setup (like Nova Launcher / Trebuchet):
        // First desktop page with popular apps
        val defaultDesktop = mutableListOf<AppInfo>()
        val defaultPackages = listOf(
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.google.android.dialer",
            "com.android.dialer",
            "com.android.chrome",
            "org.lineageos.jelly",
            "com.google.android.GoogleCamera",
            "com.android.camera2",
            "com.google.android.apps.photos",
            "com.android.gallery3d",
            "app.grapheneos.apps",
            "com.android.settings"
        )
        for (pkg in defaultPackages) {
            val app = appMap[pkg]
            if (app != null && !defaultDesktop.contains(app)) {
                defaultDesktop.add(app)
            }
        }
        if (defaultDesktop.isEmpty()) {
            defaultDesktop.addAll(allApps.take(8))
        }
        return listOf(defaultDesktop)
    }

    fun saveDesktopPages(pages: List<List<AppInfo>>) {
        val raw = pages.joinToString(";") { page ->
            page.joinToString(",") { it.packageName }
        }
        prefs.edit().putString(KEY_DESKTOP_PAGES, raw).apply()
    }

    fun loadDockPages(allApps: List<AppInfo>): List<List<AppInfo>> {
        val appMap = allApps.associateBy { it.packageName }
        val rawConfig = prefs.getString(KEY_DOCK_PAGES, null)

        if (rawConfig != null && rawConfig.isNotEmpty()) {
            val pages = mutableListOf<List<AppInfo>>()
            val pageTokens = rawConfig.split(";")
            for (pageToken in pageTokens) {
                if (pageToken.isEmpty()) continue
                val pkgList = pageToken.split(",")
                val pageApps = pkgList.mapNotNull { appMap[it] }
                pages.add(pageApps)
            }
            if (pages.isNotEmpty()) return pages
        }

        // Default initial dock: 5 apps on page 1, 5 apps on page 2 (multi-page dock)
        val dockPages = mutableListOf<List<AppInfo>>()
        val chunked = allApps.take(10).chunked(5)
        if (chunked.isNotEmpty()) {
            dockPages.addAll(chunked)
        } else {
            dockPages.add(emptyList())
        }
        return dockPages
    }

    fun saveDockPages(pages: List<List<AppInfo>>) {
        val raw = pages.joinToString(";") { page ->
            page.joinToString(",") { it.packageName }
        }
        prefs.edit().putString(KEY_DOCK_PAGES, raw).apply()
    }

    companion object {
        private const val KEY_DESKTOP_PAGES = "launcher_desktop_pages_data"
        private const val KEY_DOCK_PAGES = "launcher_dock_pages_data"
    }
}
