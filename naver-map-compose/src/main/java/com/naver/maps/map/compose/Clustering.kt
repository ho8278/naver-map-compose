package com.naver.maps.map.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import com.naver.maps.map.clustering.ClusterMarkerInfo
import com.naver.maps.map.clustering.ClusterMarkerUpdater
import com.naver.maps.map.clustering.Clusterer
import com.naver.maps.map.clustering.ClusteringKey
import com.naver.maps.map.clustering.LeafMarkerInfo
import com.naver.maps.map.clustering.LeafMarkerUpdater
import com.naver.maps.map.overlay.Overlay

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

@Composable
internal fun rememberLeafMarkerUpdater(
    composeUiViewRenderer: ComposeUiViewRenderer,
    leafContent: @Composable (LeafMarkerInfo) -> Unit,
    onClickListener: (Overlay) -> Boolean,
): LeafMarkerUpdater {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val leafContent = rememberUpdatedState(leafContent)
    val onClickListener = rememberUpdatedState(onClickListener)
    return remember(leafContent, onClickListener) {
        ComposeLeafMarkerUpdater(
            context,
            composeUiViewRenderer,
            coroutineScope,
            leafContent,
            onClickListener,
        )
    }
}

@Composable
public fun <T : ClusteringKey> rememberClusterer(
    clusterContent: @Composable (ClusterMarkerInfo) -> Unit,
    leafContent: @Composable (LeafMarkerInfo) -> Unit,
    onClickListener: (Overlay) -> Boolean,
): Clusterer<T> {
    val composeUiRenderer = rememberComposeUiViewRenderer()
    val clusterMarkerUpdater =
        rememberClusterMarkerUpdater(composeUiRenderer, clusterContent, onClickListener)
    val leafMarkerUpdater =
        rememberLeafMarkerUpdater(composeUiRenderer, leafContent, onClickListener)
    return remember(clusterMarkerUpdater, leafMarkerUpdater) {
        Clusterer.ComplexBuilder<T>()
            .clusterMarkerUpdater(clusterMarkerUpdater)
            .leafMarkerUpdater(leafMarkerUpdater)
            .build()
    }
}

@OptIn(ExperimentalNaverMapApi::class)
@Composable
public fun <T : ClusteringKey> Clustering(
    items: Map<T, Any?>,
    clusterContent: @Composable (ClusterMarkerInfo) -> Unit,
    leafContent: @Composable (LeafMarkerInfo) -> Unit,
    onClickListener: (Overlay) -> Boolean,
) {
    val clusterer = rememberClusterer<T>(clusterContent, leafContent, onClickListener)
    val itemsState = rememberUpdatedState(items)

    LaunchedEffect(items) {
        snapshotFlow { itemsState.value }
            .collect {
                clusterer.clear()
                clusterer.addAll(it)
            }
    }
    MapEffect(clusterer) {
        clusterer.map = it
    }
}