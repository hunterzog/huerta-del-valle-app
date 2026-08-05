package com.huertadelvalle.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    // Vistas de la pantalla
    private lateinit var etUsuario: EditText
    private lateinit var etContrasena: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegistro: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Vincular vistas con el XML
        etUsuario    = findViewById(R.id.etUsuario)
        etContrasena = findViewById(R.id.etContrasena)
        btnLogin     = findViewById(R.id.btnLogin)
        btnRegistro  = findViewById(R.id.btnRegistro)

        // Acción del botón login
        btnLogin.setOnClickListener {
            val usuario   = etUsuario.text.toString().trim()
            val contrasena = etContrasena.text.toString().trim()

            // Limpiar errores anteriores
            etUsuario.error    = null
            etContrasena.error = null

            // Validar campos con error junto al campo
            var hayError = false
            if (usuario.isEmpty()) {
                etUsuario.error = "El usuario es obligatorio"
                etUsuario.requestFocus()
                hayError = true
            }
            if (contrasena.isEmpty()) {
                etContrasena.error = "La contraseña es obligatoria"
                if (!hayError) etContrasena.requestFocus()
                hayError = true
            }

            if (!hayError) {
                login(usuario, contrasena)
            }
        }

        // Botón ir a registro
        btnRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
    }

    private fun login(usuario: String, contrasena: String) {
        val queue = Volley.newRequestQueue(this)

        val datos = JSONObject()
        datos.put("usuario",    usuario)
        datos.put("contrasena", contrasena)

        val peticion = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_LOGIN, datos,
            { respuesta ->
                val success = respuesta.getBoolean("success")
                val mensaje = respuesta.getString("mensaje")

                if (success) {
                    val user      = respuesta.getJSONObject("usuario")
                    val nombre    = user.getString("nombre")
                    val apellido  = user.getString("apellido")
                    val rol       = user.getString("rol")
                    val idUsuario = user.getInt("id_usuario")

                    // Guardar sesión
                    val sesion = SesionManager(this)
                    sesion.guardarSesion(idUsuario, nombre, apellido, rol)

                    // Mensaje de éxito
                    Notificacion.exito(this, "Bienvenido $nombre $apellido")

                    val intent = Intent(this, ProyectosActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Mensaje de error de credenciales
                    Notificacion.error(this, mensaje)
                }
            },
            { error ->
                // Error de conexión
                Notificacion.error(this, "Error de conexión")
            }
        )

        queue.add(peticion)
    }
}