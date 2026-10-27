package com.vibeplay.core.model

object VibeSeedData {
    val INITIAL_USB_DAC_MODELS = listOf(
        UsbDacEndpoint(
            id = "fiio-ka15",
            name = "FiiO KA15 (Dual CS43198 Direct Bit-Perfect)",
            type = EndpointType.USB_DAC,
            maxSampleRate = "PCM 768kHz / 32-bit",
            dsdSupport = true,
            vendorId = 0x2972,
            productId = 0x0047,
            isConnected = true
        ),
        UsbDacEndpoint(
            id = "qudelix-5k",
            name = "Qudelix-5K Reference DAC/AMP",
            type = EndpointType.USB_DAC,
            maxSampleRate = "PCM 96kHz / 24-bit",
            dsdSupport = false,
            vendorId = 0x0a12,
            productId = 0x1004,
            isConnected = false
        ),
        UsbDacEndpoint(
            id = "moondrop-dawn",
            name = "Moondrop Dawn Pro High-Resolution",
            type = EndpointType.USB_DAC,
            maxSampleRate = "PCM 384kHz / 32-bit",
            dsdSupport = true,
            vendorId = 0x2fc6,
            productId = 0xf032,
            isConnected = false
        ),
        UsbDacEndpoint(
            id = "mojo-2",
            name = "Chord Mojo 2 (Custom FPGA Pulse Array)",
            type = EndpointType.USB_DAC,
            maxSampleRate = "PCM 768kHz / 32-bit",
            dsdSupport = true,
            vendorId = 0x2497,
            productId = 0x8001,
            isConnected = false
        ),
        UsbDacEndpoint(
            id = "internal-speaker",
            name = "Phone Speaker (System Mixer 48kHz SRC)",
            type = EndpointType.PHONE_SPEAKER,
            maxSampleRate = "PCM 48kHz / 16-bit",
            dsdSupport = false,
            isConnected = true
        )
    )

    val INITIAL_EQ_BANDS = listOf(
        EqBand("band-31", "31 Hz", 31.0, 0.0, 1.41, FilterTopology.LOW_SHELF),
        EqBand("band-63", "63 Hz", 63.0, 1.5, 1.41, FilterTopology.BELL_PEAK),
        EqBand("band-125", "125 Hz", 125.0, 0.5, 1.41, FilterTopology.BELL_PEAK),
        EqBand("band-250", "250 Hz", 250.0, -1.0, 1.41, FilterTopology.BELL_PEAK),
        EqBand("band-500", "500 Hz", 500.0, 0.0, 1.41, FilterTopology.BELL_PEAK),
        EqBand("band-1k", "1.0 kHz", 1000.0, 1.2, 1.41, FilterTopology.BELL_PEAK),
        EqBand("band-2k5", "2.5 kHz", 2500.0, 2.0, 1.41, FilterTopology.BELL_PEAK),
        EqBand("band-5k", "5.0 kHz", 5000.0, 1.8, 1.41, FilterTopology.BELL_PEAK),
        EqBand("band-8k", "8.0 kHz", 8000.0, 0.5, 1.41, FilterTopology.BELL_PEAK),
        EqBand("band-16k", "16 kHz", 16000.0, 0.0, 1.41, FilterTopology.HIGH_SHELF)
    )

