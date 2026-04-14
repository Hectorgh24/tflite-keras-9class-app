package com.empresa.aplicaciontensorflowliteandkeras

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    val sosActive by MonitoringState.sosActive.collectAsState()
    val countdown by MonitoringState.countdown.collectAsState()
    val context = LocalContext.current

    // Recuperar el número guardado con las mismas claves que usa MainScreen
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    val emergencyNumber = prefs.getString("phone", "") ?: ""

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                MainScreen(
                    onOpenSettings = { navController.navigate("settings") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Capa superior de alerta global (Se dibuja sobre cualquier pantalla)
        if (sosActive) {
            AlertScreen(
                countdown = countdown,
                onCancel = {
                    MonitoringState.sosActive.value = false
                    MonitoringState.countdown.value = 5
                },
                onTimeout = {
                    if (emergencyNumber.isNotEmpty()) {
                        ejecutarProtocoloEmergencia(context, emergencyNumber)
                    }
                    MonitoringState.sosActive.value = false
                    MonitoringState.countdown.value = 5
                }
            )
        }
    }
}