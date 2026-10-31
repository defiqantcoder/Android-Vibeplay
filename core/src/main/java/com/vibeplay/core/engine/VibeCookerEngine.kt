package com.vibeplay.core.engine

import com.vibeplay.core.model.ScoredTrack
import com.vibeplay.core.model.Track
import com.vibeplay.core.model.VibeSqlBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

object VibeCookerEngine {

    /**
     * Parses free-form prompt string into SQL boundary constraints and target features
     */
    fun parsePromptToSqlBounds(prompt: String): VibeSqlBounds {
        val lower = prompt.lowercase()
        var bpmMin = 60
        var bpmMax = 180
        var energyMin = 0.0
        var energyMax = 1.0
        var danceMin = 0.0
        var danceMax = 1.0
        var targetKey: String? = null

        // 1. Explicit BPM Regex Matches
        val underMatch = Regex("""(?:under|<|below|sub)\s*(\d{2,3})""").find(lower)
        if (underMatch != null) {
            bpmMax = underMatch.groupValues[1].toInt()
        }

        val overMatch = Regex("""(?:over|>|above|faster than)\s*(\d{2,3})""").find(lower)
        if (overMatch != null) {
            bpmMin = overMatch.groupValues[1].toInt()
        }

        val aroundMatch = Regex("""(?:around|approx|~|near)\s*(\d{2,3})""").find(lower)
        if (aroundMatch != null) {
            val center = aroundMatch.groupValues[1].toInt()
            bpmMin = center - 8
            bpmMax = center + 8
        }

        val exactBpmMatch = Regex("""(\d{2,3})\s*bpm""").find(lower)
        if (exactBpmMatch != null && aroundMatch == null && underMatch == null && overMatch == null) {
            val center = exactBpmMatch.groupValues[1].toInt()
            bpmMin = center - 10
            bpmMax = center + 10
        }

        // 2. Keyword Vibe Rules
        if (lower.contains("cardio") || lower.contains("running") || lower.contains("workout") || lower.contains("techno") || lower.contains("gym")) {
            bpmMin = maxOf(bpmMin, 130)
            energyMin = maxOf(energyMin, 0.75)
            danceMin = maxOf(danceMin, 0.65)
        }

        if (lower.contains("chill") || lower.contains("lofi") || lower.contains("coffee") || lower.contains("study") || lower.contains("relax") || lower.contains("midnight")) {
            bpmMax = minOf(bpmMax, 105)
            energyMax = minOf(energyMax, 0.60)
            danceMin = maxOf(danceMin, 0.50)
        }

        if (lower.contains("synth") || lower.contains("cyberpunk") || lower.contains("neon") || lower.contains("retro") || lower.contains("french")) {
            bpmMin = maxOf(bpmMin, 115)
            bpmMax = minOf(bpmMax, 135)
            energyMin = maxOf(energyMin, 0.70)
            danceMin = maxOf(danceMin, 0.75)
        }

        if (lower.contains("audiophile") || lower.contains("dsd") || lower.contains("acoustic") || lower.contains("master") || lower.contains("hifi")) {
            energyMax = minOf(energyMax, 0.85)
        }

        // 3. Key Detection
        val keyMatch = Regex("""\b([1-9]|1[0-2])[ab]\b""").find(lower)
        if (keyMatch != null) {
            targetKey = keyMatch.value.uppercase()
        }

        val summary = "BPM [$bpmMin-$bpmMax], Energy [${(energyMin * 100).toInt()}%-${(energyMax * 100).toInt()}%], Dance [${(danceMin * 100).toInt()}%-${(danceMax * 100).toInt()}%]"

        return VibeSqlBounds(
            bpmMin = bpmMin,
            bpmMax = bpmMax,
            energyMin = energyMin,
            energyMax = energyMax,
            danceMin = danceMin,
            danceMax = danceMax,
            targetKey = targetKey,
            promptSummary = summary
        )
    }

    /**
     * Camelot harmonic wheel compatibility score between 0.0 and 1.0
     * Compatible transitions: Exact match (1.0), Relative Major/Minor (0.95), +/- 1 semitone adjacent (0.90)
     */
    fun evaluateCamelotTransition(currentKey: String, nextKey: String): Pair<Double, String> {
        if (currentKey.isBlank() || nextKey.isBlank()) return Pair(0.5, "Neutral Transition")
        val curNum = currentKey.filter { it.isDigit() }.toIntOrNull() ?: return Pair(0.5, "Neutral Transition")
        val curLetter = currentKey.last().uppercaseChar()

        val nextNum = nextKey.filter { it.isDigit() }.toIntOrNull() ?: return Pair(0.5, "Neutral Transition")
        val nextLetter = nextKey.last().uppercaseChar()

        // Same harmonic root
        if (curNum == nextNum && curLetter == nextLetter) {
            return Pair(1.0, "Perfect Harmonic Lock (Exact Key)")
        }

        // Relative Major/Minor switch (same number, different letter A <-> B)
        if (curNum == nextNum && curLetter != nextLetter) {
            return Pair(0.95, "Relative Mode Modulation (${curLetter} to ${nextLetter})")
        }

        // Adjacent numbers on the Camelot wheel (modulo 12)
        val diff = (nextNum - curNum + 12) % 12
        if (curLetter == nextLetter) {
            if (diff == 1) return Pair(0.90, "Smooth Step Energy Lift (+1)")
            if (diff == 11) return Pair(0.85, "Smooth Step Energy Drop (-1)")
            if (diff == 2) return Pair(0.75, "Two-Step Energy Boost (+2)")
        }

        return Pair(0.40, "Dissonant / Key Clash Transition")
    }

