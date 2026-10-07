package com.example.nightsound
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
data class SoundEvent(val fileName: String, val timeLabel: String, val durationSeconds: Int)
object MonitorState {
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring = _isMonitoring.asStateFlow()
    private val _events = MutableStateFlow<List<SoundEvent>>(emptyList())
    val events = _events.asStateFlow()
    fun setMonitoring(value: Boolean) { _isMonitoring.value = value }
    fun addEvent(event: SoundEvent) { _events.value = listOf(event) + _events.value }
}
