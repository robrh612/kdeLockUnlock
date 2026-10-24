package com.example.kdelockunlock.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.Typography.LABEL_LARGE
import androidx.wear.protolayout.material3.button
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.kdelockunlock.presentation.MainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

private const val RESOURCES_VERSION = "1"

class MainTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        Futures.immediateFuture(tile(requestParams, this))

    override fun onTileResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> =
        Futures.immediateFuture(resources(requestParams))
}

private fun resources(requestParams: ResourcesRequest): Resources {
    return Resources.Builder()
        .setVersion(RESOURCES_VERSION)
        .build()
}

private fun tile(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
): TileBuilders.Tile {
    val launchIntent = ActionBuilders.launchAction(
        android.content.ComponentName(
            context.packageName,
            MainActivity::class.java.name
        )
    )

    val clickModifier = ModifiersBuilders.Clickable.Builder()
        .setOnClick(launchIntent)
        .build()

    return TileBuilders.Tile.Builder()
        .setResourcesVersion(RESOURCES_VERSION)
        .setTileTimeline(
            TimelineBuilders.Timeline.fromLayoutElement(
                materialScope(context, requestParams.deviceConfiguration) {
                    primaryLayout(
                        titleSlot = {
                            text(
                                "KDE PC Control".layoutString,
                                typography = LABEL_LARGE
                            )
                        },
                        mainSlot = {
                            LayoutElementBuilders.Column.Builder()
                                .addContent(
                                    button(
                                        onClick = clickModifier,
                                        labelContent = { text("🔒 Lock PC".layoutString) }
                                    )
                                )
                                .addContent(
                                    LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(8f)).build()
                                )
                                .addContent(
                                    button(
                                        onClick = clickModifier,
                                        labelContent = { text("🔓 Unlock PC".layoutString) }
                                    )
                                )
                                .build()
                        }
                    )
                }
            )
        )
        .build()
}

@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
fun tilePreview(context: Context) = TilePreviewData(::resources) {
    tile(it, context)
}
