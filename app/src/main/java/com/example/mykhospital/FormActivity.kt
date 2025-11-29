package com.example.mykhospital

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class FormActivity : AppCompatActivity() {

    private lateinit var btnRegresar: Button
    private lateinit var btnSalir: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form)
        btnRegresar = findViewById(R.id.btnRegresar)

        // Regresa a MainActivity
        btnRegresar.setOnClickListener {
            finish()
        }

        //boton salir del sistema
        btnSalir = findViewById<Button>(R.id.btnSalir)
        btnSalir.setOnClickListener {
            finishAffinity()
        }

    }
}