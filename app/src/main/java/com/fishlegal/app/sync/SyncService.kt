package com.fishlegal.app.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Thin entry point kept for manifest declaration / future foreground-sync use.
 * Actual sync work runs through WorkManager (SyncWorker), not this service.
 */
class SyncService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        SyncWorker.syncNow(applicationContext)
        stopSelf()
        return START_NOT_STICKY
    }
}
