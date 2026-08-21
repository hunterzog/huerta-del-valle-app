package com.huertadelvalle.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class NivelAguaActivity : AppCompatActivity() {

    private lateinit var tvNivelAgua: TextView
    private lateinit var tvUltimaActualizacion: TextView
    private lateinit var etDistanciaLleno: EditText
    private lateinit var etDistanciaVacio: EditText
    private lateinit var btnGuardarUmbrales: Button
    private lateinit var btnVolver: Button

    private var idProyecto: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nivel_agua)

        idProyecto = intent.getIntExtra("id_proyecto", 0)

        tvNivelAgua = findViewById(R.id.tvNivelAgua)
        tvUltimaActualizacion = findViewById(R.id.tvUltimaActualizacion)
        etDistanciaLleno = findViewById(R.id.etDistanciaLleno)
        etDistanciaVacio = findViewById(R.id.etDistanciaVacio)
        btnGuardarUmbrales = findViewById(R.id.btnGuardarUmbrales)
        btnVolver = findViewById(R.id.btnVolverNivelAgua)

        btnGuardarUmbrales.setOnClickListener { guardarUmbrales() }
        btnVolver.setOnClickListener { finish() }

        cargarNivelAgua()
        cargarUmbrales()
    }

    override fun onResume() {
        super.onResume()
        cargarNivelAgua()
    }

    private fun cargarNivelAgua() {
        val queue = Volley.newRequestQueue(this)
        val datos = JSONObject()
        datos.put("id_proyecto", idProyecto)

        val peticion = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_ULTIMA_MEDICION, datos,
            { respuesta ->
                if (respuesta.optBoolean("success")) {
                    if (respuesta.isNull("nivel_agua_cm")) {
                        tvNivelAgua.text = "Sin datos aún"
                    } else {
                        val distancia = respuesta.getDouble("nivel_agua_cm")
                        tvNivelAgua.text = "%.1f cm de distancia".format(distancia)
                    }
                    tvUltimaActualizacion.text = "Última lectura: ${respuesta.optString("fecha_hora")}"
                } else {
                    tvNivelAgua.text = "Sin mediciones todavía"
                }
            },
            { tvNivelAgua.text = "Error al consultar" }
        )
        queue.add(peticion)
    }

    private fun cargarUmbrales() {
        val queue = Volley.newRequestQueue(this)
        val request = StringRequest(
            Request.Method.GET, Configuracion.URL_OBTENER_CONFIG_TANQUE,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        etDistanciaLleno.setText(json.getString("distancia_tanque_lleno"))
                        etDistanciaVacio.setText(json.getString("distancia_tanque_vacio"))
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al leer umbrales", Toast.LENGTH_SHORT).show()
                }
            },
            { Toast.makeText(this, "No se pudo conectar al servidor", Toast.LENGTH_SHORT).show() }
        )
        queue.add(request)
    }

    private fun guardarUmbrales() {
        val lleno = etDistanciaLleno.text.toString().trim()
        val vacio = etDistanciaVacio.text.toString().trim()

        if (lleno.isEmpty() || vacio.isEmpty()) {
            Toast.makeText(this, "Completá los 2 valores", Toast.LENGTH_SHORT).show()
            return
        }

        val llenoNum = lleno.toDoubleOrNull()
        val vacioNum = vacio.toDoubleOrNull()

        if (llenoNum == null || vacioNum == null) {
            Toast.makeText(this, "Los valores deben ser números", Toast.LENGTH_SHORT).show()
            return
        }

        if (llenoNum >= vacioNum) {
            Toast.makeText(this, "El valor de lleno debe ser menor al de vacío", Toast.LENGTH_SHORT).show()
            return
        }

        btnGuardarUmbrales.isEnabled = false
        btnGuardarUmbrales.text = "Guardando..."

        val body = JSONObject()
        body.put("distancia_tanque_lleno", llenoNum)
        body.put("distancia_tanque_vacio", vacioNum)

        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_ACTUALIZAR_CONFIG_TANQUE, body,
            { response ->
                btnGuardarUmbrales.isEnabled = true
                btnGuardarUmbrales.text = "Guardar umbrales"

                if (response.optBoolean("success")) {
                    Toast.makeText(this, "Umbrales actualizados", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, response.optString("mensaje", "Error al guardar"), Toast.LENGTH_SHORT).show()
                }
            },
            {
                btnGuardarUmbrales.isEnabled = true
                btnGuardarUmbrales.text = "Guardar umbrales"
                Toast.makeText(this, "Error de conexión con el servidor", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }
}