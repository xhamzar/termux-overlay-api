package com.example.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Event log entry recorded when an IPC or user interaction occurs.
 */
data class OverlayLogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
    val type: LogType,
    val message: String
)

enum class LogType {
    IPC_SHOW_TEXT,
    IPC_UPDATE_TEXT,
    IPC_SHOW_BUTTON,
    IPC_SHOW_IMAGE,
    IPC_HIDE,
    IPC_CONFIG,
    USER_CLICK,
    SERVICE_EVENT,
    ERROR
}

/**
 * Reactive Event Bus to publish overlay states and live IPC logs to the UI.
 */
object OverlayEventBus {
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _isOverlayVisible = MutableStateFlow(false)
    val isOverlayVisible: StateFlow<Boolean> = _isOverlayVisible.asStateFlow()

    private val _currentText = MutableStateFlow("")
    val currentText: StateFlow<String> = _currentText.asStateFlow()

    private val _lastAction = MutableStateFlow<String>("")
    val lastAction: StateFlow<String> = _lastAction.asStateFlow()

    private val _logEntries = MutableStateFlow<List<OverlayLogEntry>>(emptyList())
    val logEntries: StateFlow<List<OverlayLogEntry>> = _logEntries.asStateFlow()

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
        addLog(
            if (running) LogType.SERVICE_EVENT else LogType.SERVICE_EVENT,
            if (running) "Overlay Binder Service started (Foreground)" else "Overlay Binder Service stopped"
        )
    }

    fun setOverlayVisible(visible: Boolean) {
        _isOverlayVisible.value = visible
    }

    fun setCurrentText(text: String) {
        _currentText.value = text
    }

    fun setLastAction(action: String) {
        _lastAction.value = action
    }

    fun addLog(type: LogType, message: String) {
        val entry = OverlayLogEntry(type = type, message = message)
        val current = _logEntries.value.toMutableList()
        if (current.size > 100) {
            current.removeAt(0)
        }
        current.add(entry)
        _logEntries.value = current
    }

    fun clearLogs() {
        _logEntries.value = emptyList()
    }
}
