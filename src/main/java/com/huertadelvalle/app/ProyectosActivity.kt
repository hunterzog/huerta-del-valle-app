package com.huertadelvalle.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner

class ProyectosActivity : AppCompatActivity() {

    // Vistas de la pantalla
    private lateinit var rvProyectos: RecyclerView
    private lateinit var btnNuevoProyecto: Button
    private lateinit var btnContinuarProyecto: Button
    private lateinit var btnAdministracion: Button
    private lateinit var tvCerrarSesion: TextView

    private lateinit var etBuscarProyecto: EditText

    private lateinit var spinnerOrdenarProyectos: Spinner

    private lateinit var tvTotalProyectos: TextView

    private val listaProyectosCompleta = mutableListOf<Map<String, String>>()


    // Lista de proyectos
    private val listaProyectos = mutableListOf<Map<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proyectos)

        // Vincular vistas
        rvProyectos          = findViewById(R.id.rvProyectos)
        btnNuevoProyecto     = findViewById(R.id.btnNuevoProyecto)
        btnContinuarProyecto = findViewById(R.id.btnContinuarProyecto)
        btnAdministracion    = findViewById(R.id.btnAdministracion)
        tvCerrarSesion       = findViewById(R.id.tvCerrarSesion)

        // Configurar RecyclerView
        rvProyectos.layoutManager = LinearLayoutManager(this)

        // Obtener datos de la sesión guardada
        val sesion    = SesionManager(this)
        val idUsuario = sesion.getIdUsuario()
        val rol       = sesion.getRol()

        // Mostrar botón administración solo si es administrador
        if (rol == "administrador") {
            btnAdministracion.visibility = android.view.View.VISIBLE
        }

        // Cargar proyectos
        cargarProyectos(idUsuario)

        // Botón nuevo proyecto
        btnNuevoProyecto.setOnClickListener {
            val activo = listaProyectos.find { it["estado"] == "activo" }
            if (activo != null) {
                Notificacion.advertencia(this,
                    "Ya hay un proyecto activo: ${activo["nombre"]}. Finalizalo o cancelalo primero.")
            } else {
                val intent = Intent(this, NuevoProyectoActivity::class.java)
                intent.putExtra("id_usuario", idUsuario)
                startActivity(intent)
            }
        }

        // Botón continuar proyecto activo
        btnContinuarProyecto.setOnClickListener {
            val activo = listaProyectos.find { it["estado"] == "activo" }
            if (activo != null) {
                val intent = Intent(this, ProyectoActivoActivity::class.java)
                intent.putExtra("id_proyecto",  activo["id_proyecto"]?.toInt() ?: 0)
                intent.putExtra("nombre",       activo["nombre"])
                intent.putExtra("fecha_inicio", activo["fecha_inicio"])
                intent.putExtra("semilla",      activo["semilla"] ?: "")
                intent.putExtra("nutriente",    activo["nutriente"] ?: "")
                startActivity(intent)
            } else {
                Notificacion.advertencia(this, "No hay proyecto activo")
            }
        }

        // Botón administración
        btnAdministracion.setOnClickListener {
            val activo = listaProyectos.find { it["estado"] == "activo" }
            val intent = Intent(this, AdministracionActivity::class.java)
            intent.putExtra("rol", rol)
            intent.putExtra("id_usuario", idUsuario)
            intent.putExtra("id_proyecto", activo?.get("id_proyecto")?.toInt() ?: 0)
            startActivity(intent)
        }

        // Cerrar sesión con confirmación
        tvCerrarSesion.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro que querés cerrar sesión?")
                .setPositiveButton("Salir") { _, _ ->
                    sesion.cerrarSesion()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // Vincular buscador y ordenamiento
        etBuscarProyecto       = findViewById(R.id.etBuscarProyecto)
        spinnerOrdenarProyectos = findViewById(R.id.spinnerOrdenarProyectos)
        tvTotalProyectos       = findViewById(R.id.tvTotalProyectos)

        // Configurar spinner de ordenamiento
        val opcionesOrden = arrayOf("Fecha ↓", "Nombre A-Z", "Nombre Z-A", "Estado")
        spinnerOrdenarProyectos.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item, opcionesOrden)

        // Ordenar al cambiar spinner
        spinnerOrdenarProyectos.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                filtrarProyectos()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Buscar en tiempo real
        etBuscarProyecto.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { filtrarProyectos() }
        })
    }

    override fun onResume() {
        super.onResume()
        // Recargar proyectos usando la sesión guardada
        val sesion = SesionManager(this)
        cargarProyectos(sesion.getIdUsuario())
    }

    private fun cargarProyectos(idUsuario: Int) {
        val queue = Volley.newRequestQueue(this)

        val datos = JSONObject()
        datos.put("id_usuario", idUsuario)

        val peticion = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_PROYECTOS, datos,
            { respuesta ->
                val success = respuesta.getBoolean("success")
                if (success) {
                    listaProyectos.clear()
                    val proyectos = respuesta.getJSONArray("proyectos")
                    for (i in 0 until proyectos.length()) {
                        val p = proyectos.getJSONObject(i)
                        listaProyectos.add(mapOf(
                            "id_proyecto"  to p.getString("id_proyecto"),
                            "nombre"       to p.getString("nombre"),
                            "fecha_inicio" to p.getString("fecha_inicio"),
                            "estado"       to p.getString("estado"),
                            "semilla"      to if (p.isNull("nombre_semilla")) "" else p.getString("nombre_semilla"),
                            "nutriente"    to if (p.isNull("nombre_comercial")) "" else p.getString("nombre_comercial")
                        ))
                    }

            // Copiar lista completa para filtrado
                    listaProyectosCompleta.clear()
                    listaProyectosCompleta.addAll(listaProyectos)

            // Actualizar lista con adapter
                    rvProyectos.adapter = ProyectoAdapter(listaProyectos,
                        onEliminar = { idProyecto, position ->
                            confirmarEliminar(idProyecto, position)
                        },
                        onCancelar = { idProyecto, position ->
                            confirmarCancelar(idProyecto, position)
                        }
                    )

                // Aplicar filtro y ordenamiento
                    filtrarProyectos()
                } else {
                    Notificacion.error(this, "Error al cargar proyectos")
                }
            },
            { _ ->
                Notificacion.error(this, "Error de conexión")
            }
        )

        queue.add(peticion)
    }

    private fun confirmarCancelar(idProyecto: String, position: Int) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Cancelar proyecto")
            .setMessage("¿Estás seguro que querés cancelar este proyecto?")
            .setPositiveButton("Cancelar proyecto") { _, _ ->
                cancelarProyecto(idProyecto, position)
            }
            .setNegativeButton("Volver", null)
            .show()
    }

    private fun cancelarProyecto(idProyecto: String, position: Int) {
        val queue = Volley.newRequestQueue(this)

        val datos = JSONObject()
        datos.put("id_proyecto", idProyecto)

        val peticion = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_CANCELAR_PROYECTO, datos,
            { respuesta ->
                val success = respuesta.getBoolean("success")
                val mensaje = respuesta.getString("mensaje")
                if (success) {
                    Notificacion.exito(this, mensaje)
                    val sesion = SesionManager(this)
                    cargarProyectos(sesion.getIdUsuario())
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

    private fun confirmarEliminar(idProyecto: String, position: Int) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Eliminar proyecto")
            .setMessage("¿Estás seguro que querés eliminar este proyecto?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarProyecto(idProyecto, position)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarProyecto(idProyecto: String, position: Int) {
        val queue = Volley.newRequestQueue(this)

        val datos = JSONObject()
        datos.put("id_proyecto", idProyecto)

        val peticion = JsonObjectRequest(
            Request.Method.POST, Configuracion.URL_ELIMINAR_PROYECTO, datos,
            { respuesta ->
                val success = respuesta.getBoolean("success")
                val mensaje = respuesta.getString("mensaje")
                if (success) {
                    Notificacion.exito(this, mensaje)
                    (rvProyectos.adapter as ProyectoAdapter).eliminarItem(position)
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

    private fun filtrarProyectos() {
        val texto = etBuscarProyecto.text.toString().trim()
        val orden = spinnerOrdenarProyectos.selectedItem?.toString() ?: "Fecha ↓"
        tvTotalProyectos.text = "Total: ${listaProyectosCompleta.size} proyectos"
        (rvProyectos.adapter as? ProyectoAdapter)?.filtrar(listaProyectosCompleta, texto, orden)
    }
}