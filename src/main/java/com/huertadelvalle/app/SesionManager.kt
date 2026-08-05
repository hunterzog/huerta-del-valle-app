package com.huertadelvalle.app

import android.content.Context
import android.content.SharedPreferences

class SesionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "HuertaDelValleSesion", Context.MODE_PRIVATE
    )

    companion object {
        const val KEY_ID_USUARIO = "id_usuario"
        const val KEY_NOMBRE     = "nombre"
        const val KEY_APELLIDO   = "apellido"
        const val KEY_ROL        = "rol"
        const val KEY_LOGUEADO   = "logueado"
    }

    // Guardar sesión al hacer login
    fun guardarSesion(idUsuario: Int, nombre: String, apellido: String, rol: String) {
        val editor = prefs.edit()
        editor.putInt(KEY_ID_USUARIO, idUsuario)
        editor.putString(KEY_NOMBRE,   nombre)
        editor.putString(KEY_APELLIDO, apellido)
        editor.putString(KEY_ROL,      rol)
        editor.putBoolean(KEY_LOGUEADO, true)
        editor.apply()
    }

    // Obtener datos de la sesión
    fun getIdUsuario(): Int    = prefs.getInt(KEY_ID_USUARIO, 0)
    fun getNombre(): String    = prefs.getString(KEY_NOMBRE, "") ?: ""
    fun getApellido(): String  = prefs.getString(KEY_APELLIDO, "") ?: ""
    fun getRol(): String       = prefs.getString(KEY_ROL, "") ?: ""
    fun isLogueado(): Boolean  = prefs.getBoolean(KEY_LOGUEADO, false)

    // Cerrar sesión
    fun cerrarSesion() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}