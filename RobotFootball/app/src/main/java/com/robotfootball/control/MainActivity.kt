package com.robotfootball.control

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    private val vm: RobotController by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent { RobotApp(vm) }
    }

    // Seguridad: si la app pierde foco o va a segundo plano, soltar todo y enviar X
    override fun onPause() { vm.releaseAll(); super.onPause() }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) vm.releaseAll()
    }
}

@Composable
fun RobotApp(vm: RobotController) {
    val ui by vm.ui.collectAsState()
    var showBt by remember { mutableStateOf(false) }

    val enableLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { vm.refreshDevices() }
    lateinit var ensureReady: () -> Unit
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (vm.hasPermission()) ensureReady() else vm.toast("Permiso de Bluetooth denegado")
    }
    // Comprueba soporte -> permiso -> Bluetooth activo -> lista emparejados
    ensureReady = {
        when {
            !vm.bluetoothSupported -> vm.toast("Este teléfono no tiene Bluetooth")
            !vm.hasPermission() -> permLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
            !vm.bluetoothEnabled -> enableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            else -> vm.refreshDevices()
        }
    }

    ControlScreen(ui, vm::press, vm::release) { showBt = true; ensureReady() }
    if (showBt) {
        BluetoothScreen(ui, onSearch = { ensureReady() }, onSelect = vm::select,
            onConnect = vm::connect, onDisconnect = vm::disconnect, onClose = { showBt = false })
    }
}
