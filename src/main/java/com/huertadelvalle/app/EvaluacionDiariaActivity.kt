package com.huertadelvalle.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EvaluacionDiariaActivity : AppCompatActivity() {

    // Datos de pantalla
    private lateinit var tvProyecto: TextView
    private lateinit var tvFecha: TextView
    private lateinit var tvPromedio: TextView
    private lateinit var rgCrecimiento: RadioGroup
    private lateinit var rgColorHojas: RadioGroup
    private lateinit var rgFirmeza: RadioGroup
    private lateinit var rgRaices: RadioGroup
    private lateinit var rgSinPlagas: RadioGroup
    private lateinit var etObservacion: EditText
    private lateinit var ivFotoCultivo: ImageView
    private lateinit var btnTomarFoto: Button
    private lateinit var btnGuardar: Button

    // Datos de la evaluación
    private var idProyecto = 0
    private var fotoBase64Nueva: String? = null

    // Captura la foto
    private val tomarFotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap == null) {
                Notificacion.advertencia(this, "No se capturó la fotografía")
                return@registerForActivityResult
            }

            // Prepara la imagen
            val imagen = reducirImagen(bitmap)
            ivFotoCultivo.setImageBitmap(imagen)
            fotoBase64Nueva = convertirBitmapBase64(imagen)
            btnTomarFoto.text = "Volver a tomar foto"
        }

    // Solicita el permiso
    private val solicitarPermisoCamara =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
            if (concedido) tomarFotoLauncher.launch(null)
            else Notificacion.advertencia(this, "Debes permitir el acceso a la cámara")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_evaluacion_diaria)

        // Vincular vistas
        tvProyecto = findViewById(R.id.tvProyectoEvaluacion)
        tvFecha = findViewById(R.id.tvFechaEvaluacion)
        tvPromedio = findViewById(R.id.tvPromedioEvaluacion)
        rgCrecimiento = findViewById(R.id.rgCrecimiento)
        rgColorHojas = findViewById(R.id.rgColorHojas)
        rgFirmeza = findViewById(R.id.rgFirmeza)
        rgRaices = findViewById(R.id.rgRaices)
        rgSinPlagas = findViewById(R.id.rgSinPlagas)
        etObservacion = findViewById(R.id.etObservacion)
        ivFotoCultivo = findViewById(R.id.ivFotoCultivo)
        btnTomarFoto = findViewById(R.id.btnTomarFotoCultivo)
        btnGuardar = findViewById(R.id.btnGuardarEvaluacion)

        // Ayudas de la pantalla
        findViewById<TextView>(R.id.tvTooltipLikert).setOnClickListener {
            Notificacion.informacion(
                this,
                "1: Muy en desacuerdo, 2: En desacuerdo, 3: Neutral, 4: De acuerdo y 5: Muy de acuerdo"
            )
        }
        findViewById<TextView>(R.id.tvTooltipObservacion).setOnClickListener {
            Notificacion.informacion(
                this,
                "Puedes registrar cambios, problemas o detalles observados en el cultivo"
            )
        }
        findViewById<TextView>(R.id.tvTooltipFotoEvaluacion).setOnClickListener {
            Notificacion.informacion(
                this,
                "La fotografía es opcional y se guarda con calidad reducida"
            )
        }

        idProyecto = intent.getIntExtra("id_proyecto", 0)
        tvProyecto.text = intent.getStringExtra("nombre_proyecto") ?: "Proyecto"

        // Fecha de hoy
        val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        tvFecha.text = "Fecha: $fecha"

        // Escucha las respuestas
        listaGrupos().forEach { grupo ->
            grupo.setOnCheckedChangeListener { _, _ -> mostrarPromedio() }
        }
        btnTomarFoto.setOnClickListener { abrirCamara() }
        btnGuardar.setOnClickListener { guardarEvaluacion() }
        findViewById<Button>(R.id.btnVolverEvaluacion).setOnClickListener { finish() }

        // Busca la evaluación de hoy
        if (idProyecto > 0) obtenerEvaluacion()
        else {
            Notificacion.error(this, "Proyecto no válido")
            btnGuardar.isEnabled = false
        }
    }

    private fun listaGrupos() = listOf(
        rgCrecimiento, rgColorHojas, rgFirmeza, rgRaices, rgSinPlagas
    )

    private fun valorSeleccionado(grupo: RadioGroup): Int {
        // Lee el valor marcado
        val boton = grupo.findViewById<RadioButton>(grupo.checkedRadioButtonId)
        return boton?.tag?.toString()?.toIntOrNull() ?: 0
    }

    private fun mostrarPromedio() {
        // Calcula el promedio
        val valores = listaGrupos().map { valorSeleccionado(it) }
        if (valores.all { it > 0 }) {
            val promedio = valores.average()
            tvPromedio.text = "Promedio: %.2f - %s".format(
                Locale.getDefault(), promedio, interpretarPromedio(promedio)
            )
        } else tvPromedio.text = "Completa los cinco criterios"
    }

    private fun interpretarPromedio(promedio: Double): String = when {
        // Clasifica el resultado
        promedio <= 1.80 -> "Estado crítico"
        promedio <= 2.60 -> "Estado deficiente"
        promedio <= 3.40 -> "Estado regular"
        promedio <= 4.20 -> "Buen estado"
        else -> "Estado excelente"
    }

    private fun abrirCamara() {
        // Verifica el permiso
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) tomarFotoLauncher.launch(null)
        else solicitarPermisoCamara.launch(Manifest.permission.CAMERA)
    }

    private fun reducirImagen(bitmap: Bitmap): Bitmap {
        // Limita el tamaño
        val limite = 1280
        if (bitmap.width <= limite && bitmap.height <= limite) return bitmap
        val escala = minOf(limite.toFloat() / bitmap.width, limite.toFloat() / bitmap.height)
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * escala).toInt(),
            (bitmap.height * escala).toInt(),
            true
        )
    }

    private fun convertirBitmapBase64(bitmap: Bitmap): String {
        // Calidad al 65 %
        val salida = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 65, salida)
        return Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP)
    }

    private fun obtenerEvaluacion() {
        // Envía el proyecto
        val datos = JSONObject().apply { put("id_proyecto", idProyecto) }
        val peticion = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_OBTENER_EVALUACION, datos,
            { respuesta ->
                if (respuesta.optBoolean("success")) {
                    // Carga los valores guardados
                    marcarValor(rgCrecimiento, respuesta.optInt("crecimiento"))
                    marcarValor(rgColorHojas, respuesta.optInt("color_hojas"))
                    marcarValor(rgFirmeza, respuesta.optInt("firmeza"))
                    marcarValor(rgRaices, respuesta.optInt("estado_raices"))
                    marcarValor(rgSinPlagas, respuesta.optInt("sin_plagas"))
                    etObservacion.setText(respuesta.optString("observacion"))
                    cargarFoto(respuesta.optString("foto"))
                    btnGuardar.text = "Actualizar evaluación"
                    mostrarPromedio()
                }
            },
            { Notificacion.error(this, "Error de conexión") }
        )
        Volley.newRequestQueue(this).add(peticion)
    }

    private fun marcarValor(grupo: RadioGroup, valor: Int) {
        // Marca la respuesta guardada
        for (i in 0 until grupo.childCount) {
            val boton = grupo.getChildAt(i) as? RadioButton
            if (boton?.tag?.toString() == valor.toString()) {
                grupo.check(boton.id)
                return
            }
        }
    }

    private fun cargarFoto(ruta: String) {
        if (ruta.isBlank()) return

        // Carga la foto anterior
        val peticion = ImageRequest(
            Configuracion.urlArchivo(ruta),
            { bitmap -> ivFotoCultivo.setImageBitmap(bitmap) },
            1280, 1280, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
            { Notificacion.advertencia(this, "No se pudo cargar la fotografía") }
        )
        Volley.newRequestQueue(this).add(peticion)
        btnTomarFoto.text = "Tomar nueva foto"
    }

    private fun guardarEvaluacion() {
        // Revisa las respuestas
        val valores = listaGrupos().map { valorSeleccionado(it) }
        if (valores.any { it == 0 }) {
            Notificacion.advertencia(this, "Completa los cinco criterios")
            return
        }

        // Evita envíos repetidos
        btnGuardar.isEnabled = false

        // Prepara los datos
        val datos = JSONObject().apply {
            put("id_proyecto", idProyecto)
            put("crecimiento", valores[0])
            put("color_hojas", valores[1])
            put("firmeza", valores[2])
            put("estado_raices", valores[3])
            put("sin_plagas", valores[4])
            put("observacion", etObservacion.text.toString().trim())
            // Solo envía una foto nueva
            fotoBase64Nueva?.let { put("foto", it) }
        }

        // Guarda la evaluación
        val peticion = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_GUARDAR_EVALUACION, datos,
            { respuesta ->
                btnGuardar.isEnabled = true
                if (respuesta.optBoolean("success")) {
                    // Limpia la foto pendiente
                    fotoBase64Nueva = null
                    btnGuardar.text = "Actualizar evaluación"
                    Notificacion.exito(this, respuesta.optString("mensaje"))
                } else Notificacion.error(this, respuesta.optString("mensaje", "No se pudo guardar"))
            },
            {
                btnGuardar.isEnabled = true
                Notificacion.error(this, "Error de conexión")
            }
        )
        Volley.newRequestQueue(this).add(peticion)
    }
}
