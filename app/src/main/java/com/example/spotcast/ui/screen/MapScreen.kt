package com.example.spotcast.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spotcast.R
import com.example.spotcast.ui.viewmodel.MapViewModel
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onLogout: () -> Unit,
    onSettings: () -> Unit,
    onCapsuleList: () -> Unit,
    viewModel: MapViewModel = viewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val capsules by viewModel.capsules.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val activeLayers by viewModel.activeLayers.collectAsState()
    val availableLayers by viewModel.availableLayers.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var showLayersSheet by remember { mutableStateOf(false) }
    var selectedLatLng by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var isFirstLocation by remember { mutableStateOf(true) }
    var showNoGpsSnackbar by remember { mutableStateOf(false) }

    val myLocationTitle = stringResource(R.string.my_location)

    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) {
            viewModel.startLocationUpdates()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef.value?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef.value?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef.value?.onDetach()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    controller.setCenter(GeoPoint(55.7558, 37.6173)) 

                    overlays.add(
                        MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                p?.let {
                                    selectedLatLng = it.latitude to it.longitude
                                    showAddSheet = true
                                }
                                return true
                            }
                            override fun longPressHelper(p: GeoPoint?): Boolean {
                                p?.let {
                                    selectedLatLng = it.latitude to it.longitude
                                    showAddSheet = true
                                }
                                return true
                            }
                        })
                    )
                    mapViewRef.value = this
                }
            },
            update = { mapView ->
                mapView.overlays.removeAll { it is Marker }

                userLocation?.let { (lat, lon) ->
                    val userMarker = Marker(mapView).apply {
                        position = GeoPoint(lat, lon)
                        title = myLocationTitle
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_my_location)
                    }
                    mapView.overlays.add(userMarker)
                }

                capsules.forEach { capsule ->
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(capsule.latitude, capsule.longitude)
                        title = capsule.textContent ?: "Audio capsule"
                        snippet = capsule.layer
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    mapView.overlays.add(marker)
                }

                if (isFirstLocation) {
                    userLocation?.let { (lat, lon) ->
                        mapView.controller.animateTo(GeoPoint(lat, lon))
                        isFirstLocation = false
                    }
                }

                mapView.invalidate()
            },
            modifier = Modifier.fillMaxSize(),
        )

        SmallFloatingActionButton(
            onClick = onSettings,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(44.dp),
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        ) {
            SmallFloatingActionButton(
                onClick = { showLayersSheet = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource(R.string.layers),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            SmallFloatingActionButton(
                onClick = onCapsuleList,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(44.dp),
            ) {
                Icon(
                    Icons.Default.MailOutline,
                    contentDescription = stringResource(R.string.my_capsules),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        FloatingActionButton(
            onClick = {
                val loc = userLocation
                if (loc != null) {
                    selectedLatLng = loc
                    showAddSheet = true
                } else {
                    showNoGpsSnackbar = true
                }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.add_capsule),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }

        if (showNoGpsSnackbar) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            ) {
                Text(stringResource(R.string.waiting_for_gps))
            }
            LaunchedEffect(showNoGpsSnackbar) {
                kotlinx.coroutines.delay(2500)
                showNoGpsSnackbar = false
            }
        }
    }

    if (showAddSheet) {
        AddCapsuleSheet(
            latitude = selectedLatLng?.first ?: 0.0,
            longitude = selectedLatLng?.second ?: 0.0,
            onDismiss = { showAddSheet = false },
            onSaveText = { lat, lon, radius, text, layer, ttl, recipient ->
                viewModel.createTextCapsule(lat, lon, radius, text, layer, ttl, recipient)
                showAddSheet = false
            },
            onSaveAudio = { lat, lon, radius, layer, path ->
                viewModel.createAudioCapsule(lat, lon, radius, layer, path)
                showAddSheet = false
            },
        )
    }

    if (showLayersSheet) {
        LayersSheet(
            layers = availableLayers,
            activeLayers = activeLayers,
            onToggleLayer = { viewModel.toggleLayer(it) },
            onDismiss = { showLayersSheet = false },
        )
    }
}
