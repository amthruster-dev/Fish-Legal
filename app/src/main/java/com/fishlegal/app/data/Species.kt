package com.fishlegal.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local species library entry. Bundled at install time, expanded via
 * background sync when new species data is downloaded.
 */
@Entity(tableName = "species")
data class Species(
    @PrimaryKey val id: String,
    val commonName: String,
    val scientificName: String,
    val family: String,
    val imageAssetPath: String?,
    val description: String?,
    val modelLabelIndex: Int
)
