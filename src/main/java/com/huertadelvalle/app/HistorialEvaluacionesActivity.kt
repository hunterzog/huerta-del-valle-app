package com.huertadelvalle.app

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class HistorialEvaluacionesActivity : AppCompatActivity() {

    // Vistas principales
    private lateinit var tvProyecto: TextView
    private lateinit var tvResumen: TextView
    private lateinit var tvPromedios: TextView
    private lateinit var contenedor: LinearLayout
    private var idProyecto = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_evaluaciones)

        // Vincular vistas
        tvProyecto = findViewById(R.id.tvProyectoHistorial)
        tvResumen = findViewById(R.id.tvResumenHistorial)
        tvPromedios = findViewById(R.id.tvPromediosCriterios)
        contenedor = findViewById(R.id.contenedorEvaluaciones)

        // Datos recibidos
        idProyecto = intent.getIntExtra("id_proyecto", 0)
        tvProyecto.text = intent.getStringExtra("nombre_proyecto") ?: "Proyecto"

        findViewById<Button>(R.id.btnVolverHistorial).setOnClickListener { finish() }

        if (idProyecto > 0) cargarHistorial()
        else Notificacion.error(this, "Proyecto no válido")
    }

    private fun cargarHistorial() {
        // Envía el proyecto
        val datos = JSONObject().apply { put("id_proyecto", idProyecto) }
        val peticion = JsonObjectRequest(
            Request.Method.POST,
            Configuracion.URL_HISTORIAL_EVALUACIONES,
            datos,
            { respuesta ->
                if (respuesta.optBoolean("success")) {
                    mostrarResumen(respuesta.getJSONObject("resumen"))
                    mostrarEvaluaciones(respuesta.getJSONArray("evaluaciones"))
                } else {
                    Notificacion.error(this, respuesta.optString("mensaje"))
                }
            },
            { Notificacion.error(this, "Error de conexión") }
        )
        Volley.newRequestQueue(this).add(peticion)
    }

    private fun mostrarResumen(resumen: JSONObject) {
        // Datos generales
        val diasEvaluados = resumen.optInt("dias_evaluados")
        val diasProyecto = resumen.optInt("dias_proyecto")
        val diasSinEvaluacion = resumen.optInt("dias_sin_evaluacion")
        val promedio = resumen.optDouble("promedio_general", 0.0)

        tvResumen.text = if (diasEvaluados > 0) {
            "Promedio general: ${formato(promedio)} - ${interpretar(promedio)}\n" +
                    "Días del proyecto: $diasProyecto\n" +
                    "Días evaluados: $diasEvaluados\n" +
                    "Días sin evaluación: $diasSinEvaluacion"
        } else {
            "Todavía no existen evaluaciones registradas"
        }

        // Promedios por criterio
        tvPromedios.text = if (diasEvaluados > 0) {
            "Promedio por criterio\n\n" +
                    "Crecimiento: ${formato(resumen.optDouble("promedio_crecimiento"))}\n" +
                    "Color de hojas: ${formato(resumen.optDouble("promedio_color_hojas"))}\n" +
                    "Firmeza: ${formato(resumen.optDouble("promedio_firmeza"))}\n" +
                    "Estado de raíces: ${formato(resumen.optDouble("promedio_raices"))}\n" +
                    "Sin plagas: ${formato(resumen.optDouble("promedio_sin_plagas"))}"
        } else "Sin promedios disponibles"
    }

    private fun mostrarEvaluaciones(evaluaciones: JSONArray) {
        // Limpia la lista
        contenedor.removeAllViews()

        if (evaluaciones.length() == 0) {
            val mensaje = TextView(this).apply {
                text = "Sin evaluaciones"
                textSize = 16f
                setPadding(0, dp(12), 0, dp(12))
            }
            contenedor.addView(mensaje)
            return
        }

        // Crea un botón por día
        for (i in 0 until evaluaciones.length()) {
            val evaluacion = evaluaciones.getJSONObject(i)
            val promedio = evaluacion.optDouble("promedio")
            val boton = Button(this).apply {
                text = "${formatearFecha(evaluacion.optString("fecha"))}\n" +
                        "Promedio: ${formato(promedio)} - ${interpretar(promedio)}"
                isAllCaps = false
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setTextColor(Color.WHITE)
                backgroundTintList = ColorStateList.valueOf(colorPromedio(promedio))
                setPadding(dp(14), dp(10), dp(14), dp(10))
                setOnClickListener { mostrarDetalle(evaluacion) }
            }
            val parametros = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
            contenedor.addView(boton, parametros)
        }
    }

    private fun mostrarDetalle(evaluacion: JSONObject) {
        // Contenido del detalle
        val contenido = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }

        val datos = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.DKGRAY)
            text = "Fecha: ${formatearFecha(evaluacion.optString("fecha"))}\n\n" +
                    "Crecimiento: ${evaluacion.optInt("crecimiento")}/5\n" +
                    "Color de hojas: ${evaluacion.optInt("color_hojas")}/5\n" +
                    "Firmeza: ${evaluacion.optInt("firmeza")}/5\n" +
                    "Estado de raíces: ${evaluacion.optInt("estado_raices")}/5\n" +
                    "Sin plagas: ${evaluacion.optInt("sin_plagas")}/5\n\n" +
                    "Promedio: ${formato(evaluacion.optDouble("promedio"))}\n" +
                    "Resultado: ${interpretar(evaluacion.optDouble("promedio"))}\n\n" +
                    "Observación:\n${evaluacion.optString("observacion").ifBlank { "Sin observación" }}"
        }
        contenido.addView(datos)

        // Fotografía guardada
        val rutaFoto = evaluacion.optString("foto")
        if (rutaFoto.isNotBlank()) {
            val imagen = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                adjustViewBounds = true
                minimumHeight = dp(220)
                setPadding(0, dp(16), 0, 0)
            }
            contenido.addView(imagen)
            cargarFoto(rutaFoto, imagen)
        } else {
            val sinFoto = TextView(this).apply {
                text = "\nSin fotografía"
                textSize = 15f
                setTextColor(Color.GRAY)
            }
            contenido.addView(sinFoto)
        }

        val desplazamiento = ScrollView(this).apply { addView(contenido) }
        AlertDialog.Builder(this)
            .setTitle("Detalle de evaluación")
            .setView(desplazamiento)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun cargarFoto(ruta: String, imagen: ImageView) {
        // Descarga la foto
        val peticion = ImageRequest(
            Configuracion.urlArchivo(ruta),
            { bitmap -> imagen.setImageBitmap(bitmap) },
            1280,
            1280,
            ImageView.ScaleType.CENTER_CROP,
            Bitmap.Config.RGB_565,
            {
                imagen.visibility = View.GONE
                Notificacion.informacion(this, "Fotografía no disponible")
            }
        )
        Volley.newRequestQueue(this).add(peticion)
    }

    private fun formato(valor: Double): String =
        String.format(Locale.getDefault(), "%.2f", valor)

    private fun interpretar(promedio: Double): String = when {
        promedio <= 1.80 -> "Estado crítico"
        promedio <= 2.60 -> "Estado deficiente"
        promedio <= 3.40 -> "Estado regular"
        promedio <= 4.20 -> "Buen estado"
        else -> "Estado excelente"
    }

    private fun colorPromedio(promedio: Double): Int = when {
        promedio <= 2.60 -> Color.parseColor("#B71C1C")
        promedio <= 3.40 -> Color.parseColor("#E65100")
        promedio <= 4.20 -> Color.parseColor("#558B2F")
        else -> Color.parseColor("#1B5E20")
    }

    private fun formatearFecha(fecha: String): String {
        return try {
            val entrada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val salida = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            salida.format(entrada.parse(fecha)!!)
        } catch (_: Exception) {
            fecha
        }
    }

    private fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density).toInt()
}
