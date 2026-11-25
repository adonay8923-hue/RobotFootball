package com.robotfootball.control

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.IOException
import java.util.UUID

/**
 * Bluetooth Classic (RFCOMM / SPP) para HC-05 y HC-06.
 * Los caracteres pasan por una cola (Channel) con UN solo escritor => orden garantizado.
 */
@SuppressLint("MissingPermission")
class RobotBluetooth(
    private val ctx: Context,
    private val scope: CoroutineScope,
    private val onLost: () -> Unit
) {
    private val adapter: BluetoothAdapter? =
        (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null
    private var queue: Channel<Char>? = null
    private var writer: Job? = null

    val supported get() = adapter != null
    val isEnabled get() = adapter?.isEnabled == true

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    fun pairedDevices(): List<BtDevice> = try {
        adapter?.bondedDevices?.map { BtDevice(it.name ?: "Sin nombre", it.address) }
            ?.sortedBy { it.name } ?: emptyList()
    } catch (e: SecurityException) { emptyList() }

    /** Conecta en hilo IO (no bloquea la UI). Devuelve error si falla. */
    suspend fun connect(address: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            closeNow()
            val a = adapter ?: error("Bluetooth no disponible")
            val dev = a.getRemoteDevice(address)
            a.cancelDiscovery()
            var s = dev.createRfcommSocketToServiceRecord(sppUuid)
            try { s.connect() } catch (e: IOException) {
                // Alternativa que funciona en algunos HC-05/HC-06 y teléfonos
                runCatching { s.close() }
                s = dev.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    .invoke(dev, 1) as BluetoothSocket
                s.connect()
            }
            socket = s
            startWriter(s)
        }
    }

    private fun startWriter(s: BluetoothSocket) {
        val q = Channel<Char>(Channel.UNLIMITED)
        queue = q
        val out = s.outputStream
        writer = scope.launch(Dispatchers.IO) {
            var failed = false
            try {
                for (c in q) { out.write(c.code); out.flush() }
            } catch (e: IOException) { failed = true }
            finally {
                runCatching { s.close() }
                if (failed && socket === s) {
                    socket = null; queue = null
                    launch(Dispatchers.Main) { onLost() }
                }
            }
        }
    }

    /** Encola un comando (no bloquea). */
    fun send(c: Char) { queue?.trySend(c) }

    /** Desconexión limpia: envía X, vacía la cola y cierra. */
    suspend fun disconnect() {
        val q = queue; val w = writer
        queue = null
        q?.trySend('X'); q?.close()
        withTimeoutOrNull(1000) { w?.join() }
        w?.cancel()
        closeNow()
    }

    /** Cierre de emergencia (onCleared). Intenta mandar X directo. */
    fun closeImmediately() {
        runCatching { socket?.outputStream?.apply { write('X'.code); flush() } }
        closeNow()
    }

    private fun closeNow() {
        runCatching { socket?.close() }
        socket = null
    }
}
