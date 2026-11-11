package com.fishlegal.app.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.fishlegal.app.R
import com.fishlegal.app.data.RegulationRule
import com.fishlegal.app.data.Species

object NotificationHelper {
    private const val CHANNEL_ID = "fishlegal_catch_alerts"
    private const val CHANNEL_NAME = "Catch identification & regulations"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    fun showCatchResult(context: Context, species: Species?, rule: RegulationRule?) {
        ensureChannel(context)

        val title = species?.commonName ?: "Unidentified fish"
        val body = buildRegulationSummary(rule)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        androidx.core.app.NotificationManagerCompat.from(context)
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun buildRegulationSummary(rule: RegulationRule?): String {
        if (rule == null) return "No regulation data available for this zone yet."
        if (rule.isClosed) return "Season is currently CLOSED for this species in this zone."

        val parts = mutableListOf<String>()
        rule.minSizeInches?.let { parts.add("Min size: ${it}in") }
        rule.maxSizeInches?.let { parts.add("Max size: ${it}in") }
        rule.dailyBagLimit?.let { parts.add("Bag limit: $it/day") }
        if (rule.openSeasonStart != null && rule.openSeasonEnd != null) {
            parts.add("Season: ${rule.openSeasonStart} - ${rule.openSeasonEnd}")
        }
        return if (parts.isEmpty()) "No specific limits on file for this zone." else parts.joinToString(" • ")
    }
}
