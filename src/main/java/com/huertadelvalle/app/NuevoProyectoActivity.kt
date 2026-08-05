package com.huertadelvalle.app

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.Calendar

class NuevoProyectoActivity : AppCompatActivity() {

    // Vistas de la pantalla
    private lateinit var etNombreProyecto: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var btnFechaInicio: Button
    private lateinit var btnGuardarProyecto: Button
    private lateinit var btnVolverProyectos: Button

    // Fecha seleccionada
    private var fechaInicio = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nuevo_proyecto)

        // Vincular vistas
        etNombreProyecto   = findViewById(R.id.etNombreProyecto)
        etDescripcion      = findViewById(R.id.etDescripcion)
        btnFechaInicio     = findViewById(R.id.btnFechaInicio)
        btnGuardarProyecto = findViewById(R.id.btnGuardarProyecto)
        btnVolverProyectos = findViewById(R.id.btnVolverProyectos)

        // Obtener id_usuario del Intent
        val idUsuario = intent.getIntExtra("id_usuario", 0)

        // Botón seleccionar fecha
        btnFechaInicio.setOnClickListener {
            val calendario = Calendar.getInstance()
            val anio = calendario.get(Calendar.YEAR)
            val mes  = calendario.get(Calendar.MONTH)
            val dia  = calendario.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, y, m, d ->
                fechaInicio = "$y-${m + 1}-$d"
                btnFechaInicio.text = fechaInicio
            }, anio, mes, dia).show()
        }

        // Botón guardar proyecto
        btnGuardarProyecto.setOnClickListener {
            val nombre      = etNombreProyecto.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()

            // Limpiar errores anteriores
            etNombreProyecto.error = null

            // Validar campos con error junto al campo
            if (nombre.isEmpty()) {
                etNombreProyecto.error = "El nombre del proyecto es obligatorio"
                etNombreProyecto.requestFocus()
                return@setOnClickListener
            }
            if (fechaInicio.isEmpty()) {
                Notificacion.advertencia(this, "Seleccioná la fecha de inicio")
                return@setOnClickListener
            }

            guardarProyecto(idUsuario, nombre, descripcion, fechaInicio)
        }

        // Botón volver
        btnVolverProyectos.setOnClickListener {
            finish()
        }
    }

    private fun guardarProyecto(idUsuario: Int, nombre: String,
                                descripcion: String, fechaInicio: String) {
        val queue = Volley.newRequestQueue(this)

        val datos = JSONObject()
        datos.put("id_usuario",   idUsuario)
        datos.put("nombre",       nombre)
        datos.put("descripcion",  descripcion)
        datos.put("fecha_inicio", fechaInicio)

        val peticion = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_NUEVO_PROYECTO, datos,
            { respuesta ->
                val success = respuesta.getBoolean("success")
                val mensaje = respuesta.getString("mensaje")

                if (success) {
                    Notificacion.exito(this, mensaje)
                    val idProyecto = respuesta.getInt("id_proyecto")
                    val intent = Intent(this, ConfiguracionCultivoActivity::class.java)
                    intent.putExtra("id_proyecto", idProyecto)
                    startActivity(intent)
                    finish()
                } else {
                    Notificacion.error(this, mensaje)
                }
            },
            { _ ->
                Notificacion.error(this, "Error de conexión")
            }
        )

        queue.add(peticion)
    }
}