package com.hpg.goaltracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.hpg.goaltracker.data.AppSettings
import com.hpg.goaltracker.notify.Reminders
import com.hpg.goaltracker.ui.GoalTrackerApp
import com.hpg.goaltracker.ui.GoalViewModel
import com.hpg.goaltracker.ui.theme.GoalTrackerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GoalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Reminders.ensureChannel(this)
        Reminders.scheduleAll(this)

        setContent {
            var themeMode by remember { mutableIntStateOf(AppSettings.themeMode(this)) }
            var dynamicColor by remember { mutableStateOf(AppSettings.dynamicColor(this)) }

            GoalTrackerTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NotificationPermissionGate()
                    GoalTrackerApp(
                        vm = viewModel,
                        onThemeChanged = { mode, dynamic ->
                            themeMode = mode
                            dynamicColor = dynamic
                            AppSettings.setTheme(this, mode, dynamic)
                        }
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun NotificationPermissionGate() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result ignored — reminders simply won't post if denied */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
