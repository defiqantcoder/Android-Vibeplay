package com.vibeplay.wear.complication

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.vibeplay.wear.R
import com.vibeplay.wear.WearSyncManager

class VibePlayComplicationService : ComplicationDataSourceService() {

    private lateinit var wearSyncManager: WearSyncManager

    override fun onCreate() {
        super.onCreate()
        wearSyncManager = WearSyncManager(this)
        wearSyncManager.register()
    }

    override fun onDestroy() {
        super.onDestroy()
        wearSyncManager.unregister()
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val title = PlainComplicationText.Builder("Midnight City Lights").build()
        val icon = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_wave)).build()

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(title, title)
                    .setMonochromaticImage(icon)
                    .build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(0.5f, 0.0f, 1.0f, title)
                    .setMonochromaticImage(icon)
                    .setText(title)
                    .build()
            }
            else -> null
        }
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        val state = wearSyncManager.playbackState.value
        val titleText = PlainComplicationText.Builder(state.title).build()
        val icon = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_wave)).build()

        val progress = if (state.durationMs > 0) {
            (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        val data: ComplicationData? = when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(titleText, titleText)
                    .setMonochromaticImage(icon)
                    .build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(progress, 0.0f, 1.0f, titleText)
                    .setMonochromaticImage(icon)
                    .setText(titleText)
                    .build()
            }
            else -> null
        }

        listener.onComplicationData(data)
    }
}
