package com.huertadelvalle.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast

class AdministracionActivity : AppCompatActivity() {

    private lateinit var btnGestionUsuarios: Button

    // Botón que lleva a la pantalla de control manual de válvulas
    private lateinit var btnControlValvulas: Button
    private lateinit var btnVolver: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_administracion)

        // Vincular vistas
        btnGestionUsuarios = findViewById(R.id.btnGestionUsuarios)
        btnControlValvulas = findViewById(R.id.btnControlValvulas)
        btnVolver          = findViewById(R.id.btnVolver)

        // Obtener rol del Intent
        val rol        = intent.getStringExtra("rol") ?: ""
        val idUsuario  = intent.getIntExtra("id_usuario", 0)

        btnGestionUsuarios.setOnClickListener {
            val intent = Intent(this, GestionUsuariosActivity::class.java)
            intent.putExtra("rol", rol)
            intent.putExtra("id_usuario", idUsuario)
            startActivity(intent)
        }

        // Al tocar el botón, abre ControlValvulasActivity
        // pasando el rol y el id del usuario, igual que se hace con las demás pantallas
        btnControlValvulas.setOnClickListener {
            val intent = Intent(this, ControlValvulasActivity::class.java)
            intent.putExtra("rol", rol)
            intent.putExtra("id_usuario", idUsuario)
            startActivity(intent)
        }

        // Botón volver
        btnVolver.setOnClickListener {
            finish()
        }
    }
}