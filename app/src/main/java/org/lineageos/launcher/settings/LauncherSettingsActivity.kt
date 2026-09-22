package org.lineageos.launcher.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.lineageos.launcher.BuildConfig
import org.lineageos.launcher.R
import org.lineageos.launcher.databinding.ActivitySettingsBinding
import org.lineageos.launcher.preview.LottiePreviewActivity
import org.lineageos.launcher.updater.LauncherUpdater

/**
 * Settings activity for LineageLauncher.
 */
class LauncherSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, MainSettingsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            supportActionBar?.title = getString(R.string.settings_title)
            return true
        }
        finish()
        return true
    }

    class MainSettingsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.launcher_preferences, rootKey)

            // Setup Lottie preview trigger
            findPreference<Preference>("pref_lottie_preview")?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), LottiePreviewActivity::class.java))
                true
            }

            // Setup Check for Updates trigger
            findPreference<Preference>("pref_check_updates")?.apply {
                summary = getString(R.string.check_for_updates_summary, BuildConfig.VERSION_NAME)
                setOnPreferenceClickListener {
                    LauncherUpdater(requireContext()).checkForUpdates(silentIfLatest = false)
                    true
                }
            }
        }
    }
}
