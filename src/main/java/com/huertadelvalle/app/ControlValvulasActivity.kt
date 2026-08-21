package com.huertadelvalle.app

import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class ControlValvulasActivity : AppCompatActivity() {

    private lateinit var tvEstado: Array<TextView>
    private lateinit var swValvula: Array<Switch>
    private lateinit var swModo: Array<Switch>
    private lateinit var btnVolver: Button

    private val nombreValvula = arrayOf(
        "Carga de agua", "Nutriente A", "Nutriente B", "Nutriente C", "Ácido pH"
    )

    private val estadoActual = BooleanArray(5)
    private var actualizandoDesdeServidor = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control_valvulas)

        tvEstado = arrayOf(
            findViewById(R.id.tvEstadoV1),
            findViewById(R.id.tvEstadoV2),
            findViewById(R.id.tvEstadoV3),
            findViewById(R.id.tvEstadoV4),
            findViewById(R.id.tvEstadoV5)
        )

        swValvula = arrayOf(
            findViewById(R.id.swV1),
            findViewById(R.id.swV2),
            findViewById(R.id.swV3),
            findViewById(R.id.swV4),
            findViewById(R.id.swV5)
        )

        swModo = arrayOf(
            findViewById(R.id.swModoV1),
            findViewById(R.id.swModoV2),
            findViewById(R.id.swModoV3),
            findViewById(R.id.swModoV4),
            findViewById(R.id.swModoV5)
        )

        btnVolver = findViewById(R.id.btnVolverValvulas)

        for (i in 0..4) {
            // Switch de encendido/apagado
            swValvula[i].setOnCheckedChangeListener { _, isChecked ->
                if (actualizandoDesdeServidor) return@setOnCheckedChangeListener
                val nuevaAccion = if (isChecked) "abrir" else "cerrar"
                controlarValvula(i + 1, nuevaAccion, i)
            }

            // Switch de modo Automático/Manual
            swModo[i].setOnCheckedChangeListener { _, esManual ->
                if (actualizandoDesdeServidor) return@setOnCheckedChangeListener
                swValvula[i].isEnabled = esManual
                controlarValvulaConModo(i + 1, esManual, i)
            }
        }

        btnVolver.setOnClickListener { finish() }

        cargarEstadoValvulas()
    }

    override fun onResume() {
        super.onResume()
        cargarEstadoValvulas()
    }

    private fun cargarEstadoValvulas() {
        val queue = Volley.newRequestQueue(this)
        val request = StringRequest(
            Request.Method.GET, Configuracion.URL_ESTADO_VALVULAS,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        val valvulas = json.getJSONArray("valvulas")

                        for (i in 0 until valvulas.length()) {
                            val v = valvulas.getJSONObject(i)
                            val idValvula = v.getInt("id_valvula")
                            val estado = v.getString("estado")
                            val modo = v.optString("modo", "manual")

                            val idx = idValvula - 1
                            if (idx in 0..4) {
                                val esManual = modo == "manual"

                                actualizandoDesdeServidor = true
                                swModo[idx].isChecked = esManual
                                actualizandoDesdeServidor = false

                                swValvula[idx].isEnabled = esManual

                                actualizarVista(idx, estado == "abierta")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al leer estado de válvulas", Toast.LENGTH_SHORT).show()
                }
            },
            { Toast.makeText(this, "No se pudo conectar al servidor", Toast.LENGTH_SHORT).show() }
        )
        queue.add(request)
    }

    private fun controlarValvula(idValvula: Int, accion: String, indice: Int) {
        swValvula[indice].isEnabled = false

        val body = JSONObject()
        body.put("id_valvula", idValvula)
        body.put("accion", accion)

        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_CONTROLAR_VALVULA, body,
            { response ->
                swValvula[indice].isEnabled = true
                if (response.optBoolean("success")) {
                    cargarEstadoValvulas()
                    Toast.makeText(
                        this,
                        "${nombreValvula[indice]}: ${if (accion == "abrir") "abriendo" else "cerrando"}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(this, "No se pudo actualizar la válvula", Toast.LENGTH_SHORT).show()
                }
            },
            {
                swValvula[indice].isEnabled = true
                Toast.makeText(this, "Error de conexión con el servidor", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    // Envía el cambio de modo de cualquier válvula. Si pasa a manual,
    // arranca cerrada por seguridad hasta que el usuario decida.
    private fun controlarValvulaConModo(idValvula: Int, manual: Boolean, indice: Int) {
        swModo[indice].isEnabled = false

        val body = JSONObject()
        body.put("id_valvula", idValvula)
        body.put("modo", if (manual) "manual" else "automatico")
        body.put("accion", "cerrar")

        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_CONTROLAR_VALVULA, body,
            { response ->
                swModo[indice].isEnabled = true
                if (response.optBoolean("success")) {
                    cargarEstadoValvulas()
                } else {
                    Toast.makeText(this, "No se pudo cambiar el modo", Toast.LENGTH_SHORT).show()
                }
            },
            {
                swModo[indice].isEnabled = true
                Toast.makeText(this, "Error de conexión con el servidor", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    private fun actualizarVista(indice: Int, abierta: Boolean) {
        estadoActual[indice] = abierta

        actualizandoDesdeServidor = true
        swValvula[indice].isChecked = abierta
        actualizandoDesdeServidor = false

        if (abierta) {
            tvEstado[indice].text = "abierta"
            tvEstado[indice].setTextColor(0xFF2E7D32.toInt())
        } else {
            tvEstado[indice].text = "cerrada"
            tvEstado[indice].setTextColor(0xFF888888.toInt())
        }
    }
}