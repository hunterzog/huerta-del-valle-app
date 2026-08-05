package com.huertadelvalle.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UsuarioAdapter(
    private val usuarios: MutableList<Map<String, String>>,
    private val onModificar: (Map<String, String>) -> Unit,
    private val onEliminar: (Map<String, String>, Int) -> Unit
) : RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder>() {

    class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView  = itemView.findViewById(R.id.tvNombreUsuario)
        val tvRol: TextView     = itemView.findViewById(R.id.tvRolUsuario)
        val tvUsuario: TextView = itemView.findViewById(R.id.tvUsuario)
        val tvEstado: TextView  = itemView.findViewById(R.id.tvEstadoUsuario)
        val btnModificar: Button = itemView.findViewById(R.id.btnModificarUsuario)
        val btnEliminar: Button  = itemView.findViewById(R.id.btnEliminarUsuario)
        val tvCorreo: TextView = itemView.findViewById(R.id.tvCorreoUsuario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = usuarios[position]

        holder.tvNombre.text  = "${usuario["nombre"]} ${usuario["apellido"]}"
        holder.tvUsuario.text = "@${usuario["usuario"]}"
        holder.tvCorreo.text  = usuario["correo"] ?: ""

        // Rol con color
        holder.tvRol.text = usuario["rol"]?.uppercase() ?: ""
        holder.tvRol.setBackgroundColor(
            if (usuario["rol"] == "administrador")
                android.graphics.Color.parseColor("#B71C1C")
            else
                android.graphics.Color.parseColor("#1565C0")
        )

        // Estado con color
        holder.tvEstado.text = usuario["estado"]?.uppercase() ?: ""
        holder.tvEstado.setBackgroundColor(
            if (usuario["estado"] == "activo")
                android.graphics.Color.parseColor("#2E7D32")
            else
                android.graphics.Color.parseColor("#757575")
        )

        // Botones
        holder.btnModificar.setOnClickListener { onModificar(usuario) }
        holder.btnEliminar.setOnClickListener  { onEliminar(usuario, position) }
    }

    override fun getItemCount() = usuarios.size

    fun eliminarItem(position: Int) {
        usuarios.removeAt(position)
        notifyItemRemoved(position)
    }
}