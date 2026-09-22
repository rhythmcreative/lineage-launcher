package org.lineageos.launcher.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.PopupMenu
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import org.lineageos.launcher.R
import org.lineageos.launcher.databinding.ActivityLauncherBinding
import org.lineageos.launcher.dock.DockDragController
import org.lineageos.launcher.dock.DockPageAdapter
import org.lineageos.launcher.model.AppInfo
import org.lineageos.launcher.model.LauncherModel
import org.lineageos.launcher.settings.LauncherSettingsActivity
import org.lineageos.launcher.updater.LauncherUpdater
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main Home Screen Launcher Activity matching LineageOS Trebuchet & GrapheneOS AOSP standards.
 * Supports modern multi-profile app drawer (Personal, Work, Android 15/16/17 Private Space),
 * Alphabetical FastScroller (A-Z), search generation filtering, clean IME dismissal,
 * contextual long-press options, and multi-page scrollable dock.
 */
class LauncherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLauncherBinding
    private lateinit var launcherModel: LauncherModel
    private lateinit var dockAdapter: DockPageAdapter
    private lateinit var drawerAdapter: AppDrawerAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<android.widget.LinearLayout>
    private lateinit var dockDragController: DockDragController
    private lateinit var insetsController: WindowInsetsControllerCompat

    private var allAppsCached: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        insetsController = WindowCompat.getInsetsController(window, binding.root)

        // Initialize LauncherModel with real-time LauncherApps callback
        launcherModel = LauncherModel(this) {
            loadInstalledApps()
        }

        setupAtAGlance()
        setupDock()
        setupAppDrawer()
        setupQsb()
        setupDesktopGestures()
        loadInstalledApps()

        // Handle back gesture to collapse drawer if expanded
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    dismissIme()
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
        })

        // Check for updates automatically if enabled
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean("pref_auto_update", true)) {
            LauncherUpdater(this).checkForUpdates(silentIfLatest = true)
        }

        checkDefaultLauncher()
    }

    private fun checkDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val roleManager = getSystemService(android.app.role.RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME) && !roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                // Ignore fallback
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyPreferences()
        setupAtAGlance()
    }

    override fun onDestroy() {
        super.onDestroy()
        launcherModel.unregister()
    }

    private fun setupAtAGlance() {
        val dateFormat = SimpleDateFormat(getString(R.string.at_a_glance_date_format), Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()

        binding.atAGlanceDate.text = dateFormat.format(now)
        binding.atAGlanceSubtitle.text = timeFormat.format(now)
    }

    private fun setupDock() {
        dockAdapter = DockPageAdapter(
            context = this,
            onAppClicked = { app -> launchApp(app) },
            onAppLongClicked = { app, view -> startAppDrag(app, view) }
        )

        binding.dockViewPager.apply {
            adapter = dockAdapter
            offscreenPageLimit = 3
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                    binding.dockIndicators.onPageScrolled(position, positionOffset)
                }

                override fun onPageSelected(position: Int) {
                    binding.dockIndicators.onPageScrolled(position, 0f)
                }
            })
        }

        dockDragController = DockDragController(
            context = this,
            viewPager = binding.dockViewPager,
            adapter = dockAdapter,
            indicator = binding.dockIndicators
        )
        dockDragController.attachToView(binding.dockPillContainer)
    }

    private fun setupAppDrawer() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.appDrawerSheet).apply {
            peekHeight = 0
            state = BottomSheetBehavior.STATE_COLLAPSED
            isHideable = true

            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                        dismissIme()
                        binding.drawerSearchInput.text.clear()
                    } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        val prefs = PreferenceManager.getDefaultSharedPreferences(this@LauncherActivity)
                        if (prefs.getBoolean("pref_drawer_open_keyboard", false)) {
                            binding.drawerSearchInput.requestFocus()
                            insetsController.show(WindowInsetsCompat.Type.ime())
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
        }

        drawerAdapter = AppDrawerAdapter(
            context = this,
            onAppClicked = { app ->
                dismissIme()
                launchApp(app)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            },
            onAppLongClicked = { app, view ->
                showAppContextMenu(app, view)
            }
        )

        binding.drawerRecyclerView.apply {
            layoutManager = GridLayoutManager(this@LauncherActivity, 4)
            adapter = drawerAdapter
        }

        // Attach alphabetical FastScroller with popup indicator
        binding.drawerFastScroller.attachRecyclerView(
            binding.drawerRecyclerView,
            binding.drawerFastScrollPopup
        )

        // Setup search input with instant generation filter
        binding.drawerSearchInput.doAfterTextChanged { text ->
            drawerAdapter.filter(text?.toString().orEmpty())
        }

        // Setup profile tabs (All, Work, Private Space)
        binding.drawerTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                dismissIme()
                val filter = when (tab?.tag) {
                    "work" -> AppDrawerAdapter.FILTER_WORK
                    "private" -> AppDrawerAdapter.FILTER_PRIVATE
                    else -> AppDrawerAdapter.FILTER_ALL
                }
                drawerAdapter.setFilterType(filter)
                binding.drawerFastScroller.setApps(drawerAdapter.getDisplayedApps())
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupQsb() {
        binding.qsbContainer.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            binding.drawerSearchInput.requestFocus()
            insetsController.show(WindowInsetsCompat.Type.ime())
        }

        binding.btnOpenSettings.setOnClickListener {
            dismissIme()
            startActivity(Intent(this, LauncherSettingsActivity::class.java))
        }
    }

    private fun setupDesktopGestures() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val prefs = PreferenceManager.getDefaultSharedPreferences(this@LauncherActivity)
                if (prefs.getBoolean("pref_sleep_gesture", false)) {
                    // Double-tap to sleep / turn off display intent
                    val lockIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                    }
                    startActivity(lockIntent)
                    return true
                }
                return false
            }
        })

        binding.desktopWorkspace.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun loadInstalledApps() {
        lifecycleScope.launch {
            val apps = launcherModel.loadApps()
            allAppsCached = apps

            drawerAdapter.setApps(apps)
            binding.drawerFastScroller.setApps(apps)

            // Setup profile tabs
            setupProfileTabs(apps)

            // Distribute first 10 apps into dock pages
            val dockPages = mutableListOf<List<AppInfo>>()
            val chunked = apps.take(10).chunked(5)
            if (chunked.isNotEmpty()) {
                dockPages.addAll(chunked)
            } else {
                dockPages.add(emptyList())
            }

            dockAdapter.setDockPages(dockPages)
            binding.dockIndicators.setPageCount(dockAdapter.getPageCount())
        }
    }

    private fun setupProfileTabs(apps: List<AppInfo>) {
        val hasWork = apps.any { it.isWorkProfile }
        val hasPrivate = apps.any { it.isPrivateSpace }

        binding.drawerTabLayout.removeAllTabs()

        if (hasWork || hasPrivate) {
            binding.drawerTabLayout.visibility = View.VISIBLE
            val tabAll = binding.drawerTabLayout.newTab().setText(R.string.tab_all_apps).setTag("all")
            binding.drawerTabLayout.addTab(tabAll)

            if (hasWork) {
                val tabWork = binding.drawerTabLayout.newTab().setText(R.string.tab_work_apps).setTag("work")
                binding.drawerTabLayout.addTab(tabWork)
            }
            if (hasPrivate) {
                val tabPrivate = binding.drawerTabLayout.newTab().setText(R.string.tab_private_space).setTag("private")
                binding.drawerTabLayout.addTab(tabPrivate)
            }
        } else {
            binding.drawerTabLayout.visibility = View.GONE
        }
    }

    private fun applyPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        drawerAdapter.refreshPreferences()

        // Dock search bar visibility
        val showHotseatQsb = prefs.getBoolean("pref_show_hotseat_qsb", true)
        binding.qsbContainer.visibility = if (showHotseatQsb) View.VISIBLE else View.GONE

        // Desktop At-a-Glance visibility
        val showDesktopQsb = prefs.getBoolean("pref_show_desktop_qsb", true)
        binding.atAGlanceContainer.visibility = if (showDesktopQsb) View.VISIBLE else View.GONE

        // Fast scroller visibility
        val showFastScroller = prefs.getBoolean("pref_drawer_fast_scroller", true)
        binding.drawerFastScroller.visibility = if (showFastScroller) View.VISIBLE else View.GONE
    }

    private fun showAppContextMenu(app: AppInfo, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, R.string.app_info)
        popup.menu.add(0, 2, 1, R.string.uninstall)
        popup.menu.add(0, 3, 2, R.string.add_to_dock)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    dismissIme()
                    launcherModel.openAppDetails(app)
                    true
                }
                2 -> {
                    dismissIme()
                    launcherModel.uninstallApp(app)
                    true
                }
                3 -> {
                    dismissIme()
                    dockAdapter.addAppToCurrentPage(binding.dockViewPager.currentItem, app)
                    binding.dockIndicators.setPageCount(dockAdapter.getPageCount())
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun launchApp(app: AppInfo) {
        dismissIme()
        launcherModel.launchApp(app)
    }

    private fun dismissIme() {
        insetsController.hide(WindowInsetsCompat.Type.ime())
        binding.drawerSearchInput.clearFocus()
    }

    private fun startAppDrag(app: AppInfo, view: View) {
        val item = ClipData.Item(app.packageName)
        val dragData = ClipData(
            app.label,
            arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
            item
        )
        val shadowBuilder = View.DragShadowBuilder(view)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            view.startDragAndDrop(dragData, shadowBuilder, app, 0)
        } else {
            @Suppress("DEPRECATION")
            view.startDrag(dragData, shadowBuilder, app, 0)
        }
    }
}
