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

class ControlBombasActivity : AppCompatActivity() {

    private lateinit var swModo: Array<Switch>      // [0]=Bomba A, [1]=Bomba B
    private lateinit var swEstado: Array<Switch>
    private lateinit var tvEstado: Array<TextView>
    private lateinit var btnVolver: Button

    // Nombres visibles en la aplicación.
    private val nombreBomba = arrayOf("Bomba A - Mezcla", "Bomba B - Riego")

    // Guardan lo que sabemos del servidor, para no pisarnos con el usuario
    private val modoManualActual = BooleanArray(2)
    private val estadoManualActual = BooleanArray(2)

    // Evita que mover un switch por código dispare el listener como si
    // el usuario lo hubiera tocado
    private var actualizandoDesdeServidor = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control_bombas)

        swModo = arrayOf(findViewById(R.id.swModoA), findViewById(R.id.swModoB))
        swEstado = arrayOf(findViewById(R.id.swEstadoA), findViewById(R.id.swEstadoB))
        tvEstado = arrayOf(findViewById(R.id.tvEstadoA), findViewById(R.id.tvEstadoB))
        btnVolver = findViewById(R.id.btnVolverBombas)

        // Un mismo bloque de listeners para las 2 bombas, usando el índice (0 o 1)
        for (i in 0..1) {
            // Switch de MODO (Automático/Manual)
            swModo[i].setOnCheckedChangeListener { _, esManual ->
                if (actualizandoDesdeServidor) return@setOnCheckedChangeListener

                // El switch de encendido/apagado solo se puede tocar en modo Manual
                swEstado[i].isEnabled = esManual

                // Al cambiar el modo, mandamos el estado tal cual está ahora
                // (si recién pasa a manual, arranca apagada por seguridad)
                val estadoAEnviar = if (esManual) swEstado[i].isChecked else false
                enviarComando(i + 1, esManual, estadoAEnviar, i)
            }

            // Switch de ENCENDIDO/APAGADO (solo activo si el modo es Manual)
            swEstado[i].setOnCheckedChangeListener { _, encendida ->
                if (actualizandoDesdeServidor) return@setOnCheckedChangeListener

                enviarComando(i + 1, true, encendida, i)
            }
        }

        btnVolver.setOnClickListener { finish() }

        cargarEstadoBombas()
    }

    override fun onResume() {
        super.onResume()
        cargarEstadoBombas()
    }

    private fun cargarEstadoBombas() {
        val queue = Volley.newRequestQueue(this)
        val request = StringRequest(
            Request.Method.GET, Configuracion.URL_ESTADO_BOMBAS,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        val bombas = json.getJSONArray("bombas")
                        for (i in 0 until bombas.length()) {
                            val b = bombas.getJSONObject(i)
                            val idBomba = b.getInt("id_bomba")       // 1 o 2
                            val modo = b.getString("modo")            // "automatico" o "manual"
                            val estado = b.getString("estado_manual") // "encendida" o "apagada"

                            val idx = idBomba - 1
                            if (idx in 0..1) {
                                actualizarVista(idx, modo == "manual", estado == "encendida")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al leer estado de bombas", Toast.LENGTH_SHORT).show()
                }
            },
            { Toast.makeText(this, "No se pudo conectar al servidor", Toast.LENGTH_SHORT).show() }
        )
        queue.add(request)
    }

    private fun enviarComando(idBomba: Int, manual: Boolean, encendida: Boolean, indice: Int) {
        // Desactivamos los 2 switches de esta bomba mientras esperamos respuesta
        swModo[indice].isEnabled = false
        swEstado[indice].isEnabled = false

        // Armamos el JSON que espera controlar_bomba.php:
        // { "id_bomba": 1, "modo": "manual", "estado": "encendida" }
        val body = JSONObject()
        body.put("id_bomba", idBomba)
        body.put("modo", if (manual) "manual" else "automatico")
        body.put("estado", if (encendida) "encendida" else "apagada")

        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_CONTROLAR_BOMBA, body,
            { response ->
                if (response.optBoolean("success")) {
                    Toast.makeText(this, "${nombreBomba[indice]} actualizada", Toast.LENGTH_SHORT).show()
                    // Recargamos las 2 bombas: si esta se encendió en manual,
                    // el servidor puede haber apagado la otra automáticamente
                    // (regla de seguridad: nunca las 2 juntas)
                    cargarEstadoBombas()
                } else {
                    Toast.makeText(this, "No se pudo actualizar la bomba", Toast.LENGTH_SHORT).show()
                    swModo[indice].isEnabled = true
                    swEstado[indice].isEnabled = swModo[indice].isChecked
                }
            },
            {
                Toast.makeText(this, "Error de conexión con el servidor", Toast.LENGTH_SHORT).show()
                swModo[indice].isEnabled = true
                swEstado[indice].isEnabled = swModo[indice].isChecked
            }
        )
        queue.add(request)
    }

    private fun actualizarVista(indice: Int, esManual: Boolean, encendida: Boolean) {
        modoManualActual[indice] = esManual
        estadoManualActual[indice] = encendida

        // Movemos los switches por código sin disparar los listeners
        actualizandoDesdeServidor = true
        swModo[indice].isChecked = esManual
        swEstado[indice].isChecked = encendida
        actualizandoDesdeServidor = false

        // El switch de encendido solo se puede tocar si el modo es manual
        swEstado[indice].isEnabled = esManual
        swModo[indice].isEnabled = true

        if (esManual) {
            tvEstado[indice].text = if (encendida) "encendida (manual)" else "apagada (manual)"
            tvEstado[indice].setTextColor(if (encendida) 0xFF2E7D32.toInt() else 0xFF888888.toInt())
        } else {
            tvEstado[indice].text = if (indice == 0) {
                "mezcla automática tras dosificar"
            } else {
                "riego según horario"
            }
            tvEstado[indice].setTextColor(0xFF888888.toInt())
        }
    }
}