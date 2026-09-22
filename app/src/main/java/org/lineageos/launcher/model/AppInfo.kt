package org.lineageos.launcher.model

import android.content.ComponentName
import android.graphics.drawable.Drawable

/**
 * Model representing an installed launcher application.
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val className: String,
    val icon: Drawable? = null
) {
    val componentName: ComponentName
        get() = ComponentName(packageName, className)
}
