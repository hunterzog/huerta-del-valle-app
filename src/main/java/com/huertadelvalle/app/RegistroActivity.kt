


package com.huertadelvalle.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class RegistroActivity : AppCompatActivity() {

    // Referencias a los controles definidos en activity_registro.xml.
    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etCorreoReg: EditText
    private lateinit var etUsuarioReg: EditText
    private lateinit var etContrasenaReg: EditText
    private lateinit var ivFotoUsuario: ImageView
    private lateinit var btnTomarSelfie: Button
    private lateinit var btnRegistrar: Button
    private lateinit var btnVolver: Button
    private lateinit var tvTooltipUsuario: TextView

    // Guarda temporalmente la selfie codificada antes de enviarla al servidor.
    private var fotoBase64: String? = null

    // Abre la aplicación de cámara y recibe una imagen de tamaño reducido.
    private val tomarSelfieLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap == null) {
                Notificacion.advertencia(this, "No se capturó la fotografía")
                return@registerForActivityResult
            }

            ivFotoUsuario.setPadding(0, 0, 0, 0)
            ivFotoUsuario.setImageBitmap(bitmap)
            fotoBase64 = convertirBitmapBase64(bitmap)
            btnTomarSelfie.text = "Volver a tomar selfie"
        }

    // Solicita el permiso de cámara únicamente cuando el usuario intenta usarla.
    private val solicitarPermisoCamara =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
            if (concedido) {
                tomarSelfieLauncher.launch(null)
            } else {
                Notificacion.advertencia(
                    this,
                    "Debes permitir el acceso a la cámara para tomar la selfie"
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // Vinculación de los controles XML con el código Kotlin.
        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etCorreoReg = findViewById(R.id.etCorreoReg)
        etUsuarioReg = findViewById(R.id.etUsuarioReg)
        etContrasenaReg = findViewById(R.id.etContrasenaReg)
        ivFotoUsuario = findViewById(R.id.ivFotoUsuario)
        btnTomarSelfie = findViewById(R.id.btnTomarSelfie)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        btnVolver = findViewById(R.id.btnVolver)
        tvTooltipUsuario = findViewById(R.id.tvTooltipUsuario)

        tvTooltipUsuario.setOnClickListener {
            Notificacion.informacion(
                this,
                "El usuario debe ser único. No distingue mayúsculas."
            )
        }

        // Acciones principales de la pantalla.
        btnTomarSelfie.setOnClickListener {
            abrirCamara()
        }
        btnRegistrar.setOnClickListener {
            registrarSiEsValido()
        }
        btnVolver.setOnClickListener {
            // Cierra esta pantalla y regresa a la anterior.
            finish()
        }
    }

    private fun abrirCamara() {
        // Si el permiso ya existe se abre la cámara; de lo contrario se solicita.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            // Abre la cámara cuando el permiso ya fue concedido.
            tomarSelfieLauncher.launch(null)
        } else {
            // Solicita el permiso antes de intentar abrir la cámara.
            solicitarPermisoCamara.launch(Manifest.permission.CAMERA)
        }
    }

    private fun registrarSiEsValido() {
        // trim() elimina espacios accidentales al inicio y al final.
        val nombre = etNombre.text.toString().trim()
        val apellido = etApellido.text.toString().trim()
        val correo = etCorreoReg.text.toString().trim()
        val usuario = etUsuarioReg.text.toString().trim()
        val contrasena = etContrasenaReg.text.toString().trim()

        etNombre.error = null
        etApellido.error = null
        etUsuarioReg.error = null
        etContrasenaReg.error = null

        // Esta bandera impide enviar información incompleta al servidor.
        var hayError = false

        if (nombre.isEmpty()) {
            etNombre.error = "El nombre es obligatorio"
            etNombre.requestFocus()
            hayError = true
        }
        if (apellido.isEmpty()) {
            etApellido.error = "El apellido es obligatorio"
            if (!hayError) etApellido.requestFocus()
            hayError = true
        }
        if (usuario.isEmpty()) {
            etUsuarioReg.error = "El usuario es obligatorio"
            if (!hayError) etUsuarioReg.requestFocus()
            hayError = true
        }
        if (contrasena.isEmpty()) {
            etContrasenaReg.error = "La contraseña es obligatoria"
            if (!hayError) etContrasenaReg.requestFocus()
            hayError = true
        }
        if (fotoBase64 == null) {
            Notificacion.advertencia(this, "Debes tomar una selfie del usuario")
            hayError = true
        }

        // Solo se ejecuta la petición cuando todos los datos son válidos.
        if (!hayError) registrar(nombre, apellido, usuario, correo, contrasena)
    }

    private fun convertirBitmapBase64(bitmap: Bitmap): String {
        // JPEG al 80 % reduce el tamaño enviado sin perder demasiada calidad.
        val salida = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, salida)
        return Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP)
    }

    private fun registrar(
        nombre: String,
        apellido: String,
        usuario: String,
        correo: String,
        contrasena: String
    ) {
        // Se bloquea el botón para evitar registros duplicados por doble toque.
        btnRegistrar.isEnabled = false
        btnRegistrar.text = "Registrando..."

        // La API recibe los datos y la selfie dentro de un objeto JSON.
        val datos = JSONObject().apply {
            put("nombre", nombre)
            put("apellido", apellido)
            put("correo", correo)
            put("usuario", usuario)
            put("contrasena", contrasena)
            put("foto", fotoBase64)
        }

        // Volley envía el JSON mediante POST y procesa la respuesta de la API.
        val peticion = object : JsonObjectRequest(
            Request.Method.POST,
            Configuracion.URL_REGISTRO,
            datos,
            { respuesta ->
                restaurarBoton()
                val success = respuesta.optBoolean("success", false)
                val mensaje = respuesta.optString("mensaje", "Respuesta inválida")
                if (success) {
                    Notificacion.exito(this, mensaje)
                    finish()
                } else {
                    Notificacion.error(this, mensaje)
                }
            },
            {
                restaurarBoton()
                Notificacion.error(this, "Error de conexión")
            }
        ) {
            override fun getBodyContentType() = "application/json; charset=utf-8"
        }

        // Agrega la petición a la cola de comunicaciones de la aplicación.
        Volley.newRequestQueue(this).add(peticion)
    }

    private fun restaurarBoton() {
        // Se ejecuta después de recibir una respuesta o un error de conexión.
        btnRegistrar.isEnabled = true
        btnRegistrar.text = "Registrar"
    }
}