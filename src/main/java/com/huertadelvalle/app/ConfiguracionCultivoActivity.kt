package com.huertadelvalle.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

class ConfiguracionCultivoActivity : AppCompatActivity() {

    // Vistas de la pantalla
    private lateinit var etNombreSemilla: EditText
    private lateinit var etNombreNutriente: EditText
    private lateinit var etComponente1Nombre: EditText
    private lateinit var etComponente1Dosis: EditText
    private lateinit var etComponente2Nombre: EditText
    private lateinit var etComponente2Dosis: EditText
    private lateinit var etComponente3Nombre: EditText
    private lateinit var etComponente3Dosis: EditText
    private lateinit var etPhMinimo: EditText
    private lateinit var etPhMaximo: EditText
    private lateinit var etTdsMinimo: EditText
    private lateinit var etTdsMaximo: EditText
    private lateinit var btnGuardarConfiguracion: Button
    private lateinit var btnVolverConfiguracion: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracion_cultivo)

        // Vincular vistas
        etNombreSemilla       = findViewById(R.id.etNombreSemilla)
        etNombreNutriente     = findViewById(R.id.etNombreNutriente)
        etComponente1Nombre   = findViewById(R.id.etComponente1Nombre)
        etComponente1Dosis    = findViewById(R.id.etComponente1Dosis)
        etComponente2Nombre   = findViewById(R.id.etComponente2Nombre)
        etComponente2Dosis    = findViewById(R.id.etComponente2Dosis)
        etComponente3Nombre   = findViewById(R.id.etComponente3Nombre)
        etComponente3Dosis    = findViewById(R.id.etComponente3Dosis)
        etPhMinimo            = findViewById(R.id.etPhMinimo)
        etPhMaximo             = findViewById(R.id.etPhMaximo)
        etTdsMinimo            = findViewById(R.id.etTdsMinimo)
        etTdsMaximo            = findViewById(R.id.etTdsMaximo)
        btnGuardarConfiguracion = findViewById(R.id.btnGuardarConfiguracion)
        btnVolverConfiguracion  = findViewById(R.id.btnVolverConfiguracion)

        // Obtener id_proyecto del Intent
        val idProyecto = intent.getIntExtra("id_proyecto", 0)

        // Precargar datos si vienen del Intent (edición)
        val semillaExistente = intent.getStringExtra("nombre_semilla")
        if (semillaExistente != null) {
            etNombreSemilla.setText(semillaExistente)
            etNombreNutriente.setText(intent.getStringExtra("nombre_comercial"))
            etPhMinimo.setText(intent.getStringExtra("ph_minimo"))
            etPhMaximo.setText(intent.getStringExtra("ph_maximo"))
            etTdsMinimo.setText(intent.getStringExtra("tds_minimo"))
            etTdsMaximo.setText(intent.getStringExtra("tds_maximo"))

            // Precargar componentes
            intent.getStringExtra("comp1_nombre")?.let { etComponente1Nombre.setText(it) }
            intent.getStringExtra("comp1_dosis")?.let { etComponente1Dosis.setText(it) }
            intent.getStringExtra("comp2_nombre")?.let { etComponente2Nombre.setText(it) }
            intent.getStringExtra("comp2_dosis")?.let { etComponente2Dosis.setText(it) }
            intent.getStringExtra("comp3_nombre")?.let { etComponente3Nombre.setText(it) }
            intent.getStringExtra("comp3_dosis")?.let { etComponente3Dosis.setText(it) }
        }

        // Tooltips
        val tvTooltipPh = findViewById<TextView>(R.id.tvTooltipPh)
        tvTooltipPh.setOnClickListener {
            Notificacion.informacion(this,
                "El pH mide la acidez del agua. Para hidroponía lo ideal es entre 5.5 y 6.5.")
        }

        val tvTooltipTds = findViewById<TextView>(R.id.tvTooltipTds)
        tvTooltipTds.setOnClickListener {
            Notificacion.informacion(this,
                "TDS mide la concentración de nutrientes en el agua en partes por millón (ppm).")
        }

        val tvTooltipDosis = findViewById<TextView>(R.id.tvTooltipDosis)
        tvTooltipDosis.setOnClickListener {
            Notificacion.informacion(
                this,
                "Puedes elegir de 1 a 20 dosis. Cada dosis abre la válvula durante 5 segundos."
            )
        }

        // Botón guardar
        btnGuardarConfiguracion.setOnClickListener {
            val semilla       = etNombreSemilla.text.toString().trim()
            val nutriente     = etNombreNutriente.text.toString().trim()
            val comp1Nombre   = etComponente1Nombre.text.toString().trim()
            val comp1Dosis    = etComponente1Dosis.text.toString().trim()
            val comp2Nombre   = etComponente2Nombre.text.toString().trim()
            val comp2Dosis    = etComponente2Dosis.text.toString().trim()
            val comp3Nombre   = etComponente3Nombre.text.toString().trim()
            val comp3Dosis    = etComponente3Dosis.text.toString().trim()
            val phMinimo      = etPhMinimo.text.toString().trim()
            val phMaximo      = etPhMaximo.text.toString().trim()
            val tdsMinimo     = etTdsMinimo.text.toString().trim()
            val tdsMaximo     = etTdsMaximo.text.toString().trim()

            // Limpiar errores anteriores
            etNombreSemilla.error       = null
            etNombreNutriente.error     = null
            etComponente1Nombre.error   = null
            etComponente1Dosis.error    = null
            etComponente2Nombre.error   = null
            etComponente2Dosis.error    = null
            etComponente3Nombre.error   = null
            etComponente3Dosis.error    = null
            etPhMinimo.error            = null
            etPhMaximo.error            = null
            etTdsMinimo.error           = null
            etTdsMaximo.error           = null

            // Validar campos obligatorios
            var hayError = false
            if (semilla.isEmpty()) {
                etNombreSemilla.error = "El nombre de la semilla es obligatorio"
                etNombreSemilla.requestFocus()
                hayError = true
            }
            if (nutriente.isEmpty()) {
                etNombreNutriente.error = "El nombre del nutriente es obligatorio"
                if (!hayError) etNombreNutriente.requestFocus()
                hayError = true
            }
            if (comp1Nombre.isEmpty()) {
                etComponente1Nombre.error = "El componente 1 es obligatorio"
                if (!hayError) etComponente1Nombre.requestFocus()
                hayError = true
            }
            val dosis1 = comp1Dosis.toIntOrNull()
            if (dosis1 == null || dosis1 !in 1..20) {
                etComponente1Dosis.error = "Ingresa entre 1 y 20 dosis"
                if (!hayError) etComponente1Dosis.requestFocus()
                hayError = true
            }

            // Valida componentes opcionales
            val dosis2 = comp2Dosis.toIntOrNull()
            if (comp2Nombre.isNotEmpty() || comp2Dosis.isNotEmpty()) {
                if (comp2Nombre.isEmpty()) {
                    etComponente2Nombre.error = "Ingresa el nombre"
                    if (!hayError) etComponente2Nombre.requestFocus()
                    hayError = true
                }
                if (dosis2 == null || dosis2 !in 1..20) {
                    etComponente2Dosis.error = "Ingresa entre 1 y 20 dosis"
                    if (!hayError) etComponente2Dosis.requestFocus()
                    hayError = true
                }
            }

            val dosis3 = comp3Dosis.toIntOrNull()
            if (comp3Nombre.isNotEmpty() || comp3Dosis.isNotEmpty()) {
                if (comp3Nombre.isEmpty()) {
                    etComponente3Nombre.error = "Ingresa el nombre"
                    if (!hayError) etComponente3Nombre.requestFocus()
                    hayError = true
                }
                if (dosis3 == null || dosis3 !in 1..20) {
                    etComponente3Dosis.error = "Ingresa entre 1 y 20 dosis"
                    if (!hayError) etComponente3Dosis.requestFocus()
                    hayError = true
                }
            }
            if (phMinimo.isEmpty()) {
                etPhMinimo.error = "El pH mínimo es obligatorio"
                if (!hayError) etPhMinimo.requestFocus()
                hayError = true
            }
            if (phMaximo.isEmpty()) {
                etPhMaximo.error = "El pH máximo es obligatorio"
                if (!hayError) etPhMaximo.requestFocus()
                hayError = true
            }
            if (tdsMinimo.isEmpty()) {
                etTdsMinimo.error = "El TDS mínimo es obligatorio"
                if (!hayError) etTdsMinimo.requestFocus()
                hayError = true
            }
            if (tdsMaximo.isEmpty()) {
                etTdsMaximo.error = "El TDS máximo es obligatorio"
                if (!hayError) etTdsMaximo.requestFocus()
                hayError = true
            }

            if (hayError) return@setOnClickListener

            // Validar rango de pH
            val phMin = phMinimo.toDoubleOrNull()
            val phMax = phMaximo.toDoubleOrNull()
            if (phMin == null || phMax == null || phMin < 0 || phMin > 14 || phMax < 0 || phMax > 14) {
                Notificacion.error(this, "El pH debe estar entre 0 y 14")
                return@setOnClickListener
            }
            if (phMin >= phMax) {
                Notificacion.error(this, "El pH mínimo debe ser menor al máximo")
                return@setOnClickListener
            }

            // Validar TDS
            val tdsMin = tdsMinimo.toIntOrNull()
            val tdsMax = tdsMaximo.toIntOrNull()
            if (tdsMin == null || tdsMax == null || tdsMin < 0 || tdsMax < 0) {
                Notificacion.error(this, "El TDS debe ser un número positivo")
                return@setOnClickListener
            }
            if (tdsMin >= tdsMax) {
                Notificacion.error(this, "El TDS mínimo debe ser menor al máximo")
                return@setOnClickListener
            }

            // Armar componentes
            val componentes = JSONArray()
            componentes.put(JSONObject().apply {
                put("nombre", comp1Nombre)
                put("cantidad_dosis", dosis1)
            })
            if (comp2Nombre.isNotEmpty()) {
                componentes.put(JSONObject().apply {
                    put("nombre", comp2Nombre)
                    put("cantidad_dosis", dosis2)
                })
            }
            if (comp3Nombre.isNotEmpty()) {
                componentes.put(JSONObject().apply {
                    put("nombre", comp3Nombre)
                    put("cantidad_dosis", dosis3)
                })
            }

            guardarConfiguracion(idProyecto, semilla, nutriente, componentes,
                phMinimo, phMaximo, tdsMinimo, tdsMaximo)
        }

        // Botón volver
        btnVolverConfiguracion.setOnClickListener {
            finish()
        }
    }

    private fun guardarConfiguracion(
        idProyecto: Int, semilla: String, nutriente: String,
        componentes: JSONArray, phMinimo: String, phMaximo: String,
        tdsMinimo: String, tdsMaximo: String) {

        btnGuardarConfiguracion.isEnabled = false
        btnGuardarConfiguracion.text = "Guardando..."

        val queue = Volley.newRequestQueue(this)

        val datos = JSONObject()
        datos.put("id_proyecto",      idProyecto)
        datos.put("nombre_semilla",   semilla)
        datos.put("nombre_nutriente", nutriente)
        datos.put("componentes",      componentes)
        datos.put("ph_minimo",        phMinimo)
        datos.put("ph_maximo",        phMaximo)
        datos.put("tds_minimo",       tdsMinimo)
        datos.put("tds_maximo",       tdsMaximo)

        val peticion = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_CONFIGURACION_CULTIVO, datos,
            { respuesta ->
                btnGuardarConfiguracion.isEnabled = true
                btnGuardarConfiguracion.text = "Guardar Configuración"

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
                btnGuardarConfiguracion.isEnabled = true
                btnGuardarConfiguracion.text = "Guardar Configuración"
                Notificacion.error(this, "Error de conexión")
            }
        )

        queue.add(peticion)
    }
}
