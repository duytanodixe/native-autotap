package com.example.autotap

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvInfo = findViewById<TextView>(R.id.tvInfo)
        val btnOverlay = findViewById<Button>(R.id.btnOverlayPermission)
        val btnStartOverlay = findViewById<Button>(R.id.btnStartOverlay)
        val btnAccessibility = findViewById<Button>(R.id.btnAccessibilitySetting)

        tvInfo.text = """
            1. Cấp quyền vẽ nổi (overlay).
            2. Mở màn hình trợ năng và bật AutoTapNative.
            3. Nhấn Start Overlay để hiện toolbar nổi.
            4. Dùng toolbar để Add Dot và Start/Stop autotap.
        """.trimIndent()

        btnOverlay.setOnClickListener {
            requestOverlayPermission()
        }

        btnStartOverlay.setOnClickListener {
            if (OverlayService.canDrawOverlays(this)) {
                val intent = Intent(this, OverlayService::class.java)
                startService(intent)
            } else {
                requestOverlayPermission()
            }
        }

        btnAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
    }
}


