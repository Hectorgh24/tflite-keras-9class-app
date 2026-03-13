package com.empresa.aplicaciontensorflowliteandkeras

import androidx.compose.runtime.*

@Composable
fun AppNavigator() {
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
    } else {
        MainScreen(onOpenSettings = { showSettings = true })
    }
}