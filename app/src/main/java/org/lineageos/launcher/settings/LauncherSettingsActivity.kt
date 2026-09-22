package org.lineageos.launcher.settings

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import org.lineageos.launcher.BuildConfig
import org.lineageos.launcher.R
import org.lineageos.launcher.databinding.ActivitySettingsBinding
import org.lineageos.launcher.preview.LottiePreviewActivity
import org.lineageos.launcher.updater.LauncherUpdater

/**
 * Dedicated Material 3 Settings & Customizer Activity for Lineage Launcher.
 * Features live B&W animated vector dock preview, Lottie gestures, comprehensive M3 switches,
 * in-app auto-updater via GitHub Releases, and default launcher activation.
 */
class LauncherSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)

        binding.toolbar.setNavigationOnClickListener { finish() }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // Version info header
        binding.txtVersionInfo.text = "Lineage Launcher • v${BuildConfig.VERSION_NAME} (API 36 / Android 17)"

        // Dock Settings
        binding.switchScrollableDock.isChecked = prefs.getBoolean("pref_dock_pages_enabled", true)
        binding.switchScrollableDock.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("pref_dock_pages_enabled", checked).apply()
        }

        binding.switchInfiniteLoop.isChecked = prefs.getBoolean("pref_dock_infinite_loop", true)
        binding.switchInfiniteLoop.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("pref_dock_infinite_loop", checked).apply()
        }

        binding.switchDragCreatePage.isChecked = prefs.getBoolean("pref_dock_drag_create_page", true)
        binding.switchDragCreatePage.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("pref_dock_drag_create_page", checked).apply()
        }

        binding.switchDockQsb.isChecked = prefs.getBoolean("pref_show_hotseat_qsb", true)
        binding.switchDockQsb.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("pref_show_hotseat_qsb", checked).apply()
        }

        // Lottie gesture animation button
        binding.btnOpenLottie.setOnClickListener {
            startActivity(Intent(this, LottiePreviewActivity::class.java))
        }

        // Home screen settings
        binding.switchLockLayout.isChecked = prefs.getBoolean("pref_workspace_lock", false)
        binding.switchLockLayout.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("pref_workspace_lock", checked).apply()
        }

        binding.switchAutoAddShortcuts.isChecked = prefs.getBoolean("pref_add_icon_to_home", true)
        binding.switchAutoAddShortcuts.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("pref_add_icon_to_home", checked).apply()
        }

        binding.switchDesktopLabels.isChecked = prefs.getBoolean("pref_desktop_show_labels", true)
        binding.switchDesktopLabels.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("pref_desktop_show_labels", checked).apply()
        }

        binding.switchSleepGesture.isChecked = prefs.getBoolean("pref_sleep_gesture", false)
        binding.switchSleepGesture.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("pref_sleep_gesture", checked).apply()
        }

        // App Drawer settings
        binding.switchThemedIcons.isChecked = prefs.getBoolean("pref_allapps_themed_icons", true)
        binding.switchThemedIcons.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("pref_allapps_themed_icons", checked).apply()
        }

        binding.switchDrawerLabels.isChecked = prefs.getBoolean("pref_drawer_show_labels", true)
        binding.switchDrawerLabels.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("pref_drawer_show_labels", checked).apply()
        }

        binding.switchDrawerKeyboard.isChecked = prefs.getBoolean("pref_drawer_open_keyboard", false)
        binding.switchDrawerKeyboard.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("pref_drawer_open_keyboard", checked).apply()
        }

        binding.switchFastScroller.isChecked = prefs.getBoolean("pref_drawer_fast_scroller", true)
        binding.switchFastScroller.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("pref_drawer_fast_scroller", checked).apply()
        }

        // In-App Auto-Updater
        binding.btnCheckUpdates.setOnClickListener {
            LauncherUpdater(this).checkForUpdates(silentIfLatest = false)
        }

        binding.switchAutoUpdate.isChecked = prefs.getBoolean("pref_auto_update", true)
        binding.switchAutoUpdate.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("pref_auto_update", checked).apply()
        }

        // Default Launcher Trigger
        binding.btnSetDefaultLauncher.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val roleManager = getSystemService(RoleManager::class.java)
                    if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                    startActivity(intent)
                }
            } else {
                val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                startActivity(intent)
            }
        }
    }
}
