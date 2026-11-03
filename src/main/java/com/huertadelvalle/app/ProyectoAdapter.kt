package com.huertadelvalle.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProyectoAdapter(
    private val proyectos: MutableList<Map<String, String>>,
    private val onEliminar: (String, Int) -> Unit,
    private val onCancelar: (String, Int) -> Unit,
    private val onInformeFinal: (String, String) -> Unit,
    private val onInformeParcial: (String, String) -> Unit

) : RecyclerView.Adapter<ProyectoAdapter.ProyectoViewHolder>() {

    // ViewHolder que contiene las vistas de cada item
    class ProyectoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView         = itemView.findViewById(R.id.tvNombreProyecto)
        val tvFecha: TextView          = itemView.findViewById(R.id.tvFechaInicio)
        val tvEstado: TextView         = itemView.findViewById(R.id.tvEstado)
        val btnCancelar: Button        = itemView.findViewById(R.id.btnCancelarProyecto)
        val btnInformeFinal: Button    = itemView.findViewById(R.id.btnInformeFinal)
        val btnInformeParcial: Button  = itemView.findViewById(R.id.btnInformeParcial)
        val btnEliminar: Button        = itemView.findViewById(R.id.btnEliminarProyecto)
        val vistaRaiz: View            = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProyectoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_proyecto, parent, false)
        return ProyectoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProyectoViewHolder, position: Int) {
        val proyecto = proyectos[position]

        val idProyecto     = proyecto["id_proyecto"] ?: ""
        val nombreProyecto = proyecto["nombre"] ?: ""
        val estado         = proyecto["estado"] ?: ""
        // Estado real antes de eliminar: decide qué informe tiene el proyecto.
        val estadoOriginal = proyecto["estado_original"] ?: estado

        // Asignar datos
        holder.tvNombre.text = nombreProyecto
        holder.tvFecha.text  = "Inicio: ${proyecto["fecha_inicio"]}"
        holder.tvEstado.text = estado.uppercase()

        // Color del estado y borde izquierdo según estado
        val color = when (estado) {
            "activo"     -> "#2E7D32"
            "cancelado"  -> "#757575"
            "finalizado" -> "#1565C0"
            "eliminado"  -> "#8E3B3B"
            else         -> "#757575"
        }

        // Color del badge de estado
        holder.tvEstado.setBackgroundColor(Color.parseColor(color))

        // Borde de color según estado
        holder.vistaRaiz.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.WHITE)
            setStroke(8, Color.parseColor(color))
            cornerRadius = 12f
        }.also {
            holder.vistaRaiz.setPadding(16, 8, 8, 8)
        }

        // Cancelar: solo proyecto activo
        if (estado == "activo") {
            holder.btnCancelar.visibility = View.VISIBLE
            holder.btnCancelar.setOnClickListener { onCancelar(idProyecto, position) }
        } else {
            holder.btnCancelar.visibility = View.GONE
            holder.btnCancelar.setOnClickListener(null)
        }

        // Informe final: proyectos que se finalizaron (aunque luego se eliminen)
        if (estadoOriginal == "finalizado") {
            holder.btnInformeFinal.visibility = View.VISIBLE
            holder.btnInformeFinal.setOnClickListener { onInformeFinal(idProyecto, nombreProyecto) }
        } else {
            holder.btnInformeFinal.visibility = View.GONE
            holder.btnInformeFinal.setOnClickListener(null)
        }

        // Informe parcial: proyectos que se cancelaron (aunque luego se eliminen)
        if (estadoOriginal == "cancelado") {
            holder.btnInformeParcial.visibility = View.VISIBLE
            holder.btnInformeParcial.setOnClickListener { onInformeParcial(idProyecto, nombreProyecto) }
        } else {
            holder.btnInformeParcial.visibility = View.GONE
            holder.btnInformeParcial.setOnClickListener(null)
        }

        // Eliminar: solo cancelados o finalizados que todavía no fueron eliminados
        if (estado == "cancelado" || estado == "finalizado") {
            holder.btnEliminar.visibility = View.VISIBLE
            holder.btnEliminar.setOnClickListener { onEliminar(idProyecto, position) }
        } else {
            holder.btnEliminar.visibility = View.GONE
            holder.btnEliminar.setOnClickListener(null)
        }
    }

    override fun getItemCount() = proyectos.size

    // Filtrar lista localmente
    fun filtrar(listaCompleta: List<Map<String, String>>, texto: String, orden: String) {
        proyectos.clear()
        val filtrados = if (texto.isEmpty()) {
            listaCompleta.toMutableList()
        } else {
            listaCompleta.filter {
                it["nombre"]?.contains(texto, ignoreCase = true) == true ||
                        it["estado"]?.contains(texto, ignoreCase = true) == true
            }.toMutableList()
        }

        // Ordenar
        val ordenados = when (orden) {
            "Nombre A-Z"  -> filtrados.sortedBy { it["nombre"] }
            "Nombre Z-A"  -> filtrados.sortedByDescending { it["nombre"] }
            "Estado"      -> filtrados.sortedBy { it["estado"] }
            else          -> filtrados.sortedByDescending { it["fecha_inicio"] }
        }

        proyectos.addAll(ordenados)
        notifyDataSetChanged()
    }
}