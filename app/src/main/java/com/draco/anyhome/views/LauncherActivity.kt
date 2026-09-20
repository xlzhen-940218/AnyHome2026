package com.draco.anyhome.views

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.draco.anyhome.viewmodels.LauncherActivityViewModel

class LauncherActivity : AppCompatActivity() {
    private val viewModel: LauncherActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        if (!viewModel.isHomeAppSet()) {
            val intent = Intent(this, SelectActivity::class.java)
            startActivity(intent)
            disableActivityTransition()
            return
        }

        startActivity(viewModel.homeAppIntent)
        disableActivityTransition()
    }

    private fun disableActivityTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}