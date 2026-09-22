package org.lineageos.launcher.preview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.lineageos.launcher.databinding.ActivityLottiePreviewBinding

/**
 * Interactive educational screen demonstrating the Dock swipe gesture
 * with Motion Assist vector animations using Lottie.
 */
class LottiePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLottiePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLottiePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Motion Assist Preview"

        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
