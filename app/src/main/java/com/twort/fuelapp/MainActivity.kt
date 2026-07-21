package com.twort.fuelapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.twort.fuelapp.ui.screens.FuelApp
import com.twort.fuelapp.ui.theme.FuelAppTheme
import com.twort.fuelapp.viewmodel.FuelViewModel

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FuelAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val viewModel: FuelViewModel = viewModel()
                    val locationPermissions = rememberMultiplePermissionsState(
                        listOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    )

                    LaunchedEffect(locationPermissions.allPermissionsGranted) {
                        if (locationPermissions.allPermissionsGranted) {
                            viewModel.refresh()
                        } else {
                            locationPermissions.launchMultiplePermissionRequest()
                        }
                    }

                    FuelApp(viewModel = viewModel)
                }
            }
        }
    }
}
