package com.example.mykhospital

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class EmergenciaActivity: AppCompatActivity() {
    private lateinit var btnMapa: Button
    private lateinit var btnSalir: Button
    private lateinit var btnRegresar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_emergencia)

        btnMapa = findViewById<Button>(R.id.btnMapa)
        btnMapa.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

        //boton salir del sistema
        btnSalir = findViewById<Button>(R.id.btnSalir)
        btnSalir.setOnClickListener {
            finishAffinity()
        }

        // boton regresar a pantalla inicial
        btnRegresar = findViewById(R.id.btnRegresar)
        btnRegresar.setOnClickListener {
            finish()
        }
    }
}