package com.huertadelvalle.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
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

class ModificarUsuarioActivity : AppCompatActivity() {

    // Controles del formulario.
    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etUsuario: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etNuevaContrasena: EditText
    private lateinit var spinnerRol: Spinner
    private lateinit var spinnerEstado: Spinner
    private lateinit var ivFotoUsuario: ImageView
    private lateinit var btnTomarSelfie: Button
    private lateinit var btnModificar: Button
    private lateinit var btnCancelar: Button
    private lateinit var tvTooltipUsuario: TextView

    private var idUsuarioEditar = 0
    private var rol = ""
    private var idUsuario = 0
    // Datos de la fotografía.
    private var fotoActual = ""
    private var fotoBase64Nueva: String? = null

    // Captura la nueva selfie.
    private val tomarSelfieLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap == null) {
                Notificacion.advertencia(this, "No se capturó la fotografía")
                return@registerForActivityResult
            }
            ivFotoUsuario.setPadding(0, 0, 0, 0)
            ivFotoUsuario.setImageBitmap(bitmap)
            fotoBase64Nueva = convertirBitmapBase64(bitmap)
            btnTomarSelfie.text = "Volver a tomar selfie"
        }

    // Solicita permiso de cámara.
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
        setContentView(R.layout.activity_modificar_usuario)

        // Enlace con el diseño XML.
        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etUsuario = findViewById(R.id.etUsuario)
        etCorreo = findViewById(R.id.etCorreo)
        etNuevaContrasena = findViewById(R.id.etNuevaContrasena)
        spinnerRol = findViewById(R.id.spinnerRol)
        spinnerEstado = findViewById(R.id.spinnerEstado)
        ivFotoUsuario = findViewById(R.id.ivFotoUsuario)
        btnTomarSelfie = findViewById(R.id.btnTomarSelfie)
        btnModificar = findViewById(R.id.btnModificar)
        btnCancelar = findViewById(R.id.btnCancelar)
        tvTooltipUsuario = findViewById(R.id.tvTooltipUsuario)

        // Datos recibidos de la lista.
        idUsuarioEditar = intent.getIntExtra("id_usuario_editar", 0)
        rol = intent.getStringExtra("rol") ?: ""
        idUsuario = intent.getIntExtra("id_usuario", 0)
        fotoActual = intent.getStringExtra("foto") ?: ""

        etNombre.setText(intent.getStringExtra("nombre"))
        etApellido.setText(intent.getStringExtra("apellido"))
        etUsuario.setText(intent.getStringExtra("usuario"))
        etCorreo.setText(intent.getStringExtra("correo"))

        // Carga los datos actuales.
        configurarSpinners()
        cargarFotoActual()

        tvTooltipUsuario.setOnClickListener {
            Notificacion.informacion(
                this,
                "El usuario debe ser único. No distingue mayúsculas de minúsculas."
            )
        }
        // Acciones de la pantalla.
        btnTomarSelfie.setOnClickListener { abrirCamara() }
        btnModificar.setOnClickListener {
            if (validar()) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Confirmar modificación")
                    .setMessage("¿Estás seguro que querés modificar los datos de este usuario?")
                    .setPositiveButton("Modificar") { _, _ -> modificarUsuario() }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
        btnCancelar.setOnClickListener { finish() }
    }

    private fun configurarSpinners() {
        // Opciones de rol y estado.
        val roles = arrayOf("usuario", "administrador")
        spinnerRol.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            roles
        )
        val rolActual = intent.getStringExtra("rol_editar") ?: "usuario"
        spinnerRol.setSelection(roles.indexOf(rolActual).coerceAtLeast(0))

        val estados = arrayOf("activo", "inactivo")
        spinnerEstado.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            estados
        )
        val estadoActual = intent.getStringExtra("estado") ?: "activo"
        spinnerEstado.setSelection(estados.indexOf(estadoActual).coerceAtLeast(0))
    }

    private fun cargarFotoActual() {
        if (fotoActual.isBlank()) return

        // Carga la selfie guardada.
        val solicitud = ImageRequest(
            Configuracion.urlArchivo(fotoActual),
            { bitmap ->
                ivFotoUsuario.setPadding(0, 0, 0, 0)
                ivFotoUsuario.setImageBitmap(bitmap)
            },
            800,
            800,
            ImageView.ScaleType.CENTER_CROP,
            Bitmap.Config.RGB_565,
            { btnTomarSelfie.text = "Tomar nueva selfie" }
        )
        Volley.newRequestQueue(this).add(solicitud)
    }

    private fun abrirCamara() {
        // Verifica el permiso.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            tomarSelfieLauncher.launch(null)
        } else {
            solicitarPermisoCamara.launch(Manifest.permission.CAMERA)
        }
    }

    private fun convertirBitmapBase64(bitmap: Bitmap): String {
        // Comprime la imagen.
        val salida = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, salida)
        return Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP)
    }

    private fun validar(): Boolean {
        // Valida los campos obligatorios.
        if (etNombre.text.toString().trim().isEmpty()) {
            etNombre.error = "El nombre es obligatorio"
            etNombre.requestFocus()
            return false
        }
        if (etApellido.text.toString().trim().isEmpty()) {
            etApellido.error = "El apellido es obligatorio"
            etApellido.requestFocus()
            return false
        }
        if (etUsuario.text.toString().trim().isEmpty()) {
            etUsuario.error = "El usuario es obligatorio"
            etUsuario.requestFocus()
            return false
        }
        return true
    }

    private fun modificarUsuario() {
        // Evita envíos repetidos.
        btnModificar.isEnabled = false
        btnModificar.text = "Modificando..."

        val datos = JSONObject().apply {
            put("rol", rol)
            put("id_usuario", idUsuarioEditar)
            put("id_solicitante", idUsuario)
            put("nombre", etNombre.text.toString().trim())
            put("apellido", etApellido.text.toString().trim())
            put("usuario", etUsuario.text.toString().trim())
            put("correo", etCorreo.text.toString().trim())
            put("rol_nuevo", spinnerRol.selectedItem.toString())
            put("estado", spinnerEstado.selectedItem.toString())

            val nuevaContrasena = etNuevaContrasena.text.toString().trim()
            if (nuevaContrasena.isNotEmpty()) put("nueva_contrasena", nuevaContrasena)
            // Solo envía una foto nueva.
            fotoBase64Nueva?.let { put("foto", it) }
        }

        // Envía los cambios al servidor.
        val peticion = object : JsonObjectRequest(
            Request.Method.POST,
            Configuracion.URL_MODIFICAR_USUARIO,
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

        Volley.newRequestQueue(this).add(peticion)
    }

    private fun restaurarBoton() {
        // Habilita otra vez el botón.
        btnModificar.isEnabled = true
        btnModificar.text = "Modificar"
    }
}