package com.example.mykhospital

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class EmergenciaActivity: AppCompatActivity() {
    private lateinit var btnMapa: Button
    private lateinit var btnSalir: Button
    private lateinit var btnRegresar: Button


    private lateinit var chCardiologia: CheckBox
    private lateinit var chNeumologia: CheckBox
    private lateinit var chTraumatologia: CheckBox
    private lateinit var chEndocrinologia: CheckBox
    private lateinit var chOncologia: CheckBox
    private lateinit var chHematologia: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_emergencia)


        // CheckBox
        chCardiologia = findViewById(R.id.chCardiologia)
        chNeumologia = findViewById(R.id.chNeumologia)
        chTraumatologia = findViewById(R.id.chTraumatologia)
        chEndocrinologia = findViewById(R.id.chEndocrinologia)
        chOncologia = findViewById(R.id.chOncologia)
        chHematologia = findViewById(R.id.chHematologia)



        btnMapa = findViewById(R.id.btnMapa)
        btnMapa.setOnClickListener {
            // variables de especialidades
            val especialidades = arrayListOf<String>()

            if (chCardiologia.isChecked) especialidades.add("Cardiología")
            if (chNeumologia.isChecked) especialidades.add("Neumología")
            if (chTraumatologia.isChecked) especialidades.add("Traumatología")
            if (chEndocrinologia.isChecked) especialidades.add("Endocrinología")
            if (chOncologia.isChecked) especialidades.add("Oncología")
            if (chHematologia.isChecked) especialidades.add("Hematología")

            if (especialidades.isEmpty()) {
                Toast.makeText(
                    this,
                    "Debe seleccionar al menos una especialidad",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // horario actual del GPS
            val horarioConsulta = obtenerHorarioActual()

            // enviar datos al mapa
            val intent = Intent(this, MapActivity::class.java)
            intent.putStringArrayListExtra("especialidades", especialidades)
            intent.putExtra("horarioConsulta", horarioConsulta)
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

    // determina horario según hora real
    private fun obtenerHorarioActual(): String {
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        return when {
            hora in 6 until 12 -> "Mañana"
            hora in 12 until 16 -> "Tarde"
            else -> "24 horas"
        }
    }
}