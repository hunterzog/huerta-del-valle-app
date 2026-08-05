package com.huertadelvalle.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class RegistroActivity : AppCompatActivity() {

    // Vistas de la pantalla de registro
    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etCorreoReg: EditText
    private lateinit var etUsuarioReg: EditText
    private lateinit var etContrasenaReg: EditText
    private lateinit var btnRegistrar: Button
    private lateinit var btnVolver: Button
    private lateinit var tvTooltipUsuario: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // Vincular vistas con el XML
        etNombre        = findViewById(R.id.etNombre)
        etApellido      = findViewById(R.id.etApellido)
        etCorreoReg     = findViewById(R.id.etCorreoReg)
        etUsuarioReg    = findViewById(R.id.etUsuarioReg)
        etContrasenaReg = findViewById(R.id.etContrasenaReg)
        btnRegistrar    = findViewById(R.id.btnRegistrar)
        btnVolver       = findViewById(R.id.btnVolver)
        tvTooltipUsuario = findViewById(R.id.tvTooltipUsuario)

        // Tooltip campo usuario
        tvTooltipUsuario.setOnClickListener {
            Notificacion.informacion(this,
                "El usuario debe ser único. No distingue mayúsculas.")
        }

        // Botón registrar
        btnRegistrar.setOnClickListener {
            val nombre    = etNombre.text.toString().trim()
            val apellido  = etApellido.text.toString().trim()
            val correo    = etCorreoReg.text.toString().trim()
            val usuario   = etUsuarioReg.text.toString().trim()
            val contrasena = etContrasenaReg.text.toString().trim()

            // Validar campos vacíos
            // Limpiar errores anteriores
            etNombre.error = null
            etApellido.error = null
            etUsuarioReg.error = null
            etContrasenaReg.error = null

// Validar campos con error junto al campo
            var hayError = false
            if (nombre.isEmpty()) {
                etNombre.error = "El nombre es obligatorio"
                etNombre.requestFocus()
                hayError = true
            }
            if (apellido.isEmpty()) {
                etApellido.error = "El apellido es obligatorio"
                if (!hayError) etApellido.requestFocus()
                hayError = true
            }
            if (usuario.isEmpty()) {
                etUsuarioReg.error = "El usuario es obligatorio"
                if (!hayError) etUsuarioReg.requestFocus()
                hayError = true
            }
            if (contrasena.isEmpty()) {
                etContrasenaReg.error = "La contraseña es obligatoria"
                if (!hayError) etContrasenaReg.requestFocus()
                hayError = true
            }

            if (!hayError) {
                registrar(nombre, apellido, usuario, correo, contrasena)
            }
        }

        // Botón volver al login
        btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun registrar(nombre: String, apellido: String,
                          usuario: String, correo: String, contrasena: String) {
        // Deshabilitar botón para evitar duplicados
        btnRegistrar.isEnabled = false
        btnRegistrar.text = "Registrando..."

        val queue = Volley.newRequestQueue(this)

        val datos = JSONObject()
        datos.put("nombre",     nombre)
        datos.put("apellido",   apellido)
        datos.put("correo",     correo)
        datos.put("usuario",    usuario)
        datos.put("contrasena", contrasena)

        // Petición con UTF-8 forzado
        val peticion = object : JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_REGISTRO, datos,
            { respuesta ->
                btnRegistrar.isEnabled = true
                btnRegistrar.text = "Registrar"

                val success = respuesta.getBoolean("success")
                val mensaje = respuesta.getString("mensaje")
                if (success) Notificacion.exito(this, mensaje)
                else Notificacion.error(this, mensaje)

                if (success) {
                    finish()
                }
            },
            { error ->
                btnRegistrar.isEnabled = true
                btnRegistrar.text = "Registrar"
                Notificacion.error(this, "Error de conexión")
            }
        ) {
            // Forzar UTF-8 para caracteres especiales como ñ y acentos
            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }
        }

        queue.add(peticion)
    }
}