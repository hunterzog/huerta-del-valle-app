package com.huertadelvalle.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

class MonitoreoActivity : AppCompatActivity() {

    // Vistas
    private lateinit var tvTemperatura: TextView
    private lateinit var tvHumedad: TextView
    private lateinit var tvUltimaActualizacion: TextView
    private lateinit var btnVolverMonitoreo: Button
    private lateinit var tvTempAgua: TextView
    private lateinit var tvTds: TextView
    private lateinit var tvPh: TextView

    // Handler para actualizar cada 10 segundos
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var actualizarRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitoreo)

        // Vincular vistas
        tvTemperatura         = findViewById(R.id.tvTemperatura)
        tvHumedad             = findViewById(R.id.tvHumedad)
        tvUltimaActualizacion = findViewById(R.id.tvUltimaActualizacion)
        btnVolverMonitoreo    = findViewById(R.id.btnVolverMonitoreo)
        tvTempAgua            = findViewById(R.id.tvTempAgua)
        tvTds                 = findViewById(R.id.tvTds)
        tvPh                  = findViewById(R.id.tvPh)

        // Obtener id_proyecto
        val idProyecto = intent.getIntExtra("id_proyecto", 0)

        // Actualizar cada 10 segundos
        actualizarRunnable = object : Runnable {
            override fun run() {
                obtenerUltimaMedicion(idProyecto)
                handler.postDelayed(this, 10000)
            }
        }
        handler.post(actualizarRunnable)

        // Botón volver
        btnVolverMonitoreo.setOnClickListener {
            finish()
        }
    }

    private fun obtenerUltimaMedicion(idProyecto: Int) {
        val queue = Volley.newRequestQueue(this)

        val datos = JSONObject()
        datos.put("id_proyecto", idProyecto)

        val peticion = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_ULTIMA_MEDICION, datos,
            { respuesta ->
                val success = respuesta.getBoolean("success")
                if (success) {
                    val temperatura = respuesta.getString("temperatura")
                    val humedad     = respuesta.getString("humedad")
                    val tempAgua    = respuesta.getString("temp_agua")
                    val tds         = respuesta.getString("tds")
                    val ph          = respuesta.getString("ph")

                    tvTemperatura.text = "$temperatura °C"
                    tvHumedad.text     = "$humedad %"
                    tvTempAgua.text    = "$tempAgua °C"
                    tvTds.text         = "$tds ppm"
                    tvPh.text          = "$ph"

                    // Mostrar hora de actualización
                    val formato = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    tvUltimaActualizacion.text = "Última actualización: ${formato.format(Date())}"
                } else {
                    Notificacion.advertencia(this, "Sin datos disponibles")
                }
            },
            { _ ->
                Notificacion.error(this, "Error de conexión")
            }
        )

        queue.add(peticion)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Detener actualizaciones al salir
        handler.removeCallbacks(actualizarRunnable)
    }
}