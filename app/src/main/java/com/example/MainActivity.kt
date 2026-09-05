package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.overlay.LogType
import com.example.overlay.OverlayEventBus
import com.example.overlay.OverlayLogEntry
import com.example.service.OverlayBinderService
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TerminalBlue
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TerminalYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.PermissionHelper
import com.example.util.PreferenceHelper

class MainActivity : ComponentActivity() {

    private var hasOverlayPermission by mutableStateOf(false)
    private var isBatteryOptimized by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppScreen(
                    hasOverlayPermission = hasOverlayPermission,
                    isBatteryOptimized = isBatteryOptimized,
                    onRefreshPermissions = { checkPermissions() },
                    onRequestOverlayPermission = { PermissionHelper.openOverlaySettings(this) },
                    onRequestBatteryOptimization = { PermissionHelper.openBatteryOptimizationSettings(this) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        hasOverlayPermission = PermissionHelper.hasOverlayPermission(this)
        isBatteryOptimized = !PermissionHelper.isIgnoringBatteryOptimizations(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    hasOverlayPermission: Boolean,
    isBatteryOptimized: Boolean,
    onRefreshPermissions: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestBatteryOptimization: () -> Unit
) {
    val context = LocalContext.current
    val preferenceHelper = remember { PreferenceHelper(context) }
    var autoStartBoot by remember { mutableStateOf(preferenceHelper.isAutoStartEnabled) }

    val isServiceRunning by OverlayEventBus.isServiceRunning.collectAsStateWithLifecycle()
    val isOverlayVisible by OverlayEventBus.isOverlayVisible.collectAsStateWithLifecycle()
    val logEntries by OverlayEventBus.logEntries.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Console & Test", "Termux Setup", "Live Logs")

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isServiceRunning) TerminalGreen else TextMuted)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Termux Overlay API",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = if (isServiceRunning) "Binder IPC Daemon: ACTIVE" else "Binder IPC Daemon: IDLE",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isServiceRunning) TerminalGreen else TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefreshPermissions,
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Status", tint = TerminalCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Permission & Service quick indicator banner
            PermissionAndServiceHeader(
                hasOverlayPermission = hasOverlayPermission,
                isBatteryOptimized = isBatteryOptimized,
                isServiceRunning = isServiceRunning,
                isOverlayVisible = isOverlayVisible,
                autoStartBoot = autoStartBoot,
                onToggleAutoStart = { enabled ->
                    autoStartBoot = enabled
                    preferenceHelper.isAutoStartEnabled = enabled
                },
                onRequestOverlayPermission = onRequestOverlayPermission,
                onRequestBatteryOptimization = onRequestBatteryOptimization,
                onStartService = { OverlayBinderService.start(context) },
                onStopService = { OverlayBinderService.stop(context) }
            )

            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = TerminalCyan,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> TestBenchTab(
                    context = context,
                    hasOverlayPermission = hasOverlayPermission,
                    isOverlayVisible = isOverlayVisible,
                    onRequestOverlayPermission = onRequestOverlayPermission
                )
                1 -> TermuxSetupTab(context = context)
                2 -> LiveLogsTab(
                    logs = logEntries,
                    onClearLogs = { OverlayEventBus.clearLogs() }
                )
            }
        }
    }
}

