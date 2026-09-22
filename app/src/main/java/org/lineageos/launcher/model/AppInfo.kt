package org.lineageos.launcher.model

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle

/**
 * Model representing an installed launcher application supporting modern multi-profile
 * architectures (Personal, Work Profile, and Android 15/16/17 Private Space).
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val className: String,
    val icon: Drawable? = null,
    val userHandle: UserHandle = Process.myUserHandle(),
    val isWorkProfile: Boolean = false,
    val isPrivateSpace: Boolean = false
) {
    val componentName: ComponentName
        get() = ComponentName(packageName, className)

    val sectionLetter: String
        get() {
            val first = label.trim().firstOrNull() ?: return "#"
            return if (first.isLetter()) first.uppercaseChar().toString() else "#"
        }
}
