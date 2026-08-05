package com.huertadelvalle.app

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.View
import com.google.android.material.snackbar.Snackbar

object Notificacion {

    // Mensaje de éxito en verde
    fun exito(context: Context, mensaje: String) {
        mostrar(context, mensaje, "#2E7D32")
    }

    // Mensaje de error en rojo
    fun error(context: Context, mensaje: String) {
        mostrar(context, mensaje, "#B71C1C")
    }

    // Mensaje de advertencia en naranja
    fun advertencia(context: Context, mensaje: String) {
        mostrar(context, mensaje, "#E65100")
    }

    // Mensaje de información en azul
    fun informacion(context: Context, mensaje: String) {
        mostrar(context, mensaje, "#1565C0")
    }

    private fun mostrar(context: Context, mensaje: String, colorFondo: String) {
        val activity = context as? Activity ?: return
        val vista = activity.findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(vista, mensaje, Snackbar.LENGTH_SHORT)
        snackbar.setBackgroundTint(Color.parseColor(colorFondo))
        snackbar.setTextColor(Color.WHITE)
        snackbar.show()
    }
}