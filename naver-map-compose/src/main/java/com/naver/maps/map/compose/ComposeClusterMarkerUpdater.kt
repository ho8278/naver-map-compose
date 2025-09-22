package com.naver.maps.map.compose

import android.content.Context
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import com.naver.maps.map.clustering.ClusterMarkerInfo
import com.naver.maps.map.clustering.ClusterMarkerUpdater
import com.naver.maps.map.clustering.DefaultClusterMarkerUpdater
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.OverlayImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.Closeable

@Composable
internal fun rememberClusterMarkerUpdater(
    composeUiViewRenderer: ComposeUiViewRenderer,
    clusterContent: @Composable (ClusterMarkerInfo) -> Unit,
    onClickListener: (Overlay) -> Boolean,
): ClusterMarkerUpdater {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clusterContent = rememberUpdatedState(clusterContent)
    val onClickListener = rememberUpdatedState(onClickListener)
    return remember(clusterContent, onClickListener) {
        ComposeClusterMarkerUpdater(
            context,
            composeUiViewRenderer,
            coroutineScope,
            clusterContent,
            onClickListener,
        )
    }
}

internal class ComposeClusterMarkerUpdater(
    private val context: Context,
    private val composeUiViewRenderer: ComposeUiViewRenderer,
    private val coroutineScope: CoroutineScope,
    private val clusterContent: State<@Composable ((ClusterMarkerInfo) -> Unit)?>,
    private val onClickListener: State<(Overlay) -> Boolean>,
) : DefaultClusterMarkerUpdater() {
    private val fakeCanvas = Canvas()

    override fun updateClusterMarker(
        info: ClusterMarkerInfo,
        marker: Marker,
    ) {
        super.updateClusterMarker(info, marker)

        val tag = marker.tag as? Closeable
        tag?.close()

        val view = InvalidatingComposeView(context, { clusterContent.value?.invoke(info) })
        val handle = composeUiViewRenderer.startRenderingView(view)
        val job = coroutineScope.launch {
            callbackFlow {
                var invalidated = false
                view.onInvalidate = {
                    if (!invalidated) {
                        launch {
                            awaitFrame()
                            trySend(Unit)
                            invalidated = false
                        }
                        invalidated = true
                    }
                }
                view.doOnAttach {
                    launch {
                        awaitFrame()
                        trySend(Unit)
                    }
                    view.doOnDetach { close() }
                }
                awaitClose()
            }
                .collectLatest {
                    marker.icon = OverlayImage.fromBitmap(renderViewToBitmap(fakeCanvas, view))
                    marker.onClickListener = Overlay.OnClickListener { overlay ->
                        onClickListener.value.invoke(overlay)
                    }
                }
        }

        marker.tag = Closeable {
            handle.dispose()
            job.cancel()
        }
    }
}