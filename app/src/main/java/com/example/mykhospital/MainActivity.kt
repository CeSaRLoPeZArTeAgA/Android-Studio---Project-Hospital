package com.example.mykhospital

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var btnFormulario: Button
    private lateinit var btnEmergencia: Button
    private lateinit var btnSalir: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        //boton entrar a formulario para carga o correccion de hospital
        btnFormulario = findViewById(R.id.btnFormulario)
        btnFormulario.setOnClickListener {
            val intent = Intent(this, FormActivity::class.java)
            startActivity(intent)
        }

        //boton que entra a formulario para ver hospitales para atencion de emergencia
        btnEmergencia = findViewById<Button>(R.id.btnEmergencia)
        btnEmergencia.setOnClickListener {
            val intent = Intent(this, EmergenciaActivity::class.java)
            startActivity(intent)
        }

        //boton salir del sistema
        btnSalir = findViewById<Button>(R.id.btnSalir)
        btnSalir.setOnClickListener {
            finishAffinity()
        }

    }
}