@Composable
fun PermissionAndServiceHeader(
    hasOverlayPermission: Boolean,
    isBatteryOptimized: Boolean,
    isServiceRunning: Boolean,
    isOverlayVisible: Boolean,
    autoStartBoot: Boolean,
    onToggleAutoStart: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Permission Alert Row if missing
            if (!hasOverlayPermission) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalRed.copy(alpha = 0.15f))
                        .border(1.dp, TerminalRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Alert", tint = TerminalRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SYSTEM_ALERT_WINDOW permission missing",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    Button(
                        onClick = onRequestOverlayPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("overlay_permission_button")
                    ) {
                        Text("Grant", fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Status Badges & Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasOverlayPermission) TerminalGreen else TerminalRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasOverlayPermission) "Overlay Permitted" else "Overlay Denied",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = if (isOverlayVisible) TerminalCyan else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOverlayVisible) "Floating Window: VISIBLE" else "Floating Window: HIDDEN",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isOverlayVisible) TerminalCyan else TextSecondary,
                                fontWeight = if (isOverlayVisible) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isServiceRunning) {
                        OutlinedButton(
                            onClick = onStopService,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("stop_service_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = TerminalRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop Service", fontSize = 12.sp, color = TerminalRed)
                        }
                    } else {
                        Button(
                            onClick = onStartService,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalBlue),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("start_service_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start Service", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Auto-start switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto-start on Android boot",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                Switch(
                    checked = autoStartBoot,
                    onCheckedChange = onToggleAutoStart,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TerminalGreen,
                        checkedTrackColor = TerminalGreen.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("auto_start_switch")
                )
            }
        }
    }
}

