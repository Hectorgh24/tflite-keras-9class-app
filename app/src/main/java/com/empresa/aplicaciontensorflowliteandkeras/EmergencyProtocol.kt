package com.empresa.aplicaciontensorflowliteandkeras

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.widget.Toast
///
fun ejecutarProtocoloEmergencia(context: Context, phoneNumber: String) {
    try {
        // 1. Obtener SmsManager con soporte universal
        val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        // 2. Enviar el mensaje de texto
        if (smsManager != null) {
            val mensaje = "ALERTA SOS: He sufrido una posible caída o síncope. Necesito asistencia inmediata."
            smsManager.sendTextMessage(phoneNumber, null, mensaje, null, null)
        }

        // 3. Realizar la llamada telefónica
        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(callIntent)

        // 4. Mostrar Toast de forma segura enviándolo al Hilo Principal (UI Thread)
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Alerta SMS y llamada enviadas", Toast.LENGTH_LONG).show()
        }

    } catch (e: Exception) {
        // Mostrar errores también en el Hilo Principal
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Error en alerta: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}