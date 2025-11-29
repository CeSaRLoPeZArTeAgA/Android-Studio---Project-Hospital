package com.example.mykhospital

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class FormActivity : AppCompatActivity() {

    private lateinit var btnRegresar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form)
        btnRegresar = findViewById(R.id.btnRegresar)

        btnRegresar.setOnClickListener {
            finish()   // ← Regresa a MainActivity
        }
    }
}