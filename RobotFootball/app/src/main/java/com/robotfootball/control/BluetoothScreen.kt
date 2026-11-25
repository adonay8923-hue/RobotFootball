package com.robotfootball.control

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Diálogo de gestión Bluetooth: buscar emparejados, seleccionar, conectar, desconectar. */
@Composable
fun BluetoothScreen(
    ui: RobotUiState, onSearch: () -> Unit, onSelect: (BtDevice) -> Unit,
    onConnect: () -> Unit, onDisconnect: () -> Unit, onClose: () -> Unit
) {
    val cyan = Color(0xFF00B7FF)
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp), color = Color(0xFF061833),
            border = BorderStroke(1.dp, cyan)
        ) {
            Row(Modifier.padding(16.dp)) {
                Column(Modifier.weight(1.4f)) {
                    Text("DISPOSITIVOS EMPAREJADOS", color = cyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(ui.devices) { d ->
                            val sel = ui.selected?.address == d.address
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) Color(0xFF0A5BFF) else Color(0xFF0B2347))
                                    .clickable { onSelect(d) }.padding(12.dp)
                            ) {
                                Text(d.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text(d.address, color = Color(0xFFB0C4DE), fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val status = when (ui.conn) {
                        ConnState.CONNECTED -> "✔ Conectado a ${ui.deviceName}"
                        ConnState.CONNECTING -> "Conectando..."
                        ConnState.DISCONNECTED -> "Desconectado"
                    }
                    Text(status, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    ui.message?.let { Text(it, color = Color(0xFFFFC107), fontSize = 13.sp) }
                    Button(onClick = onSearch, modifier = Modifier.fillMaxWidth()) { Text("BUSCAR EMPAREJADOS") }
                    Button(onClick = onConnect, modifier = Modifier.fillMaxWidth(),
                        enabled = ui.selected != null && ui.conn == ConnState.DISCONNECTED,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B9E3A))) { Text("CONECTAR") }
                    Button(onClick = onDisconnect, modifier = Modifier.fillMaxWidth(),
                        enabled = ui.conn != ConnState.DISCONNECTED,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))) { Text("DESCONECTAR") }
                    Button(onClick = onClose, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))) { Text("CERRAR") }
                }
            }
        }
    }
}
