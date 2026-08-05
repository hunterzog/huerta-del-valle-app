package com.huertadelvalle.app

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class ModificarUsuarioActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etUsuario: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etNuevaContrasena: EditText
    private lateinit var spinnerRol: Spinner
    private lateinit var spinnerEstado: Spinner
    private lateinit var btnModificar: Button
    private lateinit var btnCancelar: Button
    private lateinit var tvTooltipUsuario: TextView

    private var idUsuarioEditar = 0
    private var rol = ""
    private var idUsuario = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modificar_usuario)

        // Vincular vistas
        etNombre          = findViewById(R.id.etNombre)
        etApellido        = findViewById(R.id.etApellido)
        etUsuario         = findViewById(R.id.etUsuario)
        etCorreo          = findViewById(R.id.etCorreo)
        etNuevaContrasena = findViewById(R.id.etNuevaContrasena)
        spinnerRol        = findViewById(R.id.spinnerRol)
        spinnerEstado     = findViewById(R.id.spinnerEstado)
        btnModificar      = findViewById(R.id.btnModificar)
        btnCancelar       = findViewById(R.id.btnCancelar)
        tvTooltipUsuario  = findViewById(R.id.tvTooltipUsuario)

        // Obtener datos del Intent
        idUsuarioEditar = intent.getIntExtra("id_usuario_editar", 0)
        rol             = intent.getStringExtra("rol") ?: ""
        idUsuario       = intent.getIntExtra("id_usuario", 0)

        // Precargar datos
        etNombre.setText(intent.getStringExtra("nombre"))
        etApellido.setText(intent.getStringExtra("apellido"))
        etUsuario.setText(intent.getStringExtra("usuario"))
        etCorreo.setText(intent.getStringExtra("correo"))

        // Spinner Rol
        val roles = arrayOf("usuario", "administrador")
        spinnerRol.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        val rolActual = intent.getStringExtra("rol_editar") ?: "usuario"
        spinnerRol.setSelection(roles.indexOf(rolActual))

        // Spinner Estado
        val estados = arrayOf("activo", "inactivo")
        spinnerEstado.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, estados)
        val estadoActual = intent.getStringExtra("estado") ?: "activo"
        spinnerEstado.setSelection(estados.indexOf(estadoActual))

        // Tooltip usuario
        tvTooltipUsuario.setOnClickListener {
            Notificacion.informacion(this,
                "El usuario debe ser único. No distingue mayúsculas de minúsculas.")
        }

        // Botón modificar
        btnModificar.setOnClickListener {
            if (validar()) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Confirmar modificación")
                    .setMessage("¿Estás seguro que querés modificar los datos de este usuario?")
                    .setPositiveButton("Modificar") { _, _ ->
                        modificarUsuario()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }

        // Botón cancelar
        btnCancelar.setOnClickListener { finish() }
    }

    private fun validar(): Boolean {
        if (etNombre.text.toString().trim().isEmpty()) {
            etNombre.error = "El nombre es obligatorio"
            etNombre.requestFocus()
            return false
        }
        if (etApellido.text.toString().trim().isEmpty()) {
            etApellido.error = "El apellido es obligatorio"
            etApellido.requestFocus()
            return false
        }
        if (etUsuario.text.toString().trim().isEmpty()) {
            etUsuario.error = "El usuario es obligatorio"
            etUsuario.requestFocus()
            return false
        }
        return true
    }

    private fun modificarUsuario() {
        // Deshabilitar botón para evitar duplicados
        btnModificar.isEnabled = false
        btnModificar.text = "Modificando..."

        val queue = Volley.newRequestQueue(this)

        val datos = JSONObject()
        datos.put("rol",       rol)
        datos.put("id_usuario", idUsuarioEditar)
        datos.put("nombre",    etNombre.text.toString().trim())
        datos.put("apellido",  etApellido.text.toString().trim())
        datos.put("usuario",   etUsuario.text.toString().trim())
        datos.put("correo",    etCorreo.text.toString().trim())
        datos.put("rol_nuevo", spinnerRol.selectedItem.toString())
        datos.put("estado",    spinnerEstado.selectedItem.toString())

        // Agregar nueva contraseña si se ingresó
        val nuevaContrasena = etNuevaContrasena.text.toString().trim()
        if (nuevaContrasena.isNotEmpty()) {
            datos.put("nueva_contrasena", nuevaContrasena)
        }

        val peticion = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_MODIFICAR_USUARIO, datos,
            { respuesta ->
                btnModificar.isEnabled = true
                btnModificar.text = "Modificar"

                val success = respuesta.getBoolean("success")
                val mensaje = respuesta.getString("mensaje")

                if (success) {
                    Notificacion.exito(this, mensaje)
                    finish()
                } else {
                    Notificacion.error(this, mensaje)
                }
            },
            { _ ->
                btnModificar.isEnabled = true
                btnModificar.text = "Modificar"
                Notificacion.error(this, "Error de conexión")
            }
        )

        queue.add(peticion)
    }
}