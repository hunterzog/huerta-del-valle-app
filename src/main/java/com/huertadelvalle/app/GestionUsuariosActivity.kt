package com.huertadelvalle.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class GestionUsuariosActivity : AppCompatActivity() {

    private lateinit var etBuscar: EditText
    private lateinit var tvTotal: TextView
    private lateinit var rvUsuarios: RecyclerView
    private lateinit var btnAnterior: Button
    private lateinit var btnSiguiente: Button
    private lateinit var tvPagina: TextView
    private lateinit var btnNuevoUsuario: Button
    private lateinit var btnVolverAdmin: Button
    // Orden seleccionado.
    private lateinit var spinnerOrdenar: Spinner

    private val listaUsuarios = mutableListOf<Map<String, String>>()
    private var paginaActual = 1
    private var totalPaginas = 1
    private var rol = ""
    private var idUsuario = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_usuarios)

        // Vincular vistas
        etBuscar        = findViewById(R.id.etBuscarUsuario)
        tvTotal         = findViewById(R.id.tvTotalUsuarios)
        rvUsuarios      = findViewById(R.id.rvUsuarios)
        btnAnterior     = findViewById(R.id.btnAnterior)
        btnSiguiente    = findViewById(R.id.btnSiguiente)
        tvPagina        = findViewById(R.id.tvPagina)
        btnNuevoUsuario = findViewById(R.id.btnNuevoUsuario)
        btnVolverAdmin  = findViewById(R.id.btnVolverAdmin)
        spinnerOrdenar  = findViewById(R.id.spinnerOrdenar)

        // Obtener datos del Intent
        rol       = intent.getStringExtra("rol") ?: ""
        idUsuario = intent.getIntExtra("id_usuario", 0)

        // Configurar RecyclerView
        rvUsuarios.layoutManager = LinearLayoutManager(this)

        // Configurar opciones de ordenamiento
        val opcionesOrden = arrayOf("Nombre A-Z", "Nombre Z-A", "Rol", "Estado")
        spinnerOrdenar.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item, opcionesOrden)

        spinnerOrdenar.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                paginaActual = 1
                cargarUsuarios()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Cargar usuarios
        cargarUsuarios()

        // Ordenar al tocar encabezados de columnas
        findViewById<TextView>(R.id.tvOrdenarNombre).setOnClickListener {
            spinnerOrdenar.setSelection(if (spinnerOrdenar.selectedItemPosition == 0) 1 else 0)
        }
        findViewById<TextView>(R.id.tvOrdenarUsuario).setOnClickListener {
            spinnerOrdenar.setSelection(if (spinnerOrdenar.selectedItemPosition == 2) 3 else 2)
        }
        findViewById<TextView>(R.id.tvOrdenarRol).setOnClickListener {
            spinnerOrdenar.setSelection(2)
        }
        findViewById<TextView>(R.id.tvOrdenarEstado).setOnClickListener {
            spinnerOrdenar.setSelection(3)
        }

        // Buscar en tiempo real mientras se escribe
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                paginaActual = 1
                cargarUsuarios()
            }
        })

        // Paginación
        btnAnterior.setOnClickListener {
            if (paginaActual > 1) {
                paginaActual--
                cargarUsuarios()
            }
        }

        btnSiguiente.setOnClickListener {
            if (paginaActual < totalPaginas) {
                paginaActual++
                cargarUsuarios()
            }
        }

        // Nuevo usuario
        btnNuevoUsuario.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        // Volver
        btnVolverAdmin.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        cargarUsuarios()
    }

    private fun cargarUsuarios() {
        val queue = Volley.newRequestQueue(this)

        val datos = JSONObject()
        datos.put("rol",      rol)
        datos.put("busqueda", etBuscar.text.toString().trim())
        datos.put("pagina",   paginaActual)

        // Agregar ordenamiento seleccionado
        val orden = when (spinnerOrdenar.selectedItemPosition) {
            0 -> "nombre ASC"
            1 -> "nombre DESC"
            2 -> "rol ASC"
            3 -> "estado ASC"
            else -> "nombre ASC"
        }
        datos.put("orden", orden)

        val peticion = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_LISTAR_USUARIOS, datos,
            { respuesta ->
                val success = respuesta.getBoolean("success")
                if (success) {
                    listaUsuarios.clear()
                    val usuarios = respuesta.getJSONArray("usuarios")
                    val total    = respuesta.getInt("total")
                    totalPaginas = respuesta.getInt("paginas")

                    for (i in 0 until usuarios.length()) {
                        val u = usuarios.getJSONObject(i)
                        listaUsuarios.add(mapOf(
                            "id_usuario" to u.getString("id_usuario"),
                            "nombre"     to u.getString("nombre"),
                            "apellido"   to u.getString("apellido"),
                            "usuario"    to u.getString("usuario"),
                            "correo"     to (if (u.isNull("correo")) "" else u.getString("correo")),
                            // Ruta de la selfie.
                            "foto"       to (if (u.isNull("foto")) "" else u.getString("foto")),
                            "rol"        to u.getString("rol"),
                            "estado"     to u.getString("estado")
                        ))
                    }

                    // Actualizar UI
                    rvUsuarios.adapter = UsuarioAdapter(listaUsuarios,
                        onModificar = { usuario ->
                            val intent = Intent(this, ModificarUsuarioActivity::class.java)
                            intent.putExtra("id_usuario_editar", usuario["id_usuario"]?.toInt() ?: 0)
                            intent.putExtra("nombre",     usuario["nombre"])
                            intent.putExtra("apellido",   usuario["apellido"])
                            intent.putExtra("usuario",    usuario["usuario"])
                            intent.putExtra("correo",     usuario["correo"])
                            // Envía la foto actual.
                            intent.putExtra("foto",       usuario["foto"])
                            intent.putExtra("rol_editar", usuario["rol"])
                            intent.putExtra("estado",     usuario["estado"])
                            intent.putExtra("rol",        rol)
                            intent.putExtra("id_usuario", idUsuario)
                            startActivity(intent)
                        },
                        onEliminar = { usuario, position ->
                            confirmarEliminar(usuario, position)
                        }
                    )

                    tvTotal.text  = "Total: $total usuarios"
                    tvPagina.text = "Página $paginaActual de $totalPaginas"

                } else {
                    Notificacion.error(this, respuesta.getString("mensaje"))
                }
            },
            { _ ->
                Notificacion.error(this, "Error de conexión")
            }
        )

        queue.add(peticion)
    }

    private fun confirmarEliminar(usuario: Map<String, String>, position: Int) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Eliminar usuario")
            .setMessage("¿Estás seguro que querés eliminar a ${usuario["nombre"]} ${usuario["apellido"]}?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarUsuario(usuario, position)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarUsuario(usuario: Map<String, String>, position: Int) {
        val queue = Volley.newRequestQueue(this)

        val datos = JSONObject()
        datos.put("rol",           rol)
        datos.put("id_usuario",    usuario["id_usuario"]?.toInt() ?: 0)
        datos.put("id_solicitante", idUsuario)

        val peticion = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_ELIMINAR_USUARIO, datos,
            { respuesta ->
                val success = respuesta.getBoolean("success")
                val mensaje = respuesta.getString("mensaje")
                if (success) {
                    Notificacion.exito(this, mensaje)
                    (rvUsuarios.adapter as UsuarioAdapter).eliminarItem(position)
                    cargarUsuarios()
                } else {
                    Notificacion.error(this, mensaje)
                }
            },
            { _ ->
                Notificacion.error(this, "Error de conexión")
            }
        )

        queue.add(peticion)
    }
}