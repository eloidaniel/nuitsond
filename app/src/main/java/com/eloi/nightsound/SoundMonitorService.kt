package com.eloi.nightsound

import android.app.*
import android.content.Intent
import android.media.*
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

class SoundMonitorService : Service() {
    companion object {
        private const val CHANNEL_ID = "night_sound"
        private const val NOTIFICATION_ID = 42
        private const val SAMPLE_RATE = 44100
        private const val PRE_ROLL_MS = 5000L
        private const val SILENCE_MS = 3000L
        private const val THRESHOLD_DB = 55.0
    }

    private var recorder: AudioRecord? = null
    private var monitoring = false
    private var recording = false
    private var output: FileOutputStream? = null
    private var file: File? = null
    private var lastSound = 0L
    private val chunks = ArrayDeque<ByteArray>()

    private val bufferSize by lazy {
        max(
            AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ), 4096
        )
    }

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Night Sound",
                NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> startMonitoring()
            "STOP" -> stopMonitoring()
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        if (monitoring) return

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Night Sound")
            .setContentText("Surveillance sonore en cours")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        recorder!!.startRecording()
        monitoring = true

        Thread {
            val buffer = ByteArray(bufferSize)
            while (monitoring) {
                val read = recorder?.read(buffer, 0, buffer.size) ?: 0
                if (read <= 0) continue

                val chunk = buffer.copyOf(read)
                addPreRoll(chunk)
                val db = calculateDb(chunk)
                val now = System.currentTimeMillis()

                if (db >= THRESHOLD_DB) {
                    lastSound = now
                    if (!recording) beginRecording()
                    output?.write(chunk)
                } else if (recording) {
                    output?.write(chunk)
                    if (now - lastSound >= SILENCE_MS) finishRecording()
                }
            }
        }.start()
    }

    private fun addPreRoll(chunk: ByteArray) {
        chunks.addLast(chunk)
        var total = chunks.sumOf { it.size }
        val maxBytes = SAMPLE_RATE * 2 * (PRE_ROLL_MS / 1000).toInt()
        while (total > maxBytes && chunks.isNotEmpty()) {
            total -= chunks.removeFirst().size
        }
    }

    private fun beginRecording() {
        val dir = File(getExternalFilesDir(null), "events").apply { mkdirs() }
        file = File(dir, "sound_${System.currentTimeMillis()}.wav")
        output = FileOutputStream(file)
        output!!.write(ByteArray(44))
        chunks.forEach { output!!.write(it) }
        recording = true
    }

    private fun finishRecording() {
        output?.flush()
        output?.close()
        output = null
        file?.let { fixWavHeader(it) }
        file = null
        chunks.clear()
        recording = false
    }

    private fun stopMonitoring() {
        monitoring = false
        if (recording) finishRecording()
        try { recorder?.stop() } catch (_: Exception) {}
        recorder?.release()
        recorder = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun calculateDb(data: ByteArray): Double {
        if (data.size < 2) return 0.0
        var sum = 0.0
        var i = 0
        while (i + 1 < data.size) {
            val raw = (data[i].toInt() and 255) or
                    (data[i + 1].toInt() shl 8)
            val sample = if (raw and 0x8000 != 0) raw - 65536 else raw
            sum += sample.toDouble() * sample
            i += 2
        }
        val rms = sqrt(sum / (data.size / 2))
        return 20.0 * log10(max(rms, 1.0) / 32768.0) + 100.0
    }

    private fun fixWavHeader(file: File) {
        val dataLength = (file.length() - 44).toInt()
        val h = ByteArray(44)

        fun intLE(o: Int, v: Int) {
            h[o] = v.toByte(); h[o+1] = (v shr 8).toByte()
            h[o+2] = (v shr 16).toByte(); h[o+3] = (v shr 24).toByte()
        }
        fun shortLE(o: Int, v: Int) {
            h[o] = v.toByte(); h[o+1] = (v shr 8).toByte()
        }

        "RIFF".toByteArray().copyInto(h, 0)
        intLE(4, dataLength + 36)
        "WAVEfmt ".toByteArray().copyInto(h, 8)
        intLE(16, 16); shortLE(20, 1); shortLE(22, 1)
        intLE(24, SAMPLE_RATE); intLE(28, SAMPLE_RATE * 2)
        shortLE(32, 2); shortLE(34, 16)
        "data".toByteArray().copyInto(h, 36)
        intLE(40, dataLength)

        val raf = java.io.RandomAccessFile(file, "rw")
        raf.seek(0); raf.write(h); raf.close()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
