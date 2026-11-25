# Robot Football – Control Bluetooth

## Compilar (Android Studio)
1. File > Open > carpeta RobotFootball (esperar el Gradle Sync, requiere Internet la 1ª vez).
2. Build > Build Bundle(s)/APK(s) > Build APK(s).
3. APK: app/build/outputs/apk/debug/app-debug.apk
Por terminal (si Gradle está instalado): `gradle wrapper --gradle-version 8.7` y luego `./gradlew assembleDebug`.

## Instalar en el teléfono
Copia app-debug.apk al teléfono, ábrelo y permite "instalar apps desconocidas". O con USB depuración: `adb install app-debug.apk`.
Antes: empareja el HC-05/HC-06 en Ajustes > Bluetooth (PIN 1234 o 0000).

## Archivos
- RobotState.kt: enums, estado de UI y tabla botones->carácter (commandFor).
- BluetoothManager.kt: RFCOMM/SPP, conexión en hilo IO, cola con un solo escritor, X al desconectar, detección de desconexión.
- RobotController.kt: ViewModel; reglas de ejes, estado de botones, única función dispatch(), solo envía si cambia el comando.
- ControlScreen.kt: UI (cabecera, cruz de botones, multitouch con awaitEachGesture).
- BluetoothScreen.kt: diálogo para buscar/seleccionar/conectar/desconectar.
- MainActivity.kt: permisos Android 12+, activar Bluetooth, pantalla completa, soltar botones en onPause/pérdida de foco.

## Protocolo (9600 baud típico del HC-05/06, 1 byte ASCII por cambio)
F adelante | B atrás | L izquierda | R derecha | G adelante+izq | I adelante+der | H atrás+izq | J atrás+der | X reposo
Arduino: leer con Serial.read() y actuar según el carácter; X = detener motores.
