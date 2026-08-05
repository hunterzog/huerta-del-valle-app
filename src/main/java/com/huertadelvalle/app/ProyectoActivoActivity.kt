package com.huertadelvalle.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProyectoActivoActivity : AppCompatActivity() {

    // Vistas de la pantalla
    private lateinit var tvNombreProyectoActivo: TextView
    private lateinit var tvFechaProyecto: TextView
    private lateinit var tvSemillaProyecto: TextView
    private lateinit var tvNutrienteProyecto: TextView
    private lateinit var tvDiasActivo: TextView
    private lateinit var btnMonitoreo: Button
    private lateinit var btnHistorial: Button
    private lateinit var btnEvaluacion: Button
    private lateinit var btnFinalizarProyecto: Button
    private lateinit var btnVolverActivo: Button
    private lateinit var btnEditarConfiguracion: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proyecto_activo)

        // Vincular vistas
        tvNombreProyectoActivo = findViewById(R.id.tvNombreProyectoActivo)
        tvFechaProyecto        = findViewById(R.id.tvFechaProyecto)
        tvSemillaProyecto      = findViewById(R.id.tvSemillaProyecto)
        tvNutrienteProyecto    = findViewById(R.id.tvNutrienteProyecto)
        tvDiasActivo           = findViewById(R.id.tvDiasActivo)
        btnMonitoreo           = findViewById(R.id.btnMonitoreo)
        btnHistorial           = findViewById(R.id.btnHistorial)
        btnEvaluacion          = findViewById(R.id.btnEvaluacion)
        btnFinalizarProyecto   = findViewById(R.id.btnFinalizarProyecto)
        btnVolverActivo        = findViewById(R.id.btnVolverActivo)
        btnEditarConfiguracion = findViewById(R.id.btnEditarConfiguracion)

        // Obtener datos del Intent
        val idProyecto  = intent.getIntExtra("id_proyecto", 0)
        val nombre      = intent.getStringExtra("nombre") ?: ""
        val fechaInicio = intent.getStringExtra("fecha_inicio") ?: ""
        val semilla     = intent.getStringExtra("semilla") ?: ""
        val nutriente   = intent.getStringExtra("nutriente") ?: ""

        // Mostrar datos
        tvNombreProyectoActivo.text = nombre
        tvFechaProyecto.text        = "Inicio: $fechaInicio"
        tvSemillaProyecto.text      = "🌱 Semilla: $semilla"
        tvNutrienteProyecto.text    = "🧪 Nutriente: $nutriente"

        // Calcular días activo
        try {
            val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val inicio  = formato.parse(fechaInicio)
            val hoy     = Date()
            val diff    = hoy.time - inicio!!.time
            val dias    = (diff / (1000 * 60 * 60 * 24)).toInt()
            tvDiasActivo.text = "📅 Día $dias del proyecto"
        } catch (e: Exception) {
            tvDiasActivo.text = "📅 Día 1 del proyecto"
        }

        // Botón monitoreo
        btnMonitoreo.setOnClickListener {
            val intent = Intent(this, MonitoreoActivity::class.java)
            intent.putExtra("id_proyecto", idProyecto)
            startActivity(intent)
        }

        // Botón historial
        btnHistorial.setOnClickListener {
            Notificacion.informacion(this, "Próximamente disponible")
        }

        // Botón evaluación
        btnEvaluacion.setOnClickListener {
            Notificacion.informacion(this, "Próximamente disponible")
        }

        // Botón editar configuración
        btnEditarConfiguracion.setOnClickListener {
            obtenerConfiguracion(idProyecto)
        }

        // Botón finalizar proyecto
        btnFinalizarProyecto.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Finalizar proyecto")
                .setMessage("¿Estás seguro que querés finalizar este proyecto?")
                .setPositiveButton("Finalizar") { _, _ ->
                    finalizarProyecto(idProyecto)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // Botón volver
        btnVolverActivo.setOnClickListener {
            finish()
        }
    }

    private fun finalizarProyecto(idProyecto: Int) {
        val queue = Volley.newRequestQueue(this)

        val datos = JSONObject()
        datos.put("id_proyecto", idProyecto)

        val peticion = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_FINALIZAR_PROYECTO, datos,
            { respuesta ->
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
                Notificacion.error(this, "Error de conexión")
            }
        )

        queue.add(peticion)
    }

    private fun obtenerConfiguracion(idProyecto: Int) {
        val queue = Volley.newRequestQueue(this)

        val datos = JSONObject()
        datos.put("id_proyecto", idProyecto)

        val peticion = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_OBTENER_CONFIGURACION, datos,
            { respuesta ->
                val success = respuesta.getBoolean("success")
                if (success) {
                    val intent = Intent(this, ConfiguracionCultivoActivity::class.java)
                    intent.putExtra("id_proyecto",       idProyecto)
                    intent.putExtra("nombre_semilla",    respuesta.getString("nombre_semilla"))
                    intent.putExtra("nombre_comercial",  respuesta.getString("nombre_comercial"))
                    intent.putExtra("ph_minimo",         respuesta.getString("ph_minimo"))
                    intent.putExtra("ph_maximo",         respuesta.getString("ph_maximo"))
                    intent.putExtra("tds_minimo",        respuesta.getString("tds_minimo"))
                    intent.putExtra("tds_maximo",        respuesta.getString("tds_maximo"))
                    intent.putExtra("horas_totales_dia", respuesta.optDouble("horas_totales_dia", 0.0).toString())
                    intent.putExtra("riegos_por_dia",    respuesta.optInt("riegos_por_dia", 1).toString())

                    // Pasar componentes
                    val componentes = if (respuesta.has("componentes"))
                        respuesta.getJSONArray("componentes")
                    else
                        org.json.JSONArray()
                    if (componentes.length() > 0) {
                        intent.putExtra("comp1_nombre",   componentes.getJSONObject(0).getString("nombre"))
                        intent.putExtra("comp1_cantidad", componentes.getJSONObject(0).getString("cantidad"))
                    }
                    if (componentes.length() > 1) {
                        intent.putExtra("comp2_nombre",   componentes.getJSONObject(1).getString("nombre"))
                        intent.putExtra("comp2_cantidad", componentes.getJSONObject(1).getString("cantidad"))
                    }
                    if (componentes.length() > 2) {
                        intent.putExtra("comp3_nombre",   componentes.getJSONObject(2).getString("nombre"))
                        intent.putExtra("comp3_cantidad", componentes.getJSONObject(2).getString("cantidad"))
                    }
                    startActivity(intent)
                } else {
                    val intent = Intent(this, ConfiguracionCultivoActivity::class.java)
                    intent.putExtra("id_proyecto", idProyecto)
                    startActivity(intent)
                }
            },
            { _ ->
                Notificacion.error(this, "Error de conexión")
            }
        )

        queue.add(peticion)
    }
}