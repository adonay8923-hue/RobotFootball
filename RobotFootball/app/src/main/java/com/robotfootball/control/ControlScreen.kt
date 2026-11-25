package com.robotfootball.control

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Green = Color(0xFF39FF14)
private val Red = Color(0xFFFF3B30)
private val Cyan = Color(0xFF00B7FF)

private val OctagonShape = GenericShape { size, _ ->
    val w = size.width; val h = size.height; val c = minOf(w, h) * 0.22f
    moveTo(c, 0f); lineTo(w - c, 0f); lineTo(w, c); lineTo(w, h - c)
    lineTo(w - c, h); lineTo(c, h); lineTo(0f, h - c); lineTo(0f, c); close()
}

@Composable
fun ControlScreen(
    ui: RobotUiState,
    onPress: (Btn) -> Unit,
    onRelease: (Btn) -> Unit,
    onOpenBluetooth: () -> Unit
) {
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF020B1F), Color(0xFF05264F), Color(0xFF020814))))
            .displayCutoutPadding().padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Header(ui, onOpenBluetooth)
        Row(Modifier.weight(1f).fillMaxWidth()) {
            // Izquierda: ADELANTE arriba, ATRÁS abajo
            Column(Modifier.weight(1f).fillMaxHeight()) {
                RobotButton(Btn.UP, "Adelante", Icons.Filled.ArrowUpward, Green, ui, onPress, onRelease, Modifier.weight(1f).fillMaxWidth())
                RobotButton(Btn.DOWN, "Atrás", Icons.Filled.ArrowDownward, Red, ui, onPress, onRelease, Modifier.weight(1f).fillMaxWidth())
            }
            // Centro: mensajes y último comando
            Column(Modifier.weight(0.9f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("⚽", fontSize = 64.sp)
                Text("Comando: ${ui.lastCommand}", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                ui.message?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xCC0A2A5A)).padding(10.dp))
                }
            }
            // Derecha: IZQUIERDA y DERECHA
            Row(Modifier.weight(1.5f).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                RobotButton(Btn.LEFT, "Izquierda", Icons.AutoMirrored.Filled.ArrowBack, Cyan, ui, onPress, onRelease, Modifier.weight(1f).fillMaxHeight(0.7f))
                RobotButton(Btn.RIGHT, "Derecha", Icons.AutoMirrored.Filled.ArrowForward, Cyan, ui, onPress, onRelease, Modifier.weight(1f).fillMaxHeight(0.7f))
            }
        }
    }
}

@Composable
private fun Header(ui: RobotUiState, onOpenBluetooth: () -> Unit) {
    val (txt, col) = when (ui.conn) {
        ConnState.CONNECTED -> "Conectado" to Color(0xFF00FF40)
        ConnState.CONNECTING -> "Conectando..." to Color(0xFFFFC107)
        ConnState.DISCONNECTED -> "Desconectado" to Red
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xAA061833))
            .border(1.dp, Cyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF0A5BFF)).clickable { onOpenBluetooth() }, contentAlignment = Alignment.Center) {
                Icon(if (ui.conn == ConnState.CONNECTED) Icons.Filled.BluetoothConnected else Icons.Filled.Bluetooth, "Bluetooth", tint = Color.White)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Bluetooth", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(col))
                    Spacer(Modifier.width(6.dp))
                    Text(txt, color = col, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ROBOT", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
            Text("FOOTBALL", color = Cyan, fontSize = 24.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text("Dispositivo:", color = Color.White, fontSize = 15.sp)
                Text(ui.deviceName ?: "---", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, Cyan, RoundedCornerShape(10.dp)).clickable { onOpenBluetooth() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Settings, "Configuración", tint = Color.White)
            }
        }
    }
}

/**
 * Botón táctil. Cada botón tiene su PROPIO detector (awaitEachGesture), por eso
 * dos dedos en dos botones distintos funcionan a la vez (multitouch real).
 * Detecta "down" y "up" (o cancelación) sin usar onClick.
 */
@Composable
private fun RobotButton(
    btn: Btn, label: String, icon: ImageVector, color: Color, ui: RobotUiState,
    onPress: (Btn) -> Unit, onRelease: (Btn) -> Unit, modifier: Modifier
) {
    val enabled = btn.opposite() !in ui.pressed      // eje contrario presionado => deshabilitado
    val isDown = btn in ui.pressed
    val scale by animateFloatAsState(if (isDown) 0.93f else 1f, tween(80), label = "scale")
    val glow by animateFloatAsState(if (isDown) 1f else 0.3f, tween(100), label = "glow")
    val enabledNow by rememberUpdatedState(enabled)
    val press by rememberUpdatedState(onPress)
    val release by rememberUpdatedState(onRelease)

    Box(
        modifier.padding(6.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.3f }
            .clip(OctagonShape)
            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.1f + 0.5f * glow), Color(0xFF020814))))
            .border(if (isDown) 5.dp else 3.dp, color, OctagonShape)
            .pointerInput(btn) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!enabledNow) return@awaitEachGesture
                    down.consume()
                    press(btn)
                    try {
                        while (true) {
                            val ev = awaitPointerEvent()
                            val c = ev.changes.firstOrNull { it.id == down.id }
                            if (c == null || !c.pressed) break   // dedo levantado o gesto cancelado
                            c.consume()
                        }
                    } finally { release(btn) }               // también si se cancela la corrutina
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.fillMaxHeight(0.6f).aspectRatio(1f))
    }
}
