package org.lineageos.launcher.model

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads and caches installed applications from PackageManager.
 */
class LauncherModel(private val context: Context) {

    suspend fun loadApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(
            mainIntent,
            PackageManager.MATCH_ALL
        )

        resolveInfos.mapNotNull { info ->
            val activityInfo = info.activityInfo ?: return@mapNotNull null
            val pkg = activityInfo.packageName
            val cls = activityInfo.name
            // Exclude our own launcher from the app drawer listing
            if (pkg == context.packageName && cls == "org.lineageos.launcher.ui.LauncherActivity") {
                return@mapNotNull null
            }
            val label = info.loadLabel(pm).toString()
            val icon = info.loadIcon(pm)
            AppInfo(label = label, packageName = pkg, className = cls, icon = icon)
        }.sortedBy { it.label.lowercase() }
    }
}
