package org.lineageos.launcher.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import org.lineageos.launcher.R

/**
 * Fragment containing preferences for the Scrollable Dock,
 * with the Material 3 monochrome animated preview header at the top.
 */
class DockSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.dock_preferences, rootKey)
    }
}
