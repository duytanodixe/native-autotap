package com.example.autotapnative

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DotSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dot_settings)

        val dotId = intent.getStringExtra(EXTRA_DOT_ID)
        val profileName = intent.getStringExtra(EXTRA_PROFILE_NAME)

        if (dotId == null || profileName == null) {
            Toast.makeText(this, "Lỗi: Thiếu thông tin Dot hoặc Profile", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val allProfiles = ProfileManager.loadProfiles(this)
        val profile = allProfiles.find { it.name == profileName }
        val dot = profile?.dots?.find { it.id == dotId }

        if (dot == null) {
            Toast.makeText(this, "Dot hoặc Profile không tồn tại", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val etInterval = findViewById<EditText>(R.id.etInterval)
        val etHold = findViewById<EditText>(R.id.etHold)
        val etAnti = findViewById<EditText>(R.id.etAntiDetection)
        val etDelay = findViewById<EditText>(R.id.etStartDelay)
        val btnSave = findViewById<Button>(R.id.btnSaveDot)

        etInterval.setText(dot.actionIntervalTime.toString())
        etHold.setText(dot.holdTime.toString())
        etAnti.setText(dot.antiDetection.toString())
        etDelay.setText(dot.startDelay.toString())

        btnSave.setOnClickListener {
            try {
                dot.actionIntervalTime = etInterval.text.toString().toLong()
                dot.holdTime = etHold.text.toString().toLong()
                dot.antiDetection = etAnti.text.toString().toFloat()
                dot.startDelay = etDelay.text.toString().toLong()

                ProfileManager.saveProfiles(this, allProfiles)

                sendBroadcast(OverlayService.newRefreshIntent(this))

                Toast.makeText(this, "Đã lưu cài đặt cho dot ${dot.id}", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, "Giá trị không hợp lệ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val EXTRA_DOT_ID = "extra_dot_id"
        const val EXTRA_PROFILE_NAME = "extra_profile_name"
    }
}