    val EQ_PRESETS = listOf(
        EqPreset("flat", "Flat Reference", listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0), "Bypasses coloration"),
        EqPreset("warm-tape", "Warm Tape Saturation", listOf(2.5, 2.0, 1.0, 0.5, 0.0, -0.5, -1.0, -1.5, -2.0, -3.0), "Analog warmth with soft treble roll-off"),
        EqPreset("bass-punch", "Club Bass Punch", listOf(4.5, 4.0, 2.5, 0.5, -1.0, 0.0, 1.0, 2.0, 1.5, 0.5), "Enhanced sub-bass and kick slam"),
        EqPreset("vocal-sparkle", "Vocal Air & Sparkle", listOf(-1.0, -0.5, 0.0, 0.5, 1.5, 2.8, 3.2, 2.5, 1.8, 1.0), "Pushes vocals to the front of the stage"),
        EqPreset("acoustic-clean", "Acoustic Clarity", listOf(0.5, 0.0, -1.0, -0.5, 1.0, 1.5, 2.0, 2.5, 2.0, 1.5), "Unmasks guitars, strings, and delicate room reverb"),
        EqPreset("harman-target", "Harman In-Ear Target", listOf(4.0, 3.5, 1.5, 0.0, -0.5, 1.0, 2.5, 3.0, 1.0, -1.0), "Neutral reference target with sub-bass boost")
    )

    val INITIAL_CRATES = listOf(
        CrateFolder("crate-lofi", "Midnight Lo-Fi Coffee", 18, "Chill Vibe", "Just now"),
        CrateFolder("crate-synth", "Neon Tokyo Cyberpunk", 24, "Steady Groove", "Yesterday"),
        CrateFolder("crate-cardio", "Sub-Zero Cardio 140+", 15, "Peak Energy", "3 days ago"),
        CrateFolder("crate-audiophile", "DSD Master Bit-Perfect", 12, "Rising Ramp", "1 week ago")
    )

    val INITIAL_TRACKS = listOf(
        Track(
            id = "t-1",
            title = "Clint Eastwood (Master Lossless)",
            artist = "Gorillaz",
            album = "Gorillaz (Remastered)",
            duration = "5:42",
            durationMs = 342000L,
            bpm = 84,
            key = "8B",
            energy = 0.58,
            danceability = 0.72,
            crateId = "crate-lofi",
            format = "FLAC",
            sampleRate = "96.0 kHz",
            bitDepth = "24-bit",
            waveforms = listOf(0.2f, 0.4f, 0.6f, 0.8f, 0.5f, 0.9f, 0.7f, 0.4f, 0.3f, 0.6f, 0.8f, 0.7f, 0.5f, 0.3f, 0.4f, 0.7f, 0.9f, 0.6f, 0.5f, 0.4f, 0.6f, 0.8f, 0.5f, 0.3f),
            lyrics = listOf(
                LyricLine(0.0, "[Intro - Melodica Instrumental]"),
                LyricLine(12.4, "I ain't happy, I'm feeling glad"),
                LyricLine(16.8, "I got sunshine in a bag"),
                LyricLine(21.2, "I'm useless, but not for long"),
                LyricLine(25.5, "The future is coming on"),
                LyricLine(34.2, "Finally, someone let me out of my cage"),
                LyricLine(38.0, "Now time for me is nothing 'cause I'm counting no age")
            )
        ),
        Track(
            id = "t-2",
            title = "Ocean Avenue (Acoustic FLAC)",
            artist = "Yellowcard",
            album = "Ocean Avenue (Acoustic)",
            duration = "3:38",
            durationMs = 218000L,
            bpm = 135,
            key = "9B",
            energy = 0.85,
            danceability = 0.64,
            crateId = "crate-cardio",
            format = "FLAC",
            sampleRate = "44.1 kHz",
            bitDepth = "16-bit",
            waveforms = listOf(0.3f, 0.6f, 0.8f, 0.9f, 0.8f, 0.9f, 0.7f, 0.6f, 0.8f, 0.9f, 0.7f, 0.8f, 0.6f, 0.7f, 0.9f, 0.8f, 0.6f, 0.5f, 0.8f, 0.9f, 0.7f, 0.6f, 0.8f, 0.5f),
            lyrics = listOf(
                LyricLine(0.0, "[Violin & Acoustic Intro]"),
                LyricLine(10.2, "There's a place off Ocean Avenue"),
                LyricLine(14.5, "Where I used to sit and talk with you"),
                LyricLine(19.0, "We were both 16 and it felt so right"),
                LyricLine(23.4, "Sleeping all day, staying up all night")
            )
        ),
        Track(
            id = "t-3",
            title = "Clarity feat. Foxes (Live Studio)",
            artist = "Zedd",
            album = "Clarity (Deluxe)",
            duration = "4:31",
            durationMs = 271000L,
            bpm = 128,
            key = "8B",
            energy = 0.88,
            danceability = 0.79,
            crateId = "crate-cardio",
            format = "FLAC",
            sampleRate = "48.0 kHz",
            bitDepth = "24-bit",
            waveforms = listOf(0.1f, 0.3f, 0.5f, 0.7f, 0.9f, 1.0f, 0.8f, 0.9f, 0.8f, 0.9f, 1.0f, 0.7f, 0.4f, 0.8f, 1.0f, 0.9f, 0.7f, 0.8f, 0.9f, 0.6f, 0.4f, 0.7f, 0.9f, 0.5f),
            lyrics = listOf(
                LyricLine(0.0, "[Atmospheric Synth Pad]"),
                LyricLine(8.0, "High dive into frozen waves where the past comes back to life"),
                LyricLine(15.2, "Fight fear for the very first time"),
                LyricLine(22.8, "'Cause you are the piece of me I wish I didn't need"),
                LyricLine(30.0, "Chasing relentlessly, still fight and I don't know why")
            )
        ),
        Track(
            id = "t-4",
            title = "Feel So Close (Radio Edit)",
            artist = "Calvin Harris",
            album = "18 Months",
            duration = "3:26",
            durationMs = 206000L,
            bpm = 128,
            key = "8A",
            energy = 0.84,
            danceability = 0.81,
            crateId = "crate-synth",
            format = "FLAC",
            sampleRate = "44.1 kHz",
            bitDepth = "16-bit",
            waveforms = listOf(0.4f, 0.5f, 0.7f, 0.8f, 0.9f, 0.8f, 0.9f, 0.8f, 0.7f, 0.9f, 0.8f, 0.7f, 0.8f, 0.9f, 0.7f, 0.6f, 0.8f, 0.9f, 0.7f, 0.6f, 0.8f, 0.7f, 0.5f, 0.3f),
            lyrics = listOf(
                LyricLine(0.0, "[Acoustic Strum Intro]"),
                LyricLine(7.5, "I feel so close to you right now"),
                LyricLine(11.2, "It's a force field"),
                LyricLine(15.0, "I wear my heart upon my sleeve, like a big deal")
            )
        ),
        Track(
            id = "t-5",
            title = "Can You Feel My Heart (Audiophile Master)",
            artist = "Bring Me The Horizon",
            album = "Sempiternal",
            duration = "3:47",
            durationMs = 227000L,
            bpm = 128,
            key = "7A",
            energy = 0.92,
            danceability = 0.54,
            crateId = "crate-audiophile",
            format = "FLAC",
            sampleRate = "96.0 kHz",
            bitDepth = "24-bit",
            waveforms = listOf(0.2f, 0.7f, 0.9f, 1.0f, 0.9f, 1.0f, 0.8f, 0.9f, 1.0f, 0.8f, 0.9f, 1.0f, 0.7f, 0.8f, 0.9f, 1.0f, 0.8f, 0.7f, 0.9f, 1.0f, 0.8f, 0.6f, 0.4f, 0.2f),
            lyrics = listOf(
                LyricLine(0.0, "Can you hear the silence?"),
                LyricLine(3.2, "Can you see the dark?"),
                LyricLine(6.5, "Can you fix the broken?"),
                LyricLine(9.8, "Can you feel, can you feel my heart?")
            )
        ),
        Track(
            id = "t-6",
            title = "In The End (Lossless Acoustic)",
            artist = "Black Veil Brides",
            album = "Wretched and Divine",
            duration = "3:48",
            durationMs = 228000L,
            bpm = 142,
            key = "9A",
            energy = 0.89,
            danceability = 0.48,
            crateId = "crate-cardio",
            format = "FLAC",
            sampleRate = "48.0 kHz",
            bitDepth = "24-bit",
            waveforms = listOf(0.3f, 0.5f, 0.8f, 0.9f, 0.9f, 0.8f, 0.9f, 0.7f, 0.8f, 0.9f, 0.8f, 0.7f, 0.8f, 0.9f, 0.7f, 0.8f, 0.9f, 0.8f, 0.7f, 0.6f, 0.8f, 0.7f, 0.4f, 0.2f),
            lyrics = listOf(
                LyricLine(0.0, "[Acoustic Intro]"),
                LyricLine(8.2, "In the end, as you fade into the night"),
                LyricLine(12.6, "Who will tell the story of your life?")
            )
        ),
        Track(
            id = "t-7",
            title = "More Than a Feeling (DSD 2.8MHz Remaster)",
            artist = "Boston",
            album = "Boston",
            duration = "4:45",
            durationMs = 285000L,
            bpm = 109,
            key = "9B",
            energy = 0.78,
            danceability = 0.61,
            crateId = "crate-audiophile",
            format = "DSD",
            sampleRate = "2.8 MHz",
            bitDepth = "1-bit",
            waveforms = listOf(0.2f, 0.4f, 0.6f, 0.8f, 0.9f, 0.8f, 0.7f, 0.8f, 0.9f, 0.7f, 0.6f, 0.8f, 0.9f, 0.8f, 0.7f, 0.8f, 0.9f, 0.8f, 0.7f, 0.5f, 0.6f, 0.4f, 0.3f, 0.2f),
            lyrics = listOf(
                LyricLine(0.0, "[12-String Acoustic Arpeggio]"),
                LyricLine(14.0, "I looked out this morning and the sun was gone"),
                LyricLine(21.2, "Turned on some music to start my day"),
                LyricLine(28.0, "I lost myself in a familiar song"),
                LyricLine(35.5, "I closed my eyes and I slipped away")
            )
        ),
        Track(
            id = "t-8",
            title = "All Over You (Pacific Lo-Fi Session)",
            artist = "LEISURE",
            album = "Sunsetter",
            duration = "3:18",
            durationMs = 198000L,
            bpm = 92,
            key = "6A",
            energy = 0.45,
            danceability = 0.74,
            crateId = "crate-lofi",
            format = "FLAC",
            sampleRate = "48.0 kHz",
            bitDepth = "24-bit",
            waveforms = listOf(0.1f, 0.2f, 0.4f, 0.5f, 0.6f, 0.5f, 0.4f, 0.6f, 0.5f, 0.4f, 0.5f, 0.6f, 0.5f, 0.4f, 0.5f, 0.6f, 0.5f, 0.4f, 0.3f, 0.4f, 0.5f, 0.4f, 0.2f, 0.1f),
            lyrics = listOf(
                LyricLine(0.0, "[Rhodes Electric Piano Intro]"),
                LyricLine(9.5, "When I'm with you, nothing else is on my mind"),
                LyricLine(17.0, "Watching the sunset, taking all our time")
            )
        ),
        Track(
            id = "t-9",
            title = "Digital Love (French Touch Master)",
            artist = "Daft Punk",
            album = "Discovery",
            duration = "4:58",
            durationMs = 298000L,
            bpm = 125,
            key = "7B",
            energy = 0.81,
            danceability = 0.86,
            crateId = "crate-synth",
            format = "FLAC",
            sampleRate = "96.0 kHz",
            bitDepth = "24-bit",
            waveforms = listOf(0.2f, 0.4f, 0.6f, 0.8f, 0.9f, 0.8f, 0.7f, 0.8f, 0.9f, 0.8f, 0.7f, 0.8f, 0.9f, 0.8f, 0.7f, 0.8f, 0.9f, 0.8f, 0.7f, 0.6f, 0.8f, 0.7f, 0.5f, 0.3f),
            lyrics = listOf(
                LyricLine(0.0, "[Wurlitzer Electric Piano Chords]"),
                LyricLine(12.5, "Last night I had a dream about you"),
                LyricLine(18.2, "In this dream I'm dancing right beside you"),
                LyricLine(24.0, "And it looked like everyone was having fun"),
                LyricLine(29.8, "The kind of feeling I've waited so long")
            )
        )
    )

    val INITIAL_ADDONS = listOf(
        AddonPackage(
            id = "addon-peq",
            name = "Studio 10-Band Biquad PEQ",
            version = "2.4.0",
            author = "VibeDSP Audio Labs",
            description = "High-precision Robert Bristow-Johnson biquad parametric filtering engine with live phase response and zero phase distortion.",
            category = "DSP Effects",
            installed = true,
            enabled = true,
            dspImpact = "Low (<1.2%)",
            latencyMs = 0.35
        ),
        AddonPackage(
            id = "addon-dedup",
            name = "Lossless Music Deduplicator",
            version = "1.8.2",
            author = "Audiophile Tools",
            description = "Scans local and remote crates for duplicate titles, identifying higher bit-depth and sample-rate audio files to archive lossy formats.",
            category = "Library Management",
            installed = true,
            enabled = true,
            dspImpact = "None",
            latencyMs = 0.0
        ),
        AddonPackage(
            id = "addon-kodi",
            name = "Kodi & UPnP Network Streamer",
            version = "3.1.0",
            author = "OpenSource Hifi",
            description = "Broadcasts bit-perfect PCM streams to local home network Kodi, Volumio, and DLNA endpoints with zero transcoding.",
            category = "Network Streaming",
            installed = false,
            enabled = false,
            dspImpact = "Moderate (2.4%)",
            latencyMs = 45.0
        ),
        AddonPackage(
            id = "addon-artvault",
            name = "Lossless Art Vault & Hi-Res Canvas",
            version = "1.0.4",
            author = "VibePlay Labs",
            description = "Fetches and caches up to 4096x4096px embedded ID3v2/FLAC album artwork with dynamic palette extraction.",
            category = "Visuals & UI",
            installed = true,
            enabled = true,
            dspImpact = "Negligible",
            latencyMs = 0.0
        )
    )

    val INITIAL_DUPLICATE_GROUPS = listOf(
        DuplicateGroup(
            id = "dup-1",
            canonicalTitle = "Clint Eastwood",
            artist = "Gorillaz",
            candidates = listOf(
                DuplicateCandidate(
                    trackId = "t-1",
                    title = "Clint Eastwood (Master Lossless)",
                    artist = "Gorillaz",
                    album = "Gorillaz (Remastered)",
                    format = "FLAC",
                    sampleRate = "96.0 kHz",
                    bitDepth = "24-bit",
                    durationMs = 342000L,
                    isBestQuality = true
                ),
                DuplicateCandidate(
                    trackId = "t-1-mp3",
                    title = "Clint Eastwood (Original Rip)",
                    artist = "Gorillaz",
                    album = "Gorillaz",
                    format = "MP3",
                    sampleRate = "44.1 kHz",
                    bitDepth = "16-bit (320kbps)",
                    durationMs = 340000L,
                    isBestQuality = false
                )
            )
        ),
        DuplicateGroup(
            id = "dup-2",
            canonicalTitle = "Ocean Avenue",
            artist = "Yellowcard",
            candidates = listOf(
                DuplicateCandidate(
                    trackId = "t-2",
                    title = "Ocean Avenue (Acoustic FLAC)",
                    artist = "Yellowcard",
                    album = "Ocean Avenue (Acoustic)",
                    format = "FLAC",
                    sampleRate = "44.1 kHz",
                    bitDepth = "16-bit",
                    durationMs = 218000L,
                    isBestQuality = true
                ),
                DuplicateCandidate(
                    trackId = "t-2-aac",
                    title = "Ocean Avenue",
                    artist = "Yellowcard",
                    album = "Ocean Avenue",
                    format = "AAC",
                    sampleRate = "44.1 kHz",
                    bitDepth = "256kbps",
                    durationMs = 217000L,
                    isBestQuality = false
                )
            )
        )
    )
}
