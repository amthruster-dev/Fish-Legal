package com.fishlegal.app.data

import androidx.room.*

@Dao
interface SpeciesDao {
    @Query("SELECT * FROM species")
    suspend fun getAll(): List<Species>

    @Query("SELECT * FROM species WHERE id = :id")
    suspend fun getById(id: String): Species?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(species: List<Species>)
}

@Dao
interface RegulationDao {
    @Query("SELECT * FROM regulation_zones")
    suspend fun getAllZones(): List<RegulationZone>

    @Query("SELECT * FROM regulation_rules WHERE zoneId = :zoneId AND speciesId = :speciesId")
    suspend fun getRule(zoneId: String, speciesId: String): RegulationRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertZones(zones: List<RegulationZone>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRules(rules: List<RegulationRule>)
}

@Dao
interface CatchLogDao {
    @Insert
    suspend fun insert(entry: CatchLog): Long

    @Query("SELECT * FROM catch_log WHERE uploadState = 'PENDING'")
    suspend fun getPendingUploads(): List<CatchLog>

    @Update
    suspend fun update(entry: CatchLog)

    @Query("SELECT * FROM catch_log ORDER BY timestamp DESC")
    suspend fun getAll(): List<CatchLog>
}
