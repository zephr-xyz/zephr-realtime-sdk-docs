package xyz.zephr.sampleclient.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.zephr.sdk.v2.ZephrEventListener
import com.zephr.sdk.v2.ZephrRealtimeManager
import com.zephr.sdk.v2.ZephrRealtimeSDK
import com.zephr.sdk.v2.model.ZephrPoseEvent

private const val TAG = "ZephrSampleService"


class ZephrGnssService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "gnss_tracking_channel"
    }

    private lateinit var zephrSDK: ZephrRealtimeSDK
    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler

    private val zephrListener = object : ZephrEventListener {
        override fun onZephrGnssReceived(zephrGnssEvent: com.zephr.sdk.v2.model.ZephrGnssEvent) {
            val status = zephrGnssEvent.status
            val location = zephrGnssEvent.location
            if (location != null) {
                Log.d(
                    TAG,
                    "GNSS Update - Status: $status, Lat: ${location.latitude}, Lng: ${location.longitude}, Alt: ${location.altitude}"
                )
            } else {
                Log.d(TAG, "GNSS Update - Status: $status, Location: null")
            }
        }

        override fun onPoseChanged(
            zephrPoseEvent: ZephrPoseEvent
        ) {
            Log.d(
                TAG,
                "Pose Update - yaw: ${zephrPoseEvent.yprWithTimestamp?.first?.get(0)} pitch: ${zephrPoseEvent.yprWithTimestamp?.first?.get(1)} roll: ${zephrPoseEvent.yprWithTimestamp?.first?.get(2)}"
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())

        zephrSDK = ZephrRealtimeManager.getZephrSDK(this)

        handlerThread = HandlerThread("GnssHandlerThread").apply { start() }
        handler = Handler(handlerThread.looper)


        registerGnssCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Make sure startForeground has already been called
        return START_STICKY
    }

    private fun registerGnssCallback() {
        try {
            zephrSDK.requestLocationUpdates(zephrListener)
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for GNSS registration", e)
            stopSelf()
        }
    }

    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "GNSS Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GNSS Tracking")
            .setContentText("Collecting GNSS measurements...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // Replace with your app icon ideally
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        zephrSDK.removeLocationUpdates(zephrListener)
        handlerThread.quitSafely()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
