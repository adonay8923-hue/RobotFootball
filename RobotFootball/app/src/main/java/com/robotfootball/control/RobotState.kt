package com.robotfootball.control

/** Botones físicos de la pantalla. */
enum class Btn { UP, DOWN, LEFT, RIGHT }
enum class ConnState { DISCONNECTED, CONNECTING, CONNECTED }
data class BtDevice(val name: String, val address: String)

/** Estado único de la UI (fuente de verdad). */
data class RobotUiState(
    val conn: ConnState = ConnState.DISCONNECTED,
    val deviceName: String? = null,
    val pressed: Set<Btn> = emptySet(),   // estado centralizado de botones presionados
    val devices: List<BtDevice> = emptyList(),
    val selected: BtDevice? = null,
    val message: String? = null,
    val lastCommand: Char = 'X'
)

/** Botón incompatible (eje contrario). */
fun Btn.opposite(): Btn = when (this) {
    Btn.UP -> Btn.DOWN; Btn.DOWN -> Btn.UP; Btn.LEFT -> Btn.RIGHT; Btn.RIGHT -> Btn.LEFT
}

/** Tabla del protocolo: botones presionados -> carácter ASCII. */
fun commandFor(p: Set<Btn>): Char {
    val v = if (Btn.UP in p) 1 else if (Btn.DOWN in p) -1 else 0
    val h = if (Btn.LEFT in p) -1 else if (Btn.RIGHT in p) 1 else 0
    return when {
        v == 1 && h == 0 -> 'F'
        v == -1 && h == 0 -> 'B'
        v == 0 && h == -1 -> 'L'
        v == 0 && h == 1 -> 'R'
        v == 1 && h == -1 -> 'G'
        v == 1 && h == 1 -> 'I'
        v == -1 && h == -1 -> 'H'
        v == -1 && h == 1 -> 'J'
        else -> 'X'
    }
}
