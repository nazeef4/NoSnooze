package com.nosnooze.alarm.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nosnooze.alarm.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AlarmRepository(app)
    val alarms = repository.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun save(alarm: Alarm, done: () -> Unit = {}) = viewModelScope.launch { repository.save(alarm); done() }
    fun toggle(alarm: Alarm, enabled: Boolean) = viewModelScope.launch { repository.toggle(alarm, enabled) }
    fun delete(alarm: Alarm) = viewModelScope.launch { repository.delete(alarm) }
}
