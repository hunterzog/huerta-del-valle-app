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
    private val onCancelar: (String, Int) -> Unit

) : RecyclerView.Adapter<ProyectoAdapter.ProyectoViewHolder>() {

    // ViewHolder que contiene las vistas de cada item
    class ProyectoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView  = itemView.findViewById(R.id.tvNombreProyecto)
        val tvFecha: TextView   = itemView.findViewById(R.id.tvFechaInicio)
        val tvEstado: TextView  = itemView.findViewById(R.id.tvEstado)
        val btnCancelar: Button = itemView.findViewById(R.id.btnCancelarProyecto)
        val btnEliminar: Button = itemView.findViewById(R.id.btnEliminarProyecto)
        val vistaRaiz: View     = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProyectoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_proyecto, parent, false)
        return ProyectoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProyectoViewHolder, position: Int) {
        val proyecto = proyectos[position]

        // Asignar datos
        holder.tvNombre.text = proyecto["nombre"]
        holder.tvFecha.text  = "Inicio: ${proyecto["fecha_inicio"]}"
        holder.tvEstado.text = proyecto["estado"]?.uppercase()

        // Color del estado y borde izquierdo según estado
        val color = when (proyecto["estado"]) {
            "activo"     -> "#2E7D32"
            "cancelado"  -> "#757575"
            "finalizado" -> "#1565C0"
            else         -> "#757575"
        }

        // Color del badge de estado
        holder.tvEstado.setBackgroundColor(Color.parseColor(color))

        // Borde izquierdo de color según estado
        holder.vistaRaiz.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.WHITE)
            setStroke(8, Color.parseColor(color))
            cornerRadius = 12f
        }.also {
            // Aplicar solo borde izquierdo
            holder.vistaRaiz.setPadding(16, 8, 8, 8)
        }

        // Mostrar cancelar solo si está activo
        if (proyecto["estado"] == "activo") {
            holder.btnCancelar.visibility = View.VISIBLE
            holder.btnCancelar.setOnClickListener {
                onCancelar(proyecto["id_proyecto"] ?: "", position)
            }
        } else {
            holder.btnCancelar.visibility = View.GONE
        }

        // Botón eliminar
        holder.btnEliminar.setOnClickListener {
            onEliminar(proyecto["id_proyecto"] ?: "", position)
        }
    }

    override fun getItemCount() = proyectos.size

    // Eliminar item de la lista visualmente
    fun eliminarItem(position: Int) {
        proyectos.removeAt(position)
        notifyItemRemoved(position)
    }

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