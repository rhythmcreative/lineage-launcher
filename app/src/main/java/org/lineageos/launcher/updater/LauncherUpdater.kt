package org.lineageos.launcher.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.lineageos.launcher.BuildConfig
import org.lineageos.launcher.R
import java.io.File

/**
 * Built-in in-app auto-updater for LineageLauncher.
 * Checks GitHub Releases API for new updates, parses release notes,
 * downloads APK, and initiates installation via FileProvider.
 */
class LauncherUpdater(private val context: Context) {

    private val client = OkHttpClient()
    private val repoSlug = "rhythmcreative/lineage-launcher"
    private val apiUrl = "https://api.github.com/repos/$repoSlug/releases/latest"

    data class ReleaseInfo(
        val tagName: String,
        val changelog: String,
        val apkUrl: String,
        val apkName: String
    )

    fun checkForUpdates(silentIfLatest: Boolean = false, onComplete: ((Boolean) -> Unit)? = null) {
        CoroutineScope(Dispatchers.Main).launch {
            if (!silentIfLatest) {
                Toast.makeText(context, R.string.checking_updates, Toast.LENGTH_SHORT).show()
            }

            val release = withContext(Dispatchers.IO) {
                fetchLatestRelease()
            }

            if (release == null) {
                if (!silentIfLatest) {
                    Toast.makeText(context, R.string.update_failed, Toast.LENGTH_LONG).show()
                }
                onComplete?.invoke(false)
                return@launch
            }

            val currentVersion = "v${BuildConfig.VERSION_NAME}"
            val hasUpdate = isNewerVersion(release.tagName, currentVersion)

            if (hasUpdate) {
                showUpdateDialog(release)
                onComplete?.invoke(true)
            } else {
                if (!silentIfLatest) {
                    val msg = context.getString(R.string.no_updates_available, BuildConfig.VERSION_NAME)
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
                onComplete?.invoke(false)
            }
        }
    }

    private fun fetchLatestRelease(): ReleaseInfo? {
        return try {
            val request = Request.Builder()
                .url(apiUrl)
                .header("User-Agent", "LineageLauncher-Updater")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyStr = response.body?.string() ?: return null
                val json = JSONObject(bodyStr)
                val tagName = json.optString("tag_name")
                val changelog = json.optString("body", "Bug fixes and performance improvements")
                val assets = json.optJSONArray("assets")

                var apkUrl: String? = null
                var apkName = "lineage-launcher-$tagName.apk"

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url")
                            apkName = name
                            break
                        }
                    }
                }

                if (apkUrl != null) {
                    ReleaseInfo(tagName, changelog, apkUrl, apkName)
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isNewerVersion(remoteTag: String, localTag: String): Boolean {
        val cleanRemote = remoteTag.trimStart('v', 'V').split(".")
        val cleanLocal = localTag.trimStart('v', 'V').split(".")

        for (i in 0 until maxOf(cleanRemote.size, cleanLocal.size)) {
            val rPart = cleanRemote.getOrNull(i)?.toIntOrNull() ?: 0
            val lPart = cleanLocal.getOrNull(i)?.toIntOrNull() ?: 0
            if (rPart > lPart) return true
            if (rPart < lPart) return false
        }
        return false
    }

    private fun showUpdateDialog(release: ReleaseInfo) {
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.update_available_title, release.tagName))
            .setMessage("${context.getString(R.string.changelog)}:\n\n${release.changelog}")
            .setPositiveButton(R.string.update_download_install) { _, _ ->
                startApkDownload(release)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun startApkDownload(release: ReleaseInfo) {
        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(release.apkUrl)
            val request = DownloadManager.Request(uri).apply {
                setTitle("LineageLauncher ${release.tagName}")
                setDescription("Downloading APK update...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, release.apkName)
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadId = dm.enqueue(request)
            Toast.makeText(context, R.string.update_downloading, Toast.LENGTH_SHORT).show()

            // Register receiver for install when finished
            val onCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        context.unregisterReceiver(this)
                        installDownloadedApk(release.apkName)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: Open browser to release page
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(release.apkUrl))
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(browserIntent)
        }
    }

    private fun installDownloadedApk(apkName: String) {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), apkName)
        if (!file.exists()) return

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }
}
