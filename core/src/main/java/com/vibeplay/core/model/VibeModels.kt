package com.vibeplay.core.model

data class LyricLine(
    val time: Double,
    val text: String
)

enum class FilterTopology(val displayName: String) {
    BELL_PEAK("Bell Peak"),
    LOW_SHELF("Low-Shelf"),
    HIGH_SHELF("High-Shelf"),
    NOTCH("Notch")
}

data class EqBand(
    val id: String,
    val label: String,
    val freq: Double,
    val gain: Double,
    val q: Double,
    val filterType: FilterTopology = FilterTopology.BELL_PEAK,
    val isEnabled: Boolean = true
)

data class EqPreset(
    val id: String,
    val name: String,
    val gains: List<Double>,
    val description: String = ""
)

enum class EndpointType {
    USB_DAC,
    PHONE_SPEAKER,
    BLUETOOTH
}

data class UsbDacEndpoint(
    val id: String,
    val name: String,
    val type: EndpointType,
    val maxSampleRate: String,
    val dsdSupport: Boolean,
    val vendorId: Int? = null,
    val productId: Int? = null,
    val isConnected: Boolean = true
)

data class CrateFolder(
    val id: String,
    val name: String,
    val trackCount: Int,
    val energyArc: String,
    val lastCooked: String,
    val coverArt: String? = null
)

data class SynthNote(
    val note: String,
    val time: Double,
    val duration: Double
)

data class Track(
    val id: String,
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
    val waveforms: List<Float> = emptyList(),
    val synthNotes: List<SynthNote> = emptyList(),
    val lyrics: List<LyricLine> = emptyList()
)

data class ParsedCookRules(
    val bpmFloor: Int,
    val bpmCeil: Int,
    val targetEnergy: Double,
    val targetDanceability: Double,
    val preferredKeys: List<String> = emptyList(),
    val targetFormats: List<String> = emptyList(),
    val genreKeywords: List<String> = emptyList()
)

data class ScoredTrack(
    val track: Track,
    val score: Double,
    val energyDelta: Double,
    val bpmDelta: Double,
    val harmonicCompatibility: String
)

data class VibeSqlBounds(
    val bpmMin: Int,
    val bpmMax: Int,
    val energyMin: Double,
    val energyMax: Double,
    val danceMin: Double,
    val danceMax: Double,
    val targetKey: String? = null,
    val promptSummary: String = ""
)

enum class VibeThemeMode(val themeId: String, val displayName: String, val primaryHex: String) {
    TEAL("teal", "Teal (Default)", "#06b6d4"),
    AMBER("amber", "Amber", "#f59e0b"),
    LOFI("lofi", "Lo-Fi Purple", "#c084fc"),
    INK("ink", "Ink Emerald", "#6ffbbe")
}

enum class AtmosProfile(val displayName: String) {
    AUTO("Auto"),
    MUSIC("Music"),
    MOVIE("Movie"),
    VOICE("Voice")
}

enum class EnergyArc(val displayName: String) {
    CHILL("Chill Vibe"),
    STEADY("Steady Groove"),
    RAMP("Rising Ramp"),
    PEAK("Peak Energy")
}

data class AddonPackage(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val category: String,
    val installed: Boolean,
    val enabled: Boolean,
    val dspImpact: String = "Low",
    val latencyMs: Double = 0.5
)

data class DuplicateCandidate(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val format: String,
    val sampleRate: String,
    val bitDepth: String,
    val durationMs: Long,
    val localUri: String? = null,
    val isBestQuality: Boolean = false
)

data class DuplicateGroup(
    val id: String,
    val canonicalTitle: String,
    val artist: String,
    val candidates: List<DuplicateCandidate>
)
