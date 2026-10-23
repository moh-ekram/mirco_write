package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.navigation.AppNavHost
import com.example.ui.theme.MicroWriteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.data.backup.BackupManager.scheduleDaily2AmBackup(this)
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val colorPalette by viewModel.colorPalette.collectAsState()
            val fontScale by viewModel.fontScale.collectAsState()

            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = currentDensity.density,
                    fontScale = currentDensity.fontScale * fontScale
                )
            ) {
                MicroWriteTheme(
                    themeMode = themeMode,
                    palette = colorPalette
                ) {
                    AppNavHost(viewModel = viewModel)
                }
            }
        }
    }
}
