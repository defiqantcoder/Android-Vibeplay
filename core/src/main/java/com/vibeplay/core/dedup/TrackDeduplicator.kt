package com.vibeplay.core.dedup

import com.vibeplay.core.model.DuplicateCandidate
import com.vibeplay.core.model.DuplicateGroup
import com.vibeplay.core.model.Track
import java.util.UUID
import kotlin.math.abs
import kotlin.math.min

data class DeduplicationReport(
    val groups: List<DuplicateGroup>
)

class TrackDeduplicator {

    fun deduplicate(tracks: List<Track>): DeduplicationReport {
        val groupedTracks = mutableListOf<MutableList<Track>>()
        val processedTrackIds = mutableSetOf<String>()

        for (i in tracks.indices) {
            val trackA = tracks[i]
            if (processedTrackIds.contains(trackA.id)) continue

            val currentGroup = mutableListOf(trackA)
            processedTrackIds.add(trackA.id)

            for (j in i + 1 until tracks.size) {
                val trackB = tracks[j]
                if (processedTrackIds.contains(trackB.id)) continue

                if (isDuplicate(trackA, trackB)) {
                    currentGroup.add(trackB)
                    processedTrackIds.add(trackB.id)
                }
            }
            if (currentGroup.size > 1) {
                groupedTracks.add(currentGroup)
            }
        }

        val duplicateGroups = groupedTracks.map { group ->
            val rankedTracks = group.sortedWith(TrackQualityComparator)

            val candidates = rankedTracks.mapIndexed { index, track ->
                DuplicateCandidate(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    format = track.format,
                    sampleRate = track.sampleRate,
                    bitDepth = track.bitDepth,
                    durationMs = track.durationMs,
                    localUri = track.localUri,
                    isBestQuality = index == 0
                )
            }

            val master = rankedTracks.first()
            DuplicateGroup(
                id = UUID.randomUUID().toString(),
                canonicalTitle = master.title,
                artist = master.artist,
                candidates = candidates
            )
        }

        return DeduplicationReport(groups = duplicateGroups)
    }

    private fun isDuplicate(t1: Track, t2: Track): Boolean {
        val durationDiff = abs(t1.durationMs - t2.durationMs)
        if (durationDiff > 3000) return false

        val titleDist = levenshtein(normalize(t1.title), normalize(t2.title))
        if (titleDist > 2) return false

        val artistDist = levenshtein(normalize(t1.artist), normalize(t2.artist))
        if (artistDist > 2) return false

        return true
    }

    private fun normalize(str: String): String {
        return str.trim().lowercase()
    }

    private fun levenshtein(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        var v0 = IntArray(s2.length + 1) { it }
        var v1 = IntArray(s2.length + 1)

        for (i in 0 until s1.length) {
            v1[0] = i + 1
            for (j in 0 until s2.length) {
                val cost = if (s1[i] == s2[j]) 0 else 1
                v1[j + 1] = min(v1[j] + 1, min(v0[j + 1] + 1, v0[j] + cost))
            }
            val temp = v0
            v0 = v1
            v1 = temp
        }

        return v0[s2.length]
    }

    private object TrackQualityComparator : Comparator<Track> {
        override fun compare(t1: Track, t2: Track): Int {
            // Compare descending Format Tier
            val format1 = getFormatTier(t1.format)
            val format2 = getFormatTier(t2.format)
            if (format1 != format2) return format2.compareTo(format1)

            // Compare descending Sample Rate Tier
            val sr1 = getSampleRateTier(t1.sampleRate)
            val sr2 = getSampleRateTier(t2.sampleRate)
            if (sr1 != sr2) return sr2.compareTo(sr1)

            // Compare descending Bit Depth Tier
            val bd1 = getBitDepthTier(t1.bitDepth)
            val bd2 = getBitDepthTier(t2.bitDepth)
            if (bd1 != bd2) return bd2.compareTo(bd1)

            // Tiebreaker: shorter file path
            val path1 = t1.localUri ?: ""
            val path2 = t2.localUri ?: ""

            if (path1.length != path2.length) {
                return path1.length.compareTo(path2.length)
            }

            // Tiebreaker: alphabetical path
            return path1.compareTo(path2)
        }

        private fun getFormatTier(format: String): Int {
            val f = format.lowercase()
            return when {
                f.contains("flac") || f.contains("alac") || f.contains("dsf") -> 3000
                f.contains("wav") -> 2000
                else -> 1000
            }
        }

        private fun getSampleRateTier(sr: String): Int {
            val s = sr.lowercase().replace("khz", "").trim()
            val rate = s.toDoubleOrNull() ?: 0.0
            return when {
                rate >= 768.0 -> 500
                rate >= 192.0 -> 400
                rate >= 96.0 -> 300
                rate >= 48.0 -> 200
                rate >= 44.1 -> 100
                else -> 0
            }
        }

        private fun getBitDepthTier(bd: String): Int {
            val b = bd.lowercase().replace("-bit", "").replace("bit", "").trim()
            val depth = b.toIntOrNull() ?: 0
            return when {
                depth >= 32 -> 30
                depth >= 24 -> 20
                depth >= 16 -> 10
                else -> 0
            }
        }
    }
}
