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
    private val channelId = "alert_channel_id"
    private val controlChannelId = "alarm_control_channel"
    private val controlNotificationId = 9999

    companion object {
        const val ACTION_STOP_ALARM = "com.example.cleanlake.STOP_ALARM"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_ALARM) {
            stopAlarm()
            cancelControlNotification()
            return START_NOT_STICKY
        }

        val lokasi = intent?.getStringExtra("lokasi") ?: "Tidak diketahui"
        val triggeredSensors = intent?.getStringArrayListExtra("triggeredSensors") ?: arrayListOf()

        if (triggeredSensors.isNotEmpty()) {
            showNotification(lokasi, triggeredSensors)

            // 🔊 Alarm hanya jika ≥ 2 sensor bermasalah
            if (triggeredSensors.size >= 2) {
                showAlarmControlNotification()
                playAlarm()
            }
        }

        return START_NOT_STICKY
    }

    /** Notifikasi semua sensor bermasalah **/
    private fun showNotification(lokasi: String, triggeredSensors: List<String>) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Peringatan Kualitas Air",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notifikasi sensor air yang melebihi atau kurang dari ambang batas"
            notificationManager.createNotificationChannel(channel)
        }

        val contentText = triggeredSensors.joinToString("\n")

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("🚨 Peringatan Air di $lokasi")
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /** Notifikasi kontrol alarm **/
    private fun showAlarmControlNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                controlChannelId,
                "Kontrol Alarm",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Kontrol untuk mematikan suara alarm"
            notificationManager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, AlertService::class.java).apply {
            action = ACTION_STOP_ALARM
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val controlNotification = NotificationCompat.Builder(this, controlChannelId)
            .setSmallIcon(R.drawable.ic_stop)
            .setContentTitle("🔊 Alarm Aktif")
            .setContentText("Tekan untuk mematikan suara alarm")
            .setOngoing(true)
            .addAction(R.drawable.ic_stop, "Matikan Alarm", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(controlNotificationId, controlNotification)
    }

    private fun cancelControlNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(controlNotificationId)
    }

    private fun playAlarm() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
            mediaPlayer?.isLooping = true
        }
        mediaPlayer?.start()
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        cancelControlNotification()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
