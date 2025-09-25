package com.naver.maps.map.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.UiComposable
import androidx.compose.ui.platform.LocalContext
import com.naver.maps.geometry.WebMercatorCoord
import com.naver.maps.map.clustering.Cluster
import com.naver.maps.map.clustering.ClusterMarkerInfo
import com.naver.maps.map.clustering.Clusterer
import com.naver.maps.map.clustering.ClusteringKey
import com.naver.maps.map.clustering.LeafMarkerInfo
import com.naver.maps.map.clustering.Node
import com.naver.maps.map.overlay.Overlay

@Composable
public fun <T : ClusteringKey> rememberClusterer(
    clusterContent: @Composable (ClusterMarkerInfo) -> Unit,
    onClickCluster: (ClusterMarkerInfo, Overlay) -> Boolean,
    leafContent: @Composable (LeafMarkerInfo) -> Unit,
    onClickLeaf: (LeafMarkerInfo, Overlay) -> Boolean,
    thresholdStrategy: ((zoom: Int) -> Double)? = null,
    distanceStrategy: ((zoom: Int, node1: Node, node2: Node) -> Double)? = null,
    tagMergeStrategy: ((Cluster) -> Any?)? = null,
    positionStrategy: ((Cluster) -> WebMercatorCoord)? = null,
    maxScreenDistance: Double? = null,
    animationDuration: Int? = null,
    maxClusteringZoom: Int? = null,
    minClusteringZoom: Int? = null,
    maxIndexingZoom: Int? = null,
    minIndexingZoom: Int? = null,
    updateOnChange: Boolean = false,
): Clusterer<T> {
    val context = LocalContext.current
    val composeUiRenderer = rememberComposeUiViewRenderer()
    val coroutineScope = rememberCoroutineScope()
    val clusterContent = rememberUpdatedState(clusterContent)
    val onClickCluster = rememberUpdatedState(onClickCluster)
    val leafContent = rememberUpdatedState(leafContent)
    val onClickLeaf = rememberUpdatedState(onClickLeaf)
    val thresholdStrategyState by rememberUpdatedState(thresholdStrategy)
    val distanceStrategyState by rememberUpdatedState(distanceStrategy)
    val tagMergeStrategyState by rememberUpdatedState(tagMergeStrategy)
    val positionStrategyState by rememberUpdatedState(positionStrategy)
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
    val markerManager = remember { ComposeClusterMarkerManager() }
    return remember(
        composeClusterMarkerUpdater,
        markerManager,
        thresholdStrategyState,
        distanceStrategyState,
        maxScreenDistance,
        tagMergeStrategyState,
        positionStrategyState,
        animationDuration,
        maxClusteringZoom,
        minClusteringZoom,
        maxIndexingZoom,
        minIndexingZoom,
        updateOnChange,
    ) {
        Clusterer.ComplexBuilder<T>()
            .clusterMarkerUpdater(composeClusterMarkerUpdater)
            .leafMarkerUpdater(composeClusterMarkerUpdater)
            .markerManager(markerManager)
            .apply {
                if (thresholdStrategyState != null) {
                    thresholdStrategy(thresholdStrategyState!!)
                }
                if (distanceStrategyState != null) {
                    distanceStrategy(distanceStrategyState!!)
                }
                if (tagMergeStrategyState != null) {
                    tagMergeStrategy(tagMergeStrategyState!!)
                }
                if (positionStrategyState != null) {
                    positioningStrategy(positionStrategyState!!)
                }
                if (maxScreenDistance != null) {
                    this.maxScreenDistance(maxScreenDistance)
                }
                if (animationDuration != null) {
                    this.animationDuration(animationDuration)
                }
                if (maxClusteringZoom != null) {
                    this.maxClusteringZoom(maxClusteringZoom)
                }
                if (minClusteringZoom != null) {
                    this.minClusteringZoom(minClusteringZoom)
                }
                if (maxIndexingZoom != null) {
                    this.maxIndexingZoom(maxIndexingZoom)
                }
                if (minIndexingZoom != null) {
                    this.minIndexingZoom(minIndexingZoom)
                }
            }
            .updateOnChange(updateOnChange)
            .build()
    }
}

@OptIn(ExperimentalNaverMapApi::class)
@Composable
@NaverMapComposable
public fun <T : ClusteringKey> Clustering(
    items: Map<T, Any?>,
    clusterContent: @[UiComposable Composable] (ClusterMarkerInfo) -> Unit,
    onClickCluster: (ClusterMarkerInfo, Overlay) -> Boolean,
    leafContent: @[UiComposable Composable] (LeafMarkerInfo) -> Unit,
    onClickLeaf: (LeafMarkerInfo, Overlay) -> Boolean,
    thresholdStrategy: ((zoom: Int) -> Double)? = null,
    distanceStrategy: ((zoom: Int, node1: Node, node2: Node) -> Double)? = null,
    tagMergeStrategy: ((Cluster) -> Any?)? = null,
    positionStrategy: ((Cluster) -> WebMercatorCoord)? = null,
    maxScreenDistance: Double? = null,
    animationDuration: Int? = null,
    maxClusteringZoom: Int? = null,
    minClusteringZoom: Int? = null,
    maxIndexingZoom: Int? = null,
    minIndexingZoom: Int? = null,
    updateOnChange: Boolean = false,
) {
    val clusterer = rememberClusterer<T>(
        clusterContent,
        onClickCluster,
        leafContent,
        onClickLeaf,
        thresholdStrategy,
        distanceStrategy,
        tagMergeStrategy,
        positionStrategy,
        maxScreenDistance,
        animationDuration,
        maxClusteringZoom,
        minClusteringZoom,
        maxIndexingZoom,
        minIndexingZoom,
        updateOnChange,
    )
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