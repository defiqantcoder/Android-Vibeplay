package com.vibeplay.core.db

import androidx.room.*
import com.vibeplay.core.model.FilterTopology

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: String,
    val durationMs: Long,
    val bpm: Int,
    val key: String,
    val energy: Double,
    val danceability: Double,
    val crateId: String,
    val format: String,
    val sampleRate: String,
    val bitDepth: String,
    val coverArt: String? = null,
    val localUri: String? = null,
    val waveformsJson: String = "",
    val lyricsJson: String = ""
)

@Entity(tableName = "crates")
data class CrateFolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val trackCount: Int,
    val energyArc: String,
    val lastCooked: String,
    val coverArt: String? = null
)

@Entity(tableName = "eq_bands")
data class EqBandEntity(
    @PrimaryKey val id: String,
    val label: String,
    val freq: Double,
    val gain: Double,
    val q: Double,
    val filterType: String = FilterTopology.BELL_PEAK.name,
    val isEnabled: Boolean = true
)

@Dao
interface VibePlayDao {
    @Query("SELECT * FROM tracks")
    suspend fun getAllTracks(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): TrackEntity?

    @Query("""
        SELECT * FROM tracks 
        WHERE bpm >= :bpmMin AND bpm <= :bpmMax 
          AND energy >= :energyMin AND energy <= :energyMax 
          AND danceability >= :danceMin AND danceability <= :danceMax
        ORDER BY energy DESC
    """)
    suspend fun queryTracksByBounds(
        bpmMin: Int,
        bpmMax: Int,
        energyMin: Double,
        energyMax: Double,
        danceMin: Double,
        danceMax: Double
    ): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Query("SELECT * FROM crates")
    suspend fun getAllCrates(): List<CrateFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrates(crates: List<CrateFolderEntity>)

    @Query("SELECT * FROM eq_bands")
    suspend fun getAllEqBands(): List<EqBandEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEqBands(bands: List<EqBandEntity>)

    @Query("UPDATE eq_bands SET gain = :gain WHERE id = :id")
    suspend fun updateBandGain(id: String, gain: Double)
}

@Database(
    entities = [TrackEntity::class, CrateFolderEntity::class, EqBandEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VibePlayDatabase : RoomDatabase() {
    abstract fun dao(): VibePlayDao
}
