package com.vibeplay.wear.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Button
import androidx.wear.protolayout.material.ButtonColors
import androidx.wear.protolayout.material.ChipColors
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.MultiButtonLayout
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.vibeplay.wear.R
import com.vibeplay.wear.WearSyncManager

class VibePlayTileService : TileService() {
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

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val state = wearSyncManager.playbackState.value

        val deviceConfig = requestParams.deviceConfiguration

        val titleText = Text.Builder(this, state.title)
            .setTypography(Typography.TYPOGRAPHY_TITLE2)
            .setColor(ColorBuilders.argb(0xFFFFFFFF.toInt()))
            .setMaxLines(1)
            .build()

        val artistText = Text.Builder(this, state.artist)
            .setTypography(Typography.TYPOGRAPHY_CAPTION1)
            .setColor(ColorBuilders.argb(0xFFB0B0B0.toInt()))
            .setMaxLines(1)
            .build()

        val badgeText = if (state.isBitPerfect) "Bit-Perfect" else "Atmos"
        val badgeClickable = ModifiersBuilders.Clickable.Builder()
            .setOnClick(ActionBuilders.launchAction(android.content.ComponentName(this, com.vibeplay.wear.WearMainActivity::class.java)))
            .build()
        val badgeChip = CompactChip.Builder(this, badgeText, badgeClickable, deviceConfig)
            .setChipColors(ChipColors.primaryChipColors(Colors(0xFF06B6D4.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt())))
            .build()

        val playPauseIcon = if (state.isPlaying) "ic_pause" else "ic_play"
        val playPauseClickable = ModifiersBuilders.Clickable.Builder()
            .setOnClick(ActionBuilders.LaunchAction.Builder().build())
            .build()

        val playPauseButton = Button.Builder(this, playPauseClickable)
            .setIconContent(playPauseIcon)
            .setButtonColors(ButtonColors.primaryButtonColors(Colors(0xFF06B6D4.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt())))
            .build()

        val nextClickable = ModifiersBuilders.Clickable.Builder()
            .setOnClick(ActionBuilders.LaunchAction.Builder().build())
            .build()

        val nextButton = Button.Builder(this, nextClickable)
            .setIconContent("ic_next")
            .setButtonColors(ButtonColors.secondaryButtonColors(Colors(0xFF333333.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt())))
            .build()

        val buttonsLayout = MultiButtonLayout.Builder()
            .addButtonContent(playPauseButton)
            .addButtonContent(nextButton)
            .build()

        val layout = PrimaryLayout.Builder(deviceConfig)
            .setPrimaryLabelTextContent(badgeChip)
            .setContent(
                LayoutElementBuilders.Column.Builder()
                    .addContent(titleText)
                    .addContent(artistText)
                    .build()
            )
            .setPrimaryChipContent(buttonsLayout)
            .build()

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(
                                        LayoutElementBuilders.Box.Builder()
                                            .setWidth(DimensionBuilders.expand())
                                            .setHeight(DimensionBuilders.expand())
                                            .setModifiers(
                                                ModifiersBuilders.Modifiers.Builder()
                                                    .setBackground(
                                                        ModifiersBuilders.Background.Builder()
                                                            .setColor(ColorBuilders.argb(0xFF121214.toInt()))
                                                            .build()
                                                    )
                                                    .build()
                                            )
                                            .addContent(layout)
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion("1")
                .addIdToImageMapping(
                    "ic_play",
                    ResourceBuilders.ImageResource.Builder()
                        .setAndroidResourceByResId(
                            ResourceBuilders.AndroidImageResourceByResId.Builder()
                                .setResourceId(R.drawable.ic_play)
                                .build()
                        )
                        .build()
                )
                .addIdToImageMapping(
                    "ic_pause",
                    ResourceBuilders.ImageResource.Builder()
                        .setAndroidResourceByResId(
                            ResourceBuilders.AndroidImageResourceByResId.Builder()
                                .setResourceId(R.drawable.ic_pause)
                                .build()
                        )
                        .build()
                )
                .addIdToImageMapping(
                    "ic_next",
                    ResourceBuilders.ImageResource.Builder()
                        .setAndroidResourceByResId(
                            ResourceBuilders.AndroidImageResourceByResId.Builder()
                                .setResourceId(R.drawable.ic_next)
                                .build()
                        )
                        .build()
                )
                .build()
        )
    }
}
