package com.robotfootball.control

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Lógica central: botones presionados -> comando único -> cola Bluetooth. */
class RobotController(app: Application) : AndroidViewModel(app) {
    private val bt = RobotBluetooth(app, viewModelScope) { onConnectionLost() }
    private val _ui = MutableStateFlow(RobotUiState())
    val ui: StateFlow<RobotUiState> = _ui.asStateFlow()
    private var lastSent = 'X'
    private var msgJob: Job? = null

    // Heartbeat: reenvia el comando actual cada 200 ms mientras haya movimiento
    init {
        viewModelScope.launch {
            while (true) {
                delay(200)
                if (_ui.value.conn == ConnState.CONNECTED && lastSent != 'X') bt.send(lastSent)
            }
        }
    }

    val bluetoothSupported get() = bt.supported
    val bluetoothEnabled get() = bt.isEnabled
    fun hasPermission() = bt.hasPermission()

    fun toast(m: String) {
        _ui.update { it.copy(message = m) }
        msgJob?.cancel()
        msgJob = viewModelScope.launch { delay(3000); _ui.update { it.copy(message = null) } }
    }

    // ---------- Bluetooth ----------
    fun refreshDevices() {
        if (!bt.supported) return toast("Este teléfono no tiene Bluetooth")
        if (!bt.hasPermission()) return toast("Falta el permiso de Bluetooth")
        if (!bt.isEnabled) return toast("Activa el Bluetooth")
        val l = bt.pairedDevices()
        _ui.update { it.copy(devices = l) }
        if (l.isEmpty()) toast("Sin dispositivos emparejados. Empareja el HC-05/HC-06 en Ajustes (PIN 1234 o 0000)")
    }

    fun select(d: BtDevice) = _ui.update { it.copy(selected = d) }

    fun connect() {
        val s = _ui.value
        val d = s.selected ?: return toast("Selecciona un dispositivo")
        if (s.conn != ConnState.DISCONNECTED) return
        if (!bt.isEnabled) return toast("Activa el Bluetooth")
        _ui.update { it.copy(conn = ConnState.CONNECTING, pressed = emptySet()) }
        viewModelScope.launch {
            bt.connect(d.address)
                .onSuccess {
                    lastSent = 'X'
                    _ui.update { it.copy(conn = ConnState.CONNECTED, deviceName = d.name, lastCommand = 'X') }
                    toast("Conexión exitosa con ${d.name}")
                }
                .onFailure {
                    _ui.update { it.copy(conn = ConnState.DISCONNECTED, deviceName = null) }
                    toast("No se pudo conectar. Verifica que el módulo esté encendido")
                }
        }
    }

    fun disconnect() {
        if (_ui.value.conn == ConnState.DISCONNECTED) return
        _ui.update { it.copy(conn = ConnState.DISCONNECTED, deviceName = null, pressed = emptySet()) }
        viewModelScope.launch { bt.disconnect(); lastSent = 'X'; toast("Desconectado") }
    }

    private fun onConnectionLost() {
        lastSent = 'X'
        _ui.update { it.copy(conn = ConnState.DISCONNECTED, deviceName = null, pressed = emptySet(), lastCommand = 'X') }
        toast("Conexión perdida")
    }

    // ---------- Botones ----------
    fun press(b: Btn) {
        val s = _ui.value
        if (s.conn != ConnState.CONNECTED) return toast("Desconectado: conecta el robot primero")
        if (b in s.pressed || b.opposite() in s.pressed) return  // eje incompatible
        _ui.update { it.copy(pressed = it.pressed + b) }
        dispatch()
    }

    fun release(b: Btn) {
        if (b !in _ui.value.pressed) return
        _ui.update { it.copy(pressed = it.pressed - b) }
        dispatch()
    }

    /** Suelta todo (ACTION_CANCEL, pérdida de foco, onPause) y envía X. */
    fun releaseAll() {
        _ui.update { it.copy(pressed = emptySet()) }
        dispatch()
    }

    /** ÚNICA función que calcula y envía; solo envía si el comando cambió. */
    private fun dispatch() {
        val s = _ui.value
        if (s.conn != ConnState.CONNECTED) return
        val cmd = commandFor(s.pressed)
        if (cmd == lastSent) return
        lastSent = cmd
        bt.send(cmd)
        _ui.update { it.copy(lastCommand = cmd) }
    }

    override fun onCleared() { bt.closeImmediately() }
}