@Composable
fun TestBenchTab(
    context: Context,
    hasOverlayPermission: Boolean,
    isOverlayVisible: Boolean,
    onRequestOverlayPermission: () -> Unit
) {
    var textInput by remember { mutableStateOf("System Load: 18% | IP: 192.168.1.100") }
    var buttonInput by remember { mutableStateOf("EXECUTE_SCRIPT") }
    var imagePathInput by remember { mutableStateOf("/sdcard/test.png") }
    var alphaVal by remember { mutableFloatStateOf(0.95f) }
    var posX by remember { mutableFloatStateOf(60f) }
    var posY by remember { mutableFloatStateOf(180f) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Interactive Overlay Test Bench",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = TerminalCyan,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Simulate commands exactly as dispatched by Termux CLI over IPC.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }

        // Text Overlay Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "1. Text Window (showText / updateText)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("text_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TerminalCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        placeholder = { Text("Enter terminal message to display") },
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(context, OverlayBinderService::class.java).apply {
                                    action = OverlayBinderService.ACTION_SHOW_TEXT
                                    putExtra(OverlayBinderService.EXTRA_TEXT, textInput)
                                }
                                context.startService(intent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("show_text_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("showText")
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(context, OverlayBinderService::class.java).apply {
                                    action = OverlayBinderService.ACTION_UPDATE_TEXT
                                    putExtra(OverlayBinderService.EXTRA_TEXT, textInput)
                                }
                                context.startService(intent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("update_text_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("updateText")
                        }
                    }
                }
            }
        }

        // Button Overlay Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "2. Button Window (showButton)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = buttonInput,
                        onValueChange = { buttonInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("button_input_field"),
                        placeholder = { Text("Button Label (e.g. DEPLOY)") }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val intent = Intent(context, OverlayBinderService::class.java).apply {
                                action = OverlayBinderService.ACTION_SHOW_BUTTON
                                putExtra(OverlayBinderService.EXTRA_LABEL, buttonInput)
                            }
                            context.startService(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("show_button_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("showButton", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Image Overlay Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "3. Image Window (showImage)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = imagePathInput,
                        onValueChange = { imagePathInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_path_field"),
                        placeholder = { Text("Absolute path: /sdcard/image.png") }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val intent = Intent(context, OverlayBinderService::class.java).apply {
                                action = OverlayBinderService.ACTION_SHOW_IMAGE
                                putExtra(OverlayBinderService.EXTRA_PATH, imagePathInput)
                            }
                            context.startService(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("show_image_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("showImage", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Position & Alpha Controls
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "4. Window Parameters (Alpha & Position)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Transparency (Alpha): ${(alphaVal * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = alphaVal,
                        onValueChange = {
                            alphaVal = it
                            val intent = Intent(context, OverlayBinderService::class.java).apply {
                                action = OverlayBinderService.ACTION_SET_ALPHA
                                putExtra(OverlayBinderService.EXTRA_ALPHA, it)
                            }
                            context.startService(intent)
                        },
                        valueRange = 0.2f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = TerminalCyan, activeTrackColor = TerminalCyan)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Position X: ${posX.toInt()}px, Y: ${posY.toInt()}px", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = posY,
                        onValueChange = {
                            posY = it
                            val intent = Intent(context, OverlayBinderService::class.java).apply {
                                action = OverlayBinderService.ACTION_SET_POS
                                putExtra(OverlayBinderService.EXTRA_POS_X, posX.toInt())
                                putExtra(OverlayBinderService.EXTRA_POS_Y, it.toInt())
                            }
                            context.startService(intent)
                        },
                        valueRange = 50f..800f,
                        colors = SliderDefaults.colors(thumbColor = TerminalYellow, activeTrackColor = TerminalYellow)
                    )
                }
            }
        }

        // Dismiss Button
        item {
            Button(
                onClick = {
                    val intent = Intent(context, OverlayBinderService::class.java).apply {
                        action = OverlayBinderService.ACTION_HIDE
                    }
                    context.startService(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hide_overlay_button"),
                colors = ButtonDefaults.buttonColors(containerColor = TerminalRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("hide() [Dismiss Overlay]", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TermuxSetupTab(context: Context) {
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    val installCmd = "mkdir -p \$PREFIX/bin && cp /sdcard/overlay \$PREFIX/bin/overlay && chmod +x \$PREFIX/bin/overlay"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Termux Integration Guide",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = TerminalCyan,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Learn how Termux connects to Android Binder IPC and runs commands.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Install Command",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(
                            onClick = {
                                val clip = ClipData.newPlainText("Termux Overlay Install", installCmd)
                                clipboardManager.setPrimaryClip(clip)
                                Toast.makeText(context, "Command copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TerminalCyan)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0D1117))
                            .border(1.dp, Color(0xFF30363D), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = installCmd,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TerminalGreen,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Command Cheat Sheet",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        val commands = listOf(
            "overlay show \"Hello Android\"" to "Display text in floating window",
            "overlay update \"CPU: 32% | RAM: 4.1GB\"" to "Update text in place without flicker",
            "overlay button \"DEPLOY\"" to "Display interactive clickable button",
            "overlay image \"/sdcard/preview.png\"" to "Display local photo or screenshot",
            "overlay alpha 0.85" to "Adjust window transparency (0.1 - 1.0)",
            "overlay pos 80 200" to "Set window coordinates (X, Y px)",
            "overlay hide" to "Dismiss and remove floating window",
            "overlay status" to "Check whether daemon is running"
        )

        items(commands) { (cmd, desc) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cmd,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TerminalCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                    IconButton(
                        onClick = {
                            val clip = ClipData.newPlainText("Overlay Command", cmd)
                            clipboardManager.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied: $cmd", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LiveLogsTab(
    logs: List<OverlayLogEntry>,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Live IPC & Event Activity (${logs.size})",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = TerminalCyan,
                    fontWeight = FontWeight.Bold
                )
            )
            IconButton(onClick = onClearLogs, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear logs", tint = TextMuted)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No IPC events logged yet.", color = TextSecondary)
                    Text("Send a command from Termux or use the Test Bench above.", color = TextMuted, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0D1117))
                    .border(1.dp, Color(0xFF30363D), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(logs.reversed(), key = { it.id }) { entry ->
                    val color = when (entry.type) {
                        LogType.IPC_SHOW_TEXT -> TerminalCyan
                        LogType.IPC_UPDATE_TEXT -> TerminalBlue
                        LogType.IPC_SHOW_BUTTON -> TerminalYellow
                        LogType.IPC_SHOW_IMAGE -> TerminalGreen
                        LogType.IPC_HIDE -> TerminalRed
                        LogType.USER_CLICK -> TerminalYellow
                        LogType.SERVICE_EVENT -> TerminalGreen
                        LogType.IPC_CONFIG -> TextSecondary
                        LogType.ERROR -> TerminalRed
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = entry.timestamp,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(color.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = entry.type.name.removePrefix("IPC_"),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = color,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = entry.message,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Kept for test backward compatibility.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
