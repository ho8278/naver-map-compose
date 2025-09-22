package com.naver.maps.map.compose

import android.content.Context
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import com.naver.maps.map.clustering.ClusterMarkerInfo
import com.naver.maps.map.clustering.DefaultClusterMarkerUpdater
import com.naver.maps.map.clustering.DefaultLeafMarkerUpdater
import com.naver.maps.map.clustering.LeafMarkerInfo
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

internal class ComposeLeafMarkerUpdater(
    private val context: Context,
    private val composeUiViewRenderer: ComposeUiViewRenderer,
    private val coroutineScope: CoroutineScope,
    private val clusterContent: State<@Composable ((LeafMarkerInfo) -> Unit)?>,
    private val onClickListener: State<(Overlay) -> Boolean>,
) : DefaultLeafMarkerUpdater() {
    private val fakeCanvas = Canvas()

    override fun updateLeafMarker(
        info: LeafMarkerInfo,
        marker: Marker,
    ) {

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
