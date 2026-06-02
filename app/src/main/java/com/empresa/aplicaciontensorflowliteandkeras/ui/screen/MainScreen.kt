package com.empresa.aplicaciontensorflowliteandkeras

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    // Observadores directos del Servicio
    val isMonitoring by MonitoringState.isMonitoring.collectAsState()
    val currentPrediction by MonitoringState.currentPrediction.collectAsState()
    val sosActive by MonitoringState.sosActive.collectAsState()
    val countdown by MonitoringState.countdown.collectAsState()
    val remainingSeconds by MonitoringState.remainingSeconds.collectAsState()

    var hasPermissions by remember { mutableStateOf(false) }
    var emergencyNumber by remember { mutableStateOf(sharedPrefs.getString("phone", "") ?: "") }

    val checkPermissions = {
        val smsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val callGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

        // ACTIVITY_RECOGNITION es necesario para el foreground service "health" en Android 14+
        val activityRecognitionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        else true

        // POST_NOTIFICATIONS es necesario para mostrar la notificación del servicio en Android 13+
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else true

        smsGranted && callGranted && activityRecognitionGranted && notificationsGranted
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermissions = checkPermissions()
    }

    LaunchedEffect(Unit) {
        hasPermissions = checkPermissions()

        val permissionsToRequest =
            mutableListOf(Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (!hasPermissions) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detector de Caídas") },
                navigationIcon = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "Ajustes")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding).fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // AlertScreen ahora es manejado globalmente por AppNavigator
            MonitorScreen(
                hasPermissions = hasPermissions,
                isMonitoring = isMonitoring,
                emergencyNumber = emergencyNumber,
                currentPrediction = currentPrediction,
                remainingSeconds = remainingSeconds,
                onNumberChange = {
                    emergencyNumber = it
                    sharedPrefs.edit().putString("phone", it).apply()
                },
                onRequestPermissions = {
                    hasPermissions = checkPermissions()
                    if (!hasPermissions) {
                        val perms =
                            mutableListOf(Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) perms.add(Manifest.permission.ACTIVITY_RECOGNITION)
                        permissionLauncher.launch(perms.toTypedArray())
                    }
                },
                onToggleMonitoring = {
                    if (emergencyNumber.length < 10 && !isMonitoring) {
                        Toast.makeText(context, "Ingresa un número válido", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        val serviceIntent =
                            Intent(context, FallDetectionService::class.java).apply {
                                putExtra("EMERGENCY_NUMBER", emergencyNumber)
                            }
                        if (isMonitoring) {
                            context.stopService(serviceIntent)
                        } else {
                            try {
                                ContextCompat.startForegroundService(context, serviceIntent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "No se pudo iniciar el monitoreo: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            )
        }
    }
}
