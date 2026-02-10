package br.ufam.audiodrain

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var batteryManager: BatteryManager
    private var batteryStart: Int = 0
    private var startTime: Long = 0

    private lateinit var txtResult: TextView
    private var mediaPlayer: MediaPlayer? = null

    private val currentSamplesUa = mutableListOf<Int>()
    private val handler = Handler(Looper.getMainLooper())
    private var sampling = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager

        val btnPlayDefault = findViewById<Button>(R.id.btnPlayDefault)
        val btnPickAudio = findViewById<Button>(R.id.btnPickAudio)
        txtResult = findViewById(R.id.txtResult)

        btnPlayDefault.setOnClickListener {
            playDefaultAudio()
        }

        btnPickAudio.setOnClickListener {
            pickAudioFromStorage()
        }
    }

    private fun getBatteryLevel(): Int {
        return batteryManager.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CAPACITY
        )
    }

    private fun getBatteryCurrentUa(): Int {
        return batteryManager.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CURRENT_NOW
        )
    }

    private fun playDefaultAudio() {
        stopPlayer()
        startMeasurement()

        mediaPlayer = MediaPlayer.create(this, R.raw.sample_audio)
        mediaPlayer?.setOnCompletionListener {
            stopMeasurement()
            showResults()
        }
        mediaPlayer?.start()
    }

    private fun playSelectedAudio(uri: android.net.Uri) {
        stopPlayer()
        startMeasurement()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@MainActivity, uri)
            prepare()
            setOnCompletionListener {
                stopMeasurement()
                showResults()
            }
            start()
        }
    }

    private fun startMeasurement() {
        batteryStart = getBatteryLevel()
        startTime = System.currentTimeMillis()
        currentSamplesUa.clear()
        sampling = true
        sampleCurrent()
    }

    private fun stopMeasurement() {
        sampling = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun sampleCurrent() {
        if (!sampling) return

        val currentUa = getBatteryCurrentUa()
        currentSamplesUa.add(currentUa)

        handler.postDelayed({ sampleCurrent() }, 500)
    }

    private fun showResults() {
        val batteryEnd = getBatteryLevel()
        val endTime = System.currentTimeMillis()
        val durationSeconds = (endTime - startTime) / 1000.0

        val currentMaSamples = currentSamplesUa.map {
            abs(it) / 1000.0
        }

        val avgCurrent = currentMaSamples.average()
        val maxCurrent = currentMaSamples.maxOrNull() ?: 0.0

        txtResult.text = """
            🔌 Corrente média: ${"%.2f".format(avgCurrent)} mA
            📈 Pico de corrente: ${"%.2f".format(maxCurrent)} mA
            ⏱️ Tempo: ${"%.2f".format(durationSeconds)} s
            🔋 Bateria inicial: $batteryStart%
            🔋 Bateria final: $batteryEnd%
        """.trimIndent()
    }

    private fun stopPlayer() {
        stopMeasurement()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun pickAudioFromStorage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "audio/*"
        }
        audioPickerLauncher.launch(intent)
    }

    private val audioPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    playSelectedAudio(uri)
                }
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayer()
    }
}
