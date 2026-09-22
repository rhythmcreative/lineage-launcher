package org.lineageos.launcher.model

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Modern LauncherModel leveraging Android LauncherApps APIs (as implemented in GrapheneOS and LineageOS Trebuchet).
 * Fully supports multi-user profiles (Personal, Work, and Android 15/16/17 Private Space)
 * and registers real-time callbacks for package additions, updates, removals, and suspensions.
 */
class LauncherModel(
    private val context: Context,
    private val onAppsChanged: (() -> Unit)? = null
) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) {
            mainHandler.post { onAppsChanged?.invoke() }
        }

        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            mainHandler.post { onAppsChanged?.invoke() }
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            mainHandler.post { onAppsChanged?.invoke() }
        }

        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            mainHandler.post { onAppsChanged?.invoke() }
        }

        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            mainHandler.post { onAppsChanged?.invoke() }
        }

        override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) {
            mainHandler.post { onAppsChanged?.invoke() }
        }

        override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) {
            mainHandler.post { onAppsChanged?.invoke() }
        }
    }

    init {
        try {
            launcherApps.registerCallback(callback, mainHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register LauncherApps callback", e)
        }
    }

    fun unregister() {
        try {
            launcherApps.unregisterCallback(callback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister LauncherApps callback", e)
        }
    }

    /**
     * Loads all launchable activities across all accessible profiles.
     */
    suspend fun loadApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val apps = mutableListOf<AppInfo>()
        val density = context.resources.displayMetrics.densityDpi
        val profiles = try {
            launcherApps.profiles
        } catch (e: Exception) {
            listOf(Process.myUserHandle())
        }

        for (profile in profiles) {
            val isWork = isManagedProfile(profile)
            val isPrivate = isPrivateSpace(profile)

            val activityList: List<LauncherActivityInfo> = try {
                launcherApps.getActivityList(null, profile)
            } catch (e: Exception) {
                emptyList()
            }

            for (info in activityList) {
                val componentName = info.componentName
                val pkg = componentName.packageName
                val cls = componentName.className

                // Skip this launcher activity from drawer
                if (pkg == context.packageName && cls == "org.lineageos.launcher.ui.LauncherActivity") {
                    continue
                }

                val label = try {
                    info.label.toString()
                } catch (e: Exception) {
                    pkg
                }

                val icon = try {
                    info.getBadgedIcon(density)
                } catch (e: Exception) {
                    null
                }

                apps.add(
                    AppInfo(
                        label = label,
                        packageName = pkg,
                        className = cls,
                        icon = icon,
                        userHandle = profile,
                        isWorkProfile = isWork,
                        isPrivateSpace = isPrivate
                    )
                )
            }
        }

        apps.sortedWith(compareBy<AppInfo> { it.isWorkProfile }
            .thenBy { it.isPrivateSpace }
            .thenBy { it.label.lowercase() }
        )
    }

    private fun isManagedProfile(userHandle: UserHandle): Boolean {
        return try {
            userManager.isManagedProfile
        } catch (e: Throwable) {
            false
        }
    }

    private fun isPrivateSpace(userHandle: UserHandle): Boolean {
        if (Build.VERSION.SDK_INT >= 35) {
            return try {
                // In Android 15/16/17 (API 35+), UserManager has isPrivateProfile()
                val method = UserManager::class.java.getMethod("isPrivateProfile")
                method.invoke(userManager) as? Boolean ?: false
            } catch (e: Throwable) {
                false
            }
        }
        return false
    }

    /**
     * Launches an app respecting user profiles and multi-window bounds.
     */
    fun launchApp(app: AppInfo): Boolean {
        return try {
            launcherApps.startMainActivity(app.componentName, app.userHandle, null, null)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to launch via LauncherApps, falling back to Intent", e)
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (intent != null) {
                    context.startActivity(intent)
                    true
                } else false
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to launch app", e2)
                false
            }
        }
    }

    /**
     * Opens system app details / settings for this app.
     */
    fun openAppDetails(app: AppInfo) {
        try {
            launcherApps.startAppDetailsActivity(app.componentName, app.userHandle, null, null)
        } catch (e: Exception) {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", app.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Launches uninstaller intent for this app.
     */
    fun uninstallApp(app: AppInfo) {
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
            data = Uri.fromParts("package", app.packageName, null)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No uninstaller found", e)
        }
    }

    companion object {
        private const val TAG = "LauncherModel"
    }
}
