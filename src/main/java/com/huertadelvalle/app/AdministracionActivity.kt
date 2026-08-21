package com.huertadelvalle.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AdministracionActivity : AppCompatActivity() {

    private lateinit var btnGestionUsuarios: Button
    private lateinit var btnControlValvulas: Button
    private lateinit var btnControlBombas: Button
    private lateinit var btnNivelAgua: Button
    private lateinit var btnVolver: Button

    private var rol: String? = null
    private var idUsuario: Int = 0
    private var idProyecto: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_administracion)

        rol = intent.getStringExtra("rol")
        idUsuario = intent.getIntExtra("id_usuario", 0)
        idProyecto = intent.getIntExtra("id_proyecto", 0)

        btnGestionUsuarios = findViewById(R.id.btnGestionUsuarios)
        btnControlValvulas = findViewById(R.id.btnControlValvulas)
        btnControlBombas   = findViewById(R.id.btnControlBombas)
        btnNivelAgua       = findViewById(R.id.btnNivelAgua)
        btnVolver          = findViewById(R.id.btnVolver)

        btnGestionUsuarios.setOnClickListener {
            val intent = Intent(this, GestionUsuariosActivity::class.java)
            intent.putExtra("rol", rol)
            intent.putExtra("id_usuario", idUsuario)
            startActivity(intent)
        }

        btnControlValvulas.setOnClickListener {
            val intent = Intent(this, ControlValvulasActivity::class.java)
            intent.putExtra("rol", rol)
            intent.putExtra("id_usuario", idUsuario)
            startActivity(intent)
        }

        btnControlBombas.setOnClickListener {
            val intent = Intent(this, ControlBombasActivity::class.java)
            intent.putExtra("rol", rol)
            intent.putExtra("id_usuario", idUsuario)
            startActivity(intent)
        }

        btnNivelAgua.setOnClickListener {
            val intent = Intent(this, NivelAguaActivity::class.java)
            intent.putExtra("rol", rol)
            intent.putExtra("id_usuario", idUsuario)
            intent.putExtra("id_proyecto", idProyecto)
            startActivity(intent)
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }
}