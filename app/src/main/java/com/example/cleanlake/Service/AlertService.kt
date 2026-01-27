package com.example.cleanlake.Service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.cleanlake.R

class AlertService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val monitorChannelId = "cleanlake_monitor"
    private val alertChannelId = "cleanlake_alert"
    private val controlChannelId = "cleanlake_control"
    private val controlNotificationId = 8888

    companion object {
        const val ACTION_STOP_ALARM = "com.example.cleanlake.STOP_ALARM"
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createForegroundNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.action == ACTION_STOP_ALARM) {
            stopAlarm()
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        val lokasi = intent?.getStringExtra("lokasi") ?: "Tidak diketahui"
        val triggeredSensors = intent?.getStringArrayListExtra("triggeredSensors") ?: arrayListOf()
        val silent = intent?.getBooleanExtra("silent", false) ?: false

        if (triggeredSensors.isNotEmpty()) {
            showAlertNotification(lokasi, triggeredSensors)

            if (triggeredSensors.size >= 2 && !silent) {
                showAlarmControlNotification()
                playAlarm()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    // ================= FOREGROUND =================

    private fun createForegroundNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                monitorChannelId,
                "CleanLake Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, monitorChannelId)
            .setSmallIcon(R.drawable.ic_lake)
            .setContentTitle("CleanLake Aktif")
            .setContentText("Memantau kualitas air")
            .setOngoing(true)
            .build()
    }

    // ================= ALERT =================

    private fun showAlertNotification(lokasi: String, triggeredSensors: List<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                alertChannelId,
                "Peringatan Air",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, alertChannelId)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("🚨 Peringatan di $lokasi")
            .setStyle(NotificationCompat.BigTextStyle().bigText(triggeredSensors.joinToString("\n")))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    // ================= CONTROL =================

    private fun showAlarmControlNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                controlChannelId,
                "Kontrol Alarm",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, AlertService::class.java).apply {
            action = ACTION_STOP_ALARM
        }

        val pending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, controlChannelId)
            .setSmallIcon(R.drawable.ic_stop)
            .setContentTitle("🔊 Alarm Aktif")
            .setContentText("Tekan untuk mematikan alarm")
            .addAction(R.drawable.ic_stop, "Matikan", pending)
            .setOngoing(true)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(controlNotificationId, notification)
    }

    // ================= SOUND =================

    private fun playAlarm() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound).apply {
                isLooping = true
                start()
            }
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}


