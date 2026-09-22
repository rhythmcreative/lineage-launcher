package org.lineageos.launcher.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
 * Main Home Screen Launcher Activity.
 */
class LauncherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLauncherBinding
    private lateinit var launcherModel: LauncherModel
    private lateinit var dockAdapter: DockPageAdapter
    private lateinit var drawerAdapter: AppDrawerAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var dockDragController: DockDragController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        launcherModel = LauncherModel(this)

        setupAtAGlance()
        setupDock()
        setupAppDrawer()
        setupQsb()
        loadInstalledApps()

        // Handle back press to collapse drawer if open
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
        })

        // Check for updates automatically in background if enabled
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean("pref_auto_update", true)) {
            LauncherUpdater(this).checkForUpdates(silentIfLatest = true)
        }
    }

    private fun setupAtAGlance() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat(getString(R.string.at_a_glance_date_format), Locale.getDefault())

        val now = Date()
        binding.txtClock.text = timeFormat.format(now)
        binding.txtDate.text = dateFormat.format(now)
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

        // Initialize drag-to-new-page controller
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
        }

        drawerAdapter = AppDrawerAdapter(
            context = this,
            onAppClicked = { app ->
                launchApp(app)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            },
            onAppLongClicked = { app, view ->
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                startAppDrag(app, view)
            }
        )

        binding.drawerRecyclerView.apply {
            layoutManager = GridLayoutManager(this@LauncherActivity, 4)
            adapter = drawerAdapter
        }

        binding.drawerSearchInput.doAfterTextChanged { text ->
            drawerAdapter.filter(text?.toString().orEmpty())
        }
    }

    private fun setupQsb() {
        binding.qsbContainer.setOnClickListener {
            // Tap search bar -> expand app drawer and focus search
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            binding.drawerSearchInput.requestFocus()
        }

        binding.btnOpenSettings.setOnClickListener {
            startActivity(Intent(this, LauncherSettingsActivity::class.java))
        }
    }

    private fun loadInstalledApps() {
        lifecycleScope.launch {
            val apps = launcherModel.loadApps()
            drawerAdapter.setApps(apps)

            // Distribute the first 10 apps across 2 default dock pages
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

    private fun launchApp(app: AppInfo) {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        }
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
            view.startDrag(dragData, shadowBuilder, app, 0)
        }
    }
}
