package com.fishlegal.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A geographic regulation zone (e.g. state waters, specific coastal district).
 * Boundaries stored as a simple bounding box + polygon GeoJSON string for
 * offline point-in-zone lookups.
 */
@Entity(tableName = "regulation_zones")
data class RegulationZone(
    @PrimaryKey val zoneId: String,
    val name: String,
    val state: String,
    val polygonGeoJson: String,
    val lastUpdated: Long
)

/**
 * Per-species rule within a zone: size/bag limits, season, closures.
 */
@Entity(tableName = "regulation_rules", primaryKeys = ["zoneId", "speciesId"])
data class RegulationRule(
    val zoneId: String,
    val speciesId: String,
    val minSizeInches: Double?,
    val maxSizeInches: Double?,
    val dailyBagLimit: Int?,
    val openSeasonStart: String?, // MM-dd
    val openSeasonEnd: String?,   // MM-dd
    val isClosed: Boolean,
    val notes: String?,
    val sourceUrl: String?,
    val lastUpdated: Long
)
