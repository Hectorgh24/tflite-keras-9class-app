package com.empresa.aplicaciontensorflowliteandkeras

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.content.ContextCompat

fun ejecutarProtocoloEmergencia(context: Context, phoneNumber: String) {
    val mensaje = "ALERTA SOS: He sufrido una posible caída o síncope. Necesito asistencia inmediata."

    // 1. Ejecutar intento de WhatsApp
    try {
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(mensaje)}")
        val waIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(waIntent)
    } catch (e: Exception) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "WhatsApp no instalado o falló. Ejecutando SMS y llamada.", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Ejecutar protocolos nativos (SMS y Llamada) con validación de seguridad
    try {
        // Enviar SMS
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            if (smsManager != null) {
                smsManager.sendTextMessage(phoneNumber, null, mensaje, null, null)
            }
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Error: Permiso de SMS no concedido", Toast.LENGTH_LONG).show()
            }
        }

        // Realizar Llamada Telefónica
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(callIntent)
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Error: Permiso de Llamada no concedido", Toast.LENGTH_LONG).show()
            }
        }

    } catch (e: Exception) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Error en alerta nativa: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}