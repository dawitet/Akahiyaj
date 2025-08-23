package com.dawitf.akahidegn

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.dawitf.akahidegn.R
import com.dawitf.akahidegn.ui.components.CreateRideDialog
import com.dawitf.akahidegn.viewmodel.MainViewModel
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import android.util.Log
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer // Added for clustering
import android.graphics.Bitmap // For potential cluster icon customization
import android.graphics.Canvas // For potential cluster icon customization
import android.graphics.Color // For potential cluster icon customization
import android.graphics.Paint // For potential cluster icon customization
import android.graphics.drawable.BitmapDrawable // For potential cluster icon customization
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider

@Composable
fun MapScreen(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val resources = context.resources
    val mapView = rememberMapViewWithLifecycle(mainViewModel)
    // groupMarkers map can still be useful for direct access if needed, e.g., for info window management outside clustering
    val groupMarkers = remember { mutableStateMapOf<String, Marker>() }

    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var selectedGeoPointForCreation by remember { mutableStateOf<GeoPoint?>(null) }
    var destinationInput by remember { mutableStateOf("") }

    val userIconDrawable: Drawable? = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_user_location)
    }

    val groupIconDrawable: Drawable? = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_custom_group_marker)
    }

    // Initialize RadiusMarkerClusterer
    val groupClusterer = remember {
        RadiusMarkerClusterer(context).apply {
            setRadius(100) // Cluster radius in pixels, adjust as needed

            // Optional: Customize the cluster icon appearance
            // This creates a simple text-based cluster icon. You can use a Bitmap too.
            val clusterIconBitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(clusterIconBitmap)
            val paint = Paint().apply {
                color = Color.BLUE // Example color
                style = Paint.Style.FILL
            }
            canvas.drawCircle(24f, 24f, 24f, paint)
            paint.color = Color.WHITE
            paint.textSize = 24f
            paint.textAlign = Paint.Align.CENTER
            // Text drawing logic within RadiusMarkerClusterer is a bit more complex as it draws the count.
            // This basic bitmap is a placeholder. A common approach is to provide a nine-patch drawable or a more sophisticated custom drawable.
            // For simplicity, we'll let RadiusMarkerClusterer use its default text-based icon or you can design a drawable.
            // setIcon(BitmapDrawable(resources, clusterIconBitmap)) // Example of setting a custom bitmap icon
        }
    }

    // Add clusterer to map overlays (once)
    LaunchedEffect(mapView, groupClusterer) {
        if (!mapView.overlays.contains(groupClusterer)) {
            mapView.overlays.add(groupClusterer)
        }
    }

    LaunchedEffect(mapView, userIconDrawable) {
        mainViewModel.currentLocation.collect { androidLocation ->
            androidLocation?.let {
                val userGeoPoint = GeoPoint(it.latitude, it.longitude)
                // userMarker logic removed as MyLocationNewOverlay is used
                if (mainViewModel.initialMapLatitudeValue == null) {
                    mapView.controller.animateTo(userGeoPoint)
                    mapView.controller.setZoom(16.0)
                }
                mapView.invalidate() // Invalidate map for user marker updates
            }
        }
    }

    LaunchedEffect(mapView, groupIconDrawable, groupClusterer) {
        mainViewModel.mainGroups.collect { groups ->
            // Clear previous markers from the clusterer for a fresh update
            groupClusterer.items.clear()
            groupMarkers.clear() // Clear our tracking map as well

            groups.forEach { group ->
                group.groupId?.let { id ->
                    if (group.pickupLat != null && group.pickupLng != null) {
                        val groupGeoPoint = GeoPoint(group.pickupLat!!, group.pickupLng!!)
                        val newMarker = Marker(mapView).apply {
                            position = groupGeoPoint
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = group.destinationName ?: "Group Location"
                            snippet = "Members: ${group.memberCount}/${group.maxMembers}"
                            icon = groupIconDrawable
                            // InfoWindow will be managed by the clusterer if it's set up to show them
                            // or by individual markers if they are not clustered.
                            setOnMarkerClickListener { marker, _ ->
                                if (marker.isInfoWindowShown) {
                                    marker.closeInfoWindow()
                                } else {
                                    // Close other info windows before showing a new one
                                    // This might need adjustment with clustering behavior
                                    groupClusterer.items.forEach { it.closeInfoWindow() }
                                    // userMarker?.closeInfoWindow() // userMarker logic removed
                                    marker.showInfoWindow()
                                }
                                true
                            }
                        }
                        groupClusterer.add(newMarker) // Add to clusterer
                        groupMarkers[id] = newMarker // Keep in tracking map if needed for other logic
                    }
                }
            }
            groupClusterer.invalidate() // Tell clusterer to re-evaluate and draw clusters
            mapView.invalidate() // Redraw the map
        }
    }

    // MyLocationNewOverlay for user location
    val myLocationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
            enableFollowLocation()
        }
    }
    // Add overlay to map if not already present
    LaunchedEffect(mapView, myLocationOverlay) {
        if (!mapView.overlays.contains(myLocationOverlay)) {
            mapView.overlays.add(myLocationOverlay)
        }
    }

    // MapEventsOverlay for long press to create group (should be added before clusterer if it needs to intercept touch)
    LaunchedEffect(mapView) {
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                // Close all info windows on map tap
                groupClusterer.items.forEach { it.closeInfoWindow() }
                // userMarker?.closeInfoWindow() // userMarker logic removed
                return false // Continue processing (e.g. for marker tap)
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                p?.let {
                    selectedGeoPointForCreation = it
                    showCreateGroupDialog = true
                }
                return true // Consumed
            }
        }
        val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
        // Add MapEventsOverlay before the clusterer or other overlays that might consume taps
        if (!mapView.overlays.contains(mapEventsOverlay)) {
             // Ensure it's typically one of the first overlays to receive events
            mapView.overlays.add(0, mapEventsOverlay) 
        }
        mapView.invalidate()
    }

    if (showCreateGroupDialog && selectedGeoPointForCreation != null) {
        CreateRideDialog(
            destinationInput = destinationInput,
            onDestinationChange = { destinationInput = it },
            onDismissRequest = {
                showCreateGroupDialog = false
                destinationInput = ""
            },
            onConfirm = { destinationName ->
                val geoPoint = selectedGeoPointForCreation!!
                // Construct a Group object (fill in other required fields as needed)
                val group = Group(
                    groupId = null, // Let backend assign
                    destinationName = destinationName,
                    pickupLat = geoPoint.latitude,
                    pickupLng = geoPoint.longitude,
                    memberCount = 1,
                    maxMembers = 4, // Default, or prompt for this if needed
                    // ...other fields as required by your Group data class...
                )
                mainViewModel.createGroup(group, "anonymous") // Replace with real user ID if available
                showCreateGroupDialog = false
                destinationInput = ""
            }
        )
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            if (mapView.mapCenter != null) {
                mainViewModel.saveMapState(
                    mapView.mapCenter.latitude,
                    mapView.mapCenter.longitude,
                    mapView.zoomLevelDouble
                )
            }
            // Clean up OSMdroid resources
            mapView.onDetach()
        }
    }
}

@Composable
fun rememberMapViewWithLifecycle(mainViewModel: MainViewModel): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            id = ViewCompat.generateViewId()
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)

            // Use currentLocation if available, else default to Addis Ababa
            val location = mainViewModel.currentLocation.value
            if (location != null) {
                controller.setZoom(16.0)
                controller.setCenter(GeoPoint(location.latitude, location.longitude))
                Log.d("MapScreen", "Map state: Centered on user location.")
            } else {
                controller.setZoom(10.0)
                controller.setCenter(GeoPoint(9.03, 38.74)) // Default: Addis Ababa
                Log.d("MapScreen", "Map state: Using default initial state.")
            }
        }
    }

    val lifecycleObserver = remember {
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
            // mapView.onDetach() already called in the main MapScreen DisposableEffect
        }
    }
    return mapView
}
