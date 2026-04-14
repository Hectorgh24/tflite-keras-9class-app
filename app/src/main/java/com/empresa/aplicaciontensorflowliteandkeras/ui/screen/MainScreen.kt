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

    var hasPermissions by remember { mutableStateOf(false) }
    var emergencyNumber by remember { mutableStateOf(sharedPrefs.getString("phone", "") ?: "") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val sms = permissions[Manifest.permission.SEND_SMS] ?: false
        val call = permissions[Manifest.permission.CALL_PHONE] ?: false
        hasPermissions = sms && call
    }

    LaunchedEffect(Unit) {
        val smsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val callGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        hasPermissions = smsGranted && callGranted

        val permissionsToRequest = mutableListOf(Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
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
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // AlertScreen ahora es manejado globalmente por AppNavigator
            MonitorScreen(
                hasPermissions = hasPermissions,
                    isMonitoring = isMonitoring,
                    emergencyNumber = emergencyNumber,
                    currentPrediction = currentPrediction,
                    onNumberChange = {
                        emergencyNumber = it
                        sharedPrefs.edit().putString("phone", it).apply()
                    },
                    onRequestPermissions = {
                        val perms = mutableListOf(Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        permissionLauncher.launch(perms.toTypedArray())
                    },
                    onToggleMonitoring = {
                        if (emergencyNumber.length < 10 && !isMonitoring) {
                            Toast.makeText(context, "Ingresa un número válido", Toast.LENGTH_SHORT).show()
                        } else {
                            val serviceIntent = Intent(context, FallDetectionService::class.java)
                            if (isMonitoring) {
                                context.stopService(serviceIntent)
                            } else {
                                ContextCompat.startForegroundService(context, serviceIntent)
                            }
                        }
                    }
                )
            }
        }
    }
