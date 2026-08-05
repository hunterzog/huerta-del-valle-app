package com.huertadelvalle.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import android.widget.Switch

class ControlValvulasActivity : AppCompatActivity() {
    // Arreglo con las 5 vistas de "estado"
    private lateinit var tvEstado: Array<TextView>

    // Arreglo con los 5 switches
    private lateinit var swValvula: Array<Switch>

    // Para distinguir "el usuario tocó el switch"
    private var actualizandoDesdeServidor = false

    private lateinit var btnVolver: Button

    private val nombreValvula = arrayOf(
        "Carga de agua", "Nutriente A", "Nutriente B", "Nutriente C", "Ácido pH"
    )

    // Guarda en memoria si cada válvula está abierta (true) o cerrada (false).
    // Índice 0 = válvula 1, índice 1 = válvula 2
    private val estadoActual = BooleanArray(5)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control_valvulas)

        // Vinculamos cada TextView de estado con su posición en el arreglo
        tvEstado = arrayOf(
            findViewById(R.id.tvEstadoV1),
            findViewById(R.id.tvEstadoV2),
            findViewById(R.id.tvEstadoV3),
            findViewById(R.id.tvEstadoV4),
            findViewById(R.id.tvEstadoV5)
        )

        // 5 switches con su posición en el arreglo (0 a 4)
        swValvula = arrayOf(
            findViewById(R.id.swV1),
            findViewById(R.id.swV2),
            findViewById(R.id.swV3),
            findViewById(R.id.swV4),
            findViewById(R.id.swV5)
        )

        btnVolver = findViewById(R.id.btnVolverValvulas)

        for (i in 0..4) {
            swValvula[i].setOnCheckedChangeListener { _, isChecked ->
                if (actualizandoDesdeServidor) return@setOnCheckedChangeListener

                val nuevaAccion = if (isChecked) "abrir" else "cerrar"
                controlarValvula(i + 1, nuevaAccion, i)
            }
        }

        btnVolver.setOnClickListener { finish() }

        //consultamos el estado real de las válvulas
        cargarEstadoValvulas()
    }

    // actualiza pantalla por si hay cmabios
    override fun onResume() {
        super.onResume()
        cargarEstadoValvulas()
    }

    private fun cargarEstadoValvulas() {
        val queue = Volley.newRequestQueue(this)

        // Usamos StringRequest (no JsonObjectRequest) porque acá solo
        // hacemos un GET simple, sin body que enviar
        val request = StringRequest(
            Request.Method.GET, Configuracion.URL_ESTADO_VALVULAS,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        // El PHP devuelve un array "valvulas" con 5 objetos,
                        // cada uno con id_valvula, nombre, gpio y estado
                        val valvulas = json.getJSONArray("valvulas")

                        for (i in 0 until valvulas.length()) {
                            val v = valvulas.getJSONObject(i)
                            val idValvula = v.getInt("id_valvula")   // viene 1 a 5
                            val estado = v.getString("estado")       // "abierta" o "cerrada"

                            val idx = idValvula - 1  // lo convertimos a índice 0-4
                            if (idx in 0..4) {
                                actualizarVista(idx, estado == "abierta")
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Si el JSON viene mal formado o algo falla al leerlo,
                    // avisamos sin que la app se cierre de golpe
                    Toast.makeText(this, "Error al leer estado de válvulas", Toast.LENGTH_SHORT).show()
                }
            },
            {
                // (error listener) se dispara si no hay
                // conexión al servidor
                Toast.makeText(this, "No se pudo conectar al servidor", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    private fun controlarValvula(idValvula: Int, accion: String, indice: Int) {
        // Desactivamos el botón mientras esperamos respuesta, para evitar
        // que el usuario lo toque varias veces seguidas por error
        swValvula[indice].isEnabled = false

        // Armamos el JSON que espera controlar_valvula.php:
        // { "id_valvula": 2, "accion": "abrir" }
        val body = JSONObject()
        body.put("id_valvula", idValvula)
        body.put("accion", accion)

        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_CONTROLAR_VALVULA, body,
            { response ->
                swValvula[indice].isEnabled = true

                if (response.optBoolean("success")) {
                    // Actualizamos la pantalla al instante (optimista),
                    // aunque el ESP32 recién va a aplicarlo en su próximo ciclo
                    actualizarVista(indice, accion == "abrir")
                    // Si se abrió una válvula, el servidor cerró las demás:
                    // refrescamos toda la pantalla para reflejar ese cambio
                    if (accion == "abrir") {
                        cargarEstadoValvulas()
                    }

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

    private fun actualizarVista(indice: Int, abierta: Boolean) {
        estadoActual[indice] = abierta
        // Activamos la bandera antes de mover el switch por código,
        // para que el setOnCheckedChangeListener sepa que debe ignorar este cambio
        actualizandoDesdeServidor = true
        swValvula[indice].isChecked = abierta
        actualizandoDesdeServidor = false

        if (abierta) {
            tvEstado[indice].text = "abierta"
            tvEstado[indice].setTextColor(0xFF2E7D32.toInt())  // verde
        } else {
            tvEstado[indice].text = "cerrada"
            tvEstado[indice].setTextColor(0xFF888888.toInt())  // gris
        }
    }
}