    /**
     * Scores and ranks available tracks against parsed vibe rules and harmonic transitions
     */
    fun scoreAndRankTracks(
        tracks: List<Track>,
        rules: VibeSqlBounds,
        anchorKey: String? = null
    ): List<ScoredTrack> {
        val targetEnergy = (rules.energyMin + rules.energyMax) / 2.0
        val targetBpm = (rules.bpmMin + rules.bpmMax) / 2.0

        val scored = tracks.map { track ->
            val energyDelta = abs(track.energy - targetEnergy)
            val bpmDelta = abs(track.bpm - targetBpm)

            // BPM normalization penalty (over 40 bpm delta gives 0)
            val bpmScore = maxOf(0.0, 1.0 - (bpmDelta / 40.0))
            val energyScore = maxOf(0.0, 1.0 - energyDelta)
            val danceScore = if (track.danceability >= rules.danceMin) 1.0 else 0.5

            var harmonicScore = 0.7
            var harmonicNote = "General Transition"
            if (anchorKey != null) {
                val eval = evaluateCamelotTransition(anchorKey, track.key)
                harmonicScore = eval.first
                harmonicNote = eval.second
            } else if (rules.targetKey != null) {
                val eval = evaluateCamelotTransition(rules.targetKey, track.key)
                harmonicScore = eval.first
                harmonicNote = eval.second
            }

            // Weighted composite score
            val totalScore = (energyScore * 0.35) + (bpmScore * 0.30) + (danceScore * 0.15) + (harmonicScore * 0.20)

            ScoredTrack(
                track = track,
                score = totalScore,
                energyDelta = energyDelta,
                bpmDelta = bpmDelta,
                harmonicCompatibility = harmonicNote
            )
        }

        return scored.sortedByDescending { it.score }
    }

    /**
     * Synthesizes DJ liner notes for the generated mixtape
     */
    fun generateMixtapeLinerNotes(prompt: String, playlist: List<Track>): String {
        if (playlist.isEmpty()) return "No tracks matched the requested vibe parameters."
        val avgBpm = playlist.map { it.bpm }.average().toInt()
        val highResCount = playlist.count { it.sampleRate.contains("96") || it.sampleRate.contains("2.8") || it.bitDepth.contains("24") }

        return "Curated '$prompt' with ${playlist.size} tracks averaging $avgBpm BPM. " +
                "Includes $highResCount lossless hi-res master(s) with harmonic key sequencing."
    }

    /**
     * Optional Gemini Cloud Expansion:
     * When an API key is provided, synthesizes rich DJ liner notes and contextual nuances
     * via Google Gemini REST API, falling back 100% cleanly to local heuristics when offline or blank.
     */
    suspend fun synthesizeWithGemini(
        prompt: String,
        playlist: List<Track>,
        apiKey: String?
    ): String {
        if (apiKey.isNullOrBlank() || playlist.isEmpty()) {
            return generateMixtapeLinerNotes(prompt, playlist)
        }

        return withContext(Dispatchers.IO) {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.connectTimeout = 4000
                conn.readTimeout = 6000
                conn.doOutput = true

                val trackListSummary = playlist.take(8).joinToString(", ") {
                    "${it.title} by ${it.artist} (${it.bpm.toInt()} BPM, Key ${it.key})"
                }
                val promptText = "You are VibePlay AI, an audiophile DJ liner notes author. Write a short, stylish 2-sentence liner note for a playlist curated from prompt: '$prompt'. The playlist tracks are: $trackListSummary."

                val jsonPayload = """
                    {
                      "contents": [{
                        "parts": [{"text": ${JSONObject.quote(promptText)}}]
                      }]
                    }
                """.trimIndent()

                conn.outputStream.use { os ->
                    os.write(jsonPayload.toByteArray(Charsets.UTF_8))
                }

                if (conn.responseCode == 200) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseStr)
                    val text = json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                    text.trim()
                } else {
                    generateMixtapeLinerNotes(prompt, playlist)
                }
            } catch (_: Exception) {
                generateMixtapeLinerNotes(prompt, playlist)
            }
        }
    }
}
