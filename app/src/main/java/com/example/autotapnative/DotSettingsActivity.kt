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
        if (dotId == null) {
            Toast.makeText(this, "Không tìm thấy dot", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val dots = DotStorage.loadDots(this)
        val dot = dots.find { it.id == dotId }
        if (dot == null) {
            Toast.makeText(this, "Dot không tồn tại", Toast.LENGTH_SHORT).show()
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
        etAnti.setText(dot.antiDetection.toInt().toString())
        etDelay.setText(dot.startDelay.toString())

        btnSave.setOnClickListener {
            try {
                val interval = etInterval.text.toString().toLong()
                val hold = etHold.text.toString().toLong()
                val anti = etAnti.text.toString().toFloat()
                val delay = etDelay.text.toString().toLong()

                dot.actionIntervalTime = interval
                dot.holdTime = hold
                dot.antiDetection = anti
                dot.startDelay = delay

                DotStorage.saveDots(this, dots)

                // Yêu cầu OverlayService reload dots & cập nhật autotap
                sendBroadcast(OverlayService.newRefreshIntent(this))

                Toast.makeText(this, "Đã lưu dot", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, "Giá trị không hợp lệ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val EXTRA_DOT_ID = "extra_dot_id"
    }
}
