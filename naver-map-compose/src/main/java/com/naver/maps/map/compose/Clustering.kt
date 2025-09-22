package com.naver.maps.map.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import com.naver.maps.map.clustering.ClusterMarkerInfo
import com.naver.maps.map.clustering.Clusterer
import com.naver.maps.map.clustering.ClusteringKey
import com.naver.maps.map.clustering.LeafMarkerInfo
import com.naver.maps.map.overlay.Overlay

@Composable
public fun <T : ClusteringKey> rememberClusterer(
    clusterContent: @Composable (ClusterMarkerInfo) -> Unit,
    onClickCluster: (ClusterMarkerInfo, Overlay) -> Boolean,
    leafContent: @Composable (LeafMarkerInfo) -> Unit,
    onClickLeaf: (LeafMarkerInfo, Overlay) -> Boolean,
): Clusterer<T> {
    val context = LocalContext.current
    val composeUiRenderer = rememberComposeUiViewRenderer()
    val coroutineScope = rememberCoroutineScope()
    val clusterContent = rememberUpdatedState(clusterContent)
    val onClickCluster = rememberUpdatedState(onClickCluster)
    val leafContent = rememberUpdatedState(leafContent)
    val onClickLeaf = rememberUpdatedState(onClickLeaf)
    val composeClusterMarkerUpdater = remember(
        context, composeUiRenderer, clusterContent, onClickCluster, leafContent, onClickLeaf
    ) {
        ComposeClusterMarkerUpdater(
            context,
            composeUiRenderer,
            coroutineScope,
            clusterContent,
            onClickCluster,
            leafContent,
            onClickLeaf
        )
    }
    return remember(composeClusterMarkerUpdater) {
        Clusterer.ComplexBuilder<T>()
            .clusterMarkerUpdater(composeClusterMarkerUpdater)
            .leafMarkerUpdater(composeClusterMarkerUpdater)
            .build()
    }
}

@OptIn(ExperimentalNaverMapApi::class)
@Composable
public fun <T : ClusteringKey> Clustering(
    items: Map<T, Any?>,
    clusterContent: @Composable (ClusterMarkerInfo) -> Unit,
    onClickCluster: (ClusterMarkerInfo, Overlay) -> Boolean,
    leafContent: @Composable (LeafMarkerInfo) -> Unit,
    onClickLeaf: (LeafMarkerInfo, Overlay) -> Boolean,
) {
    val clusterer = rememberClusterer<T>(clusterContent, onClickCluster, leafContent, onClickLeaf)
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