package com.fishlegal.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single identification event. Stored locally always; synced to backend
 * when online (uploadState tracks the queue).
 */
@Entity(tableName = "catch_log")
data class CatchLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val photoPath: String,
    val speciesId: String?,
    val confidence: Float,
    val latitude: Double,
    val longitude: Double,
    val zoneId: String?,
    val timestamp: Long,
    val uploadState: UploadState = UploadState.PENDING
)

enum class UploadState { PENDING, UPLOADED, FAILED }
