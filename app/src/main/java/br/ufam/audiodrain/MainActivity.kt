package br.ufam.audiodrain

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.os.BatteryManager
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var batteryManager: BatteryManager
    private var batteryStart: Int = 0
    private var startTime: Long = 0

    private lateinit var txtResult: TextView
    private var mediaPlayer: MediaPlayer? = null

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
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun playDefaultAudio() {
        stopPlayer()

        batteryStart = getBatteryLevel()
        startTime = System.currentTimeMillis()

        mediaPlayer = MediaPlayer.create(this, R.raw.sample_audio)
        mediaPlayer?.setOnCompletionListener {
            showResults()
        }
        mediaPlayer?.start()
    }

    private fun playSelectedAudio(uri: android.net.Uri) {
        stopPlayer()

        batteryStart = getBatteryLevel()
        startTime = System.currentTimeMillis()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@MainActivity, uri)
            prepare()
            setOnCompletionListener {
                showResults()
            }
            start()
        }
    }

    private fun showResults() {
        val batteryEnd = getBatteryLevel()
        val endTime = System.currentTimeMillis()

        val batteryConsumed = batteryStart - batteryEnd
        val durationSeconds = (endTime - startTime) / 1000.0

        txtResult.text = """
            🔋 Bateria inicial: $batteryStart%
            🔋 Bateria final: $batteryEnd%
            📉 Consumo: $batteryConsumed%
            ⏱️ Tempo: $durationSeconds segundos
        """.trimIndent()
    }

    private fun stopPlayer() {
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
