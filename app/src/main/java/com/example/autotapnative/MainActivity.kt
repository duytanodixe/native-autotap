package com.example.autotapnative

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.example.autotapnative.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // After returning from the settings screen, check for the permission again.
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Quyền Overlay đã được cấp", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bạn chưa cấp quyền Overlay", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAccessibilitySetting.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        binding.btnOverlayPermission.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:$packageName".toUri())
                overlayPermissionLauncher.launch(intent)
            } else {
                Toast.makeText(this, "Quyền Overlay đã được cấp từ trước", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnStartOverlay.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                val serviceIntent = Intent(this, OverlayService::class.java)
                startService(serviceIntent)
            } else {
                Toast.makeText(this, "Cần cấp quyền Overlay để bắt đầu", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
