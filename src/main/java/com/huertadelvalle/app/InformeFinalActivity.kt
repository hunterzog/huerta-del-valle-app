package com.huertadelvalle.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InformeFinalActivity : AppCompatActivity() {

    companion object {
        // Ciclo horario fijo programado en el ESP32 (se repite las 24 horas).
        private const val TEXTO_CICLO_RIEGO =
            "Ciclo de riego: cada hora, 5 min de aireación, 30 min de riego " +
                    "y 25 min de control de calidad y dosificación (las 24 horas)"
    }

    // Vistas del informe
    private lateinit var tvProyecto: TextView
    private lateinit var tvFecha: TextView
    private lateinit var tvDatosProyecto: TextView
    private lateinit var tvConfiguracion: TextView
    private lateinit var tvSensores: TextView
    private lateinit var tvEvaluacion: TextView
    private lateinit var tvObservaciones: TextView
    private lateinit var ivFoto: ImageView
    private lateinit var btnGenerarPdf: Button

    // Datos preparados
    private var datosInforme: JSONObject? = null
    private var fotoInforme: Bitmap? = null
    private var nombreProyecto = "Proyecto"
    private var textoProyecto = ""
    private var textoConfiguracion = ""
    private var textoSensores = ""
    private var textoEvaluacion = ""
    private var textoObservaciones = ""

    // Elementos del PDF
    private lateinit var documentoPdf: PdfDocument
    private lateinit var paginaPdf: PdfDocument.Page
    private lateinit var canvasPdf: Canvas
    private val pinturaPdf = Paint(Paint.ANTI_ALIAS_FLAG)
    private var numeroPagina = 0
    private var posicionY = 0f

    // El usuario elige dónde guardar
    private val crearDocumento =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
            if (uri != null) generarPdf(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_informe_final)

        // Vincular vistas
        tvProyecto = findViewById(R.id.tvProyectoInforme)
        tvFecha = findViewById(R.id.tvFechaInforme)
        tvDatosProyecto = findViewById(R.id.tvDatosProyectoInforme)
        tvConfiguracion = findViewById(R.id.tvConfiguracionInforme)
        tvSensores = findViewById(R.id.tvSensoresInforme)
        tvEvaluacion = findViewById(R.id.tvEvaluacionInforme)
        tvObservaciones = findViewById(R.id.tvObservacionesInforme)
        ivFoto = findViewById(R.id.ivFotoInforme)
        btnGenerarPdf = findViewById(R.id.btnGenerarPdf)

        // Datos recibidos
        val idProyecto = intent.getIntExtra("id_proyecto", 0)
        nombreProyecto = intent.getStringExtra("nombre_proyecto") ?: "Proyecto"
        tvProyecto.text = nombreProyecto
        tvFecha.text = "Generado: ${fechaActual()}"
        btnGenerarPdf.isEnabled = false

        // Ayuda del informe
        findViewById<TextView>(R.id.tvTooltipInforme).setOnClickListener {
            Notificacion.informacion(
                this,
                "Este informe reúne los resultados del proyecto finalizado"
            )
        }

        btnGenerarPdf.setOnClickListener {
            val nombre = nombreProyecto.replace(Regex("[^A-Za-z0-9_-]"), "_")
            crearDocumento.launch("Informe_final_$nombre.pdf")
        }
        findViewById<Button>(R.id.btnVolverInforme).setOnClickListener { finish() }

        if (idProyecto > 0) cargarInforme(idProyecto)
        else Notificacion.error(this, "Proyecto no válido")
    }

    private fun cargarInforme(idProyecto: Int) {
        // Solicita los datos
        val datos = JSONObject().apply { put("id_proyecto", idProyecto) }
        val peticion = JsonObjectRequest(
            Request.Method.POST,
            Configuracion.URL_INFORME_FINAL,
            datos,
            { respuesta ->
                if (respuesta.optBoolean("success")) {
                    datosInforme = respuesta
                    mostrarInforme(respuesta)
                    cargarFotoReciente(respuesta.optJSONObject("ultima_foto"))
                    btnGenerarPdf.isEnabled = true
                } else {
                    Notificacion.error(this, respuesta.optString("mensaje"))
                }
            },
            { Notificacion.error(this, "Error de conexión") }
        )
        Volley.newRequestQueue(this).add(peticion)
    }

    private fun mostrarInforme(respuesta: JSONObject) {
        val proyecto = respuesta.getJSONObject("proyecto")
        val configuracion = respuesta.optJSONObject("configuracion") ?: JSONObject()
        val sensores = respuesta.getJSONObject("sensores")
        val evaluacion = respuesta.getJSONObject("evaluacion")
        val componentes = respuesta.optJSONArray("componentes") ?: JSONArray()
        val observaciones = respuesta.optJSONArray("observaciones") ?: JSONArray()

        // Texto del proyecto
        textoProyecto = "DATOS DEL PROYECTO\n\n" +
                "Nombre: ${valorTexto(proyecto, "nombre")}\n" +
                "Descripción: ${valorTexto(proyecto, "descripcion", "Sin descripción")}\n" +
                "Responsable: ${valorTexto(proyecto, "responsable")}\n" +
                "Fecha de inicio: ${formatearFecha(valorTexto(proyecto, "fecha_inicio"))}\n" +
                "Fecha de finalización: ${formatearFecha(valorTexto(proyecto, "fecha_fin"))}\n" +
                "Duración: ${proyecto.optInt("dias")} días\n" +
                "Estado: ${valorTexto(proyecto, "estado")}"

        // Texto de configuración
        textoConfiguracion = if (configuracion.length() > 0) {
            "CONFIGURACIÓN DEL CULTIVO\n\n" +
                    "Semilla: ${valorTexto(configuracion, "nombre_semilla")}\n" +
                    "Nutriente: ${valorTexto(configuracion, "nombre_comercial", "Sin dato")}\n" +
                    textoComponentes(componentes) +
                    "pH configurado: ${valorTexto(configuracion, "ph_minimo")} - ${valorTexto(configuracion, "ph_maximo")}\n" +
                    "TDS configurado: ${valorTexto(configuracion, "tds_minimo")} - ${valorTexto(configuracion, "tds_maximo")} ppm\n" +
                    TEXTO_CICLO_RIEGO
        } else "CONFIGURACIÓN DEL CULTIVO\n\nSin configuración disponible"

        // Texto de sensores
        textoSensores = "ESTADÍSTICAS DE SENSORES\n\n" +
                "Mediciones registradas: ${sensores.optInt("total_mediciones")}\n" +
                lineaSensor(sensores, "Temperatura ambiente", "temperatura", "°C") +
                lineaSensor(sensores, "Humedad", "humedad", "%") +
                lineaSensor(sensores, "Temperatura del agua", "temp_agua", "°C") +
                lineaSensor(sensores, "pH", "ph", "") +
                lineaSensor(sensores, "TDS", "tds", "ppm") +
                lineaSensor(sensores, "Nivel de agua", "nivel", "cm")

        // Texto de evaluación
        val diasEvaluados = evaluacion.optInt("dias_evaluados")
        textoEvaluacion = if (diasEvaluados > 0) {
            val promedio = evaluacion.optDouble("promedio_general")
            "EVALUACIONES LIKERT\n\n" +
                    "Días evaluados: $diasEvaluados\n" +
                    "Promedio general: ${numero(promedio)} - ${interpretar(promedio)}\n" +
                    "Crecimiento: ${valorNumero(evaluacion, "crecimiento")}\n" +
                    "Color de hojas: ${valorNumero(evaluacion, "color_hojas")}\n" +
                    "Firmeza: ${valorNumero(evaluacion, "firmeza")}\n" +
                    "Estado de raíces: ${valorNumero(evaluacion, "estado_raices")}\n" +
                    "Sin plagas: ${valorNumero(evaluacion, "sin_plagas")}"
        } else "EVALUACIONES LIKERT\n\nSin evaluaciones registradas"

        // Texto de observaciones (el informe final incluye todas)
        textoObservaciones = "OBSERVACIONES DEL PROYECTO\n\n" + textoObservaciones(observaciones)

        tvDatosProyecto.text = textoProyecto
        tvConfiguracion.text = textoConfiguracion
        tvSensores.text = textoSensores
        tvEvaluacion.text = textoEvaluacion
        tvObservaciones.text = textoObservaciones
    }

    private fun textoComponentes(componentes: JSONArray): String {
        if (componentes.length() == 0) return "Componentes: Sin datos\n"
        val texto = StringBuilder("Componentes:\n")
        for (i in 0 until componentes.length()) {
            val item = componentes.getJSONObject(i)
            val cantidad = item.optInt("cantidad_dosis", 1)
            val duracion = item.optInt("duracion_dosis_segundos", 5)
            val total = cantidad * duracion
            texto.append("- ${valorTexto(item, "nombre_componente")}: ")
            texto.append("$cantidad dosis de $duracion segundos ($total segundos en total)\n")
        }
        return texto.toString()
    }

    private fun lineaSensor(datos: JSONObject, titulo: String, clave: String, unidad: String): String {
        val promedio = valorNumero(datos, "${clave}_promedio")
        val minimo = valorNumero(datos, "${clave}_minima", valorNumero(datos, "${clave}_minimo"))
        val maximo = valorNumero(datos, "${clave}_maxima", valorNumero(datos, "${clave}_maximo"))
        val sufijo = if (unidad.isBlank()) "" else " $unidad"
        return "$titulo: prom. $promedio$sufijo | mín. $minimo$sufijo | máx. $maximo$sufijo\n"
    }

    private fun textoObservaciones(datos: JSONArray): String {
        if (datos.length() == 0) return "Sin observaciones registradas"
        val texto = StringBuilder()
        for (i in 0 until datos.length()) {
            val item = datos.getJSONObject(i)
            texto.append("${formatearFecha(item.optString("fecha"))}: ")
            texto.append(item.optString("observacion")).append("\n")
        }
        return texto.toString().trim()
    }

    private fun cargarFotoReciente(datos: JSONObject?) {
        val ruta = datos?.optString("foto").orEmpty()
        if (ruta.isBlank()) return

        // Descarga la última foto
        val peticion = ImageRequest(
            Configuracion.urlArchivo(ruta),
            { bitmap ->
                fotoInforme = bitmap
                ivFoto.setImageBitmap(bitmap)
                ivFoto.visibility = View.VISIBLE
            },
            1280,
            1280,
            ImageView.ScaleType.CENTER_CROP,
            Bitmap.Config.RGB_565,
            { Notificacion.informacion(this, "Fotografía no disponible") }
        )
        Volley.newRequestQueue(this).add(peticion)
    }

    private fun generarPdf(uri: Uri) {
        try {
            // Inicia el documento
            documentoPdf = PdfDocument()
            numeroPagina = 0
            nuevaPaginaPdf()

            escribirPdf("HUERTA DEL VALLE", 19f, true, Color.rgb(198, 40, 40), 4f)
            escribirPdf("INFORME FINAL", 17f, true, Color.DKGRAY, 10f)
            escribirPdf("Proyecto: $nombreProyecto", 13f, true, Color.DKGRAY, 2f)
            escribirPdf("Fecha: ${fechaActual()}", 10f, false, Color.GRAY, 14f)

            escribirBloquePdf(textoProyecto)
            escribirBloquePdf(textoConfiguracion)
            escribirBloquePdf(textoSensores)
            escribirBloquePdf(textoEvaluacion)
            escribirBloquePdf(textoObservaciones)
            agregarFotoPdf()
            escribirPdf(
                "Documento final generado con los registros almacenados durante el proyecto.",
                9f,
                false,
                Color.GRAY,
                0f
            )

            documentoPdf.finishPage(paginaPdf)
            contentResolver.openOutputStream(uri)?.use { salida ->
                documentoPdf.writeTo(salida)
            } ?: throw IllegalStateException("No se pudo abrir el archivo")
            documentoPdf.close()
            Notificacion.exito(this, "PDF guardado correctamente")
        } catch (_: Exception) {
            if (::documentoPdf.isInitialized) documentoPdf.close()
            Notificacion.error(this, "No se pudo generar el PDF")
        }
    }

    private fun nuevaPaginaPdf() {
        // Crea una página A4
        if (::paginaPdf.isInitialized && numeroPagina > 0) documentoPdf.finishPage(paginaPdf)
        numeroPagina++
        val info = PdfDocument.PageInfo.Builder(595, 842, numeroPagina).create()
        paginaPdf = documentoPdf.startPage(info)
        canvasPdf = paginaPdf.canvas
        canvasPdf.drawColor(Color.WHITE)
        posicionY = 45f
    }

    private fun escribirBloquePdf(texto: String) {
        val lineas = texto.lines()
        lineas.forEachIndexed { indice, linea ->
            val esTitulo = indice == 0
            escribirPdf(
                linea,
                if (esTitulo) 12f else 10f,
                esTitulo,
                if (esTitulo) Color.rgb(198, 40, 40) else Color.DKGRAY,
                if (esTitulo || linea.isBlank()) 5f else 1f
            )
        }
        posicionY += 8f
    }

    private fun escribirPdf(
        texto: String,
        tamano: Float,
        negrita: Boolean,
        color: Int,
        margen: Float
    ) {
        pinturaPdf.textSize = tamano
        pinturaPdf.color = color
        pinturaPdf.typeface = Typeface.create(
            Typeface.DEFAULT,
            if (negrita) Typeface.BOLD else Typeface.NORMAL
        )

        // Ajusta el texto al ancho
        val palabras = texto.ifBlank { " " }.split(" ")
        var linea = ""
        for (palabra in palabras) {
            val prueba = if (linea.isEmpty()) palabra else "$linea $palabra"
            if (pinturaPdf.measureText(prueba) > 515f && linea.isNotEmpty()) {
                dibujarLineaPdf(linea, tamano)
                linea = palabra
            } else linea = prueba
        }
        if (linea.isNotEmpty()) dibujarLineaPdf(linea, tamano)
        posicionY += margen
    }

    private fun dibujarLineaPdf(texto: String, tamano: Float) {
        if (posicionY > 805f) nuevaPaginaPdf()
        canvasPdf.drawText(texto, 40f, posicionY, pinturaPdf)
        posicionY += tamano + 5f
    }

    private fun agregarFotoPdf() {
        val foto = fotoInforme ?: return
        if (posicionY > 600f) nuevaPaginaPdf()
        escribirPdf("FOTOGRAFÍA RECIENTE", 12f, true, Color.rgb(198, 40, 40), 8f)
        val ancho = 360
        val alto = (foto.height * (ancho.toFloat() / foto.width)).toInt().coerceAtMost(240)
        val escalada = Bitmap.createScaledBitmap(foto, ancho, alto, true)
        canvasPdf.drawBitmap(escalada, 40f, posicionY, pinturaPdf)
        posicionY += alto + 14f
    }

    private fun valorTexto(datos: JSONObject, clave: String, defecto: String = "Sin dato"): String {
        if (!datos.has(clave) || datos.isNull(clave)) return defecto
        return datos.optString(clave).ifBlank { defecto }
    }

    private fun valorNumero(datos: JSONObject, clave: String, defecto: String = "Sin dato"): String {
        if (!datos.has(clave) || datos.isNull(clave)) return defecto
        return numero(datos.optDouble(clave))
    }

    private fun numero(valor: Double): String =
        String.format(Locale.getDefault(), "%.2f", valor)

    private fun interpretar(promedio: Double): String = when {
        promedio <= 1.80 -> "Estado crítico"
        promedio <= 2.60 -> "Estado deficiente"
        promedio <= 3.40 -> "Estado regular"
        promedio <= 4.20 -> "Buen estado"
        else -> "Estado excelente"
    }

    private fun fechaActual(): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

    private fun formatearFecha(fecha: String): String {
        return try {
            val entrada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val salida = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            salida.format(entrada.parse(fecha)!!)
        } catch (_: Exception) {
            fecha
        }
    }
}