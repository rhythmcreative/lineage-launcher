package org.lineageos.launcher.dock

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.DragEvent
import android.view.View
import androidx.viewpager2.widget.ViewPager2

/**
 * Handles edge hovering during drag-and-drop on the scrollable dock.
 * When hovering over the right edge on the last dock page for 450ms,
 * it dynamically creates a new dock page and auto-scrolls to it.
 */
class DockDragController(
    private val context: Context,
    private val viewPager: ViewPager2,
    private val adapter: DockPageAdapter,
    private val indicator: DockPageIndicator
) {

    private val handler = Handler(Looper.getMainLooper())
    private var isHoveringEdge = false
    private var hoverDirection = 0 // +1 for next/new page, -1 for previous page
    private var temporaryPageCreated = false

    private val hoverRunnable = Runnable {
        if (!isHoveringEdge) return@Runnable

        val currentItem = viewPager.currentItem
        val totalPages = adapter.getPageCount()

        if (hoverDirection > 0) {
            if (currentItem == totalPages - 1 && totalPages < 10) {
                // Last page reached: dynamically add new empty dock page
                val newIndex = adapter.addEmptyPage()
                indicator.setPageCount(adapter.getPageCount())
                viewPager.setCurrentItem(newIndex, true)
                temporaryPageCreated = true
            } else if (currentItem < totalPages - 1) {
                // Advance to next page
                viewPager.setCurrentItem(currentItem + 1, true)
            }
        } else if (hoverDirection < 0) {
            if (currentItem > 0) {
                viewPager.setCurrentItem(currentItem - 1, true)
            }
        }
    }

    fun attachToView(targetView: View) {
        targetView.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_ENTERED,
                DragEvent.ACTION_DRAG_LOCATION -> {
                    val width = targetView.width
                    val x = event.x
                    val edgeThreshold = 48f * context.resources.displayMetrics.density

                    val onRightEdge = x >= width - edgeThreshold
                    val onLeftEdge = x <= edgeThreshold

                    if (onRightEdge) {
                        if (!isHoveringEdge || hoverDirection != 1) {
                            isHoveringEdge = true
                            hoverDirection = 1
                            handler.removeCallbacks(hoverRunnable)
                            handler.postDelayed(hoverRunnable, 450)
                        }
                    } else if (onLeftEdge) {
                        if (!isHoveringEdge || hoverDirection != -1) {
                            isHoveringEdge = true
                            hoverDirection = -1
                            handler.removeCallbacks(hoverRunnable)
                            handler.postDelayed(hoverRunnable, 450)
                        }
                    } else {
                        cancelHover()
                    }
                    true
                }

                DragEvent.ACTION_DRAG_EXITED -> {
                    cancelHover()
                    true
                }

                DragEvent.ACTION_DROP -> {
                    cancelHover()
                    temporaryPageCreated = false
                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    cancelHover()
                    temporaryPageCreated = false
                    true
                }

                else -> true
            }
        }
    }

    private fun cancelHover() {
        isHoveringEdge = false
        hoverDirection = 0
        handler.removeCallbacks(hoverRunnable)
    }
}
