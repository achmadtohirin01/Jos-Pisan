package com.example.dsp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log

class MediaProjectionService : Service() {

    companion object {
        private const val CHANNEL_ID = "MediaProjectionServiceChannel"
        private const val NOTIFICATION_ID = 8888
        
        var activeMediaProjection: MediaProjection? = null
            private set
            
        fun stopActiveProjection() {
            activeMediaProjection?.stop()
            activeMediaProjection = null
        }
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): MediaProjectionService = this@MediaProjectionService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("RESULT_CODE", -1) ?: -1
        val resultData: Intent? = intent?.getParcelableExtra("RESULT_DATA")

        if (resultCode != -1 && resultData != null) {
            val notification = createNotification()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID, 
                    notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            try {
                val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val mp = mpManager.getMediaProjection(resultCode, resultData)
                if (mp != null) {
                    activeMediaProjection = mp
                    
                    // Set callbacks to clean up if stopped from system
                    mp.registerCallback(object : MediaProjection.Callback() {
                        override fun onStop() {
                            Log.d("MediaProjectionService", "MediaProjection stopped")
                            activeMediaProjection = null
                            stopSelf()
                        }
                    }, null)
                    
                    Log.d("MediaProjectionService", "MediaProjection successfully initialized")
                    
                    // Notify AudioEngine to restart recording using the active media projection
                    AudioEngine.instance.setMediaProjection(mp)
                } else {
                    Log.e("MediaProjectionService", "MediaProjection is null")
                    stopSelf()
                }
            } catch (e: Exception) {
                Log.e("MediaProjectionService", "Failed to start media projection: ${e.message}")
                stopSelf()
            }
        } else {
            Log.e("MediaProjectionService", "Invalid service start parameters")
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        stopActiveProjection()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Latar Belakang Penangkapan Audio",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("BroAudio Cross Aktif")
            .setContentText("Sedang menangkap audio internal perangkat...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
    }
}
