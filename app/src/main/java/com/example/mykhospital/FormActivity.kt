package com.example.mykhospital

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.database.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FormActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var etNombre: EditText
    private lateinit var etDireccion: EditText
    private lateinit var etLatitud: EditText
    private lateinit var etLongitud: EditText
    private lateinit var spinnerGenero: Spinner
    private lateinit var btnAgregar: Button
    private lateinit var btnRegresar: Button
    private lateinit var btnSalir: Button
    private lateinit var btnSelecionar: Button
    private lateinit var btnLimpiar: Button
    private lateinit var btnModificar: Button

    // Checkboxes de especialidades
    private lateinit var chCardiologia: CheckBox
    private lateinit var chNeumologia: CheckBox
    private lateinit var chTraumatologia: CheckBox
    private lateinit var chEndocrinologia: CheckBox
    private lateinit var chOncologia: CheckBox
    private lateinit var chHematologia: CheckBox
    private lateinit var rvHospitales: RecyclerView
    private val listaHospitales = ArrayList<Hospital>()
    private lateinit var adapter: HospitalAdapter

    private lateinit var imgHospital: ImageView

    // guarda la imagen previa del hospital cargado
    private var mBitmap: Bitmap? = null
    private var imagenBase64Original: String? = null

    // para evitar recarga visual después de modificar
    private var evitarRecargaFormulario = false

    // *** NUEVO: GUARDA LA KEY DEL HOSPITAL SELECCIONADO ***
    private var keySeleccionada: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form)

        //llenado de 10 hospitales por unica vez
        //primero inicializamos la base de datos
        database = FirebaseDatabase.getInstance().reference.child("hospital")

        // sharedPreferences para controlar si ya se insertaron los hospitales
        val prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE)
        val yaInsertado = prefs.getBoolean("hospitales_cargados", false)

        // ejecutar solo si NO se ha cargado antes
        database.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (!yaInsertado) {
                    llenarDiezHospitales()
                    prefs.edit().putBoolean("hospitales_cargados", true).apply()
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) { }
        })

        // enlace a las cajas de texto en el layout activity_form
        etNombre = findViewById(R.id.etNombre)
        etDireccion = findViewById(R.id.etDireccion)
        etLatitud = findViewById(R.id.etLatitud)
        etLongitud = findViewById(R.id.etLongitud)

        // enlace de los checkboxes en el layout activity_form
        chCardiologia = findViewById(R.id.chCardiologia)
        chNeumologia = findViewById(R.id.chNeumologia)
        chTraumatologia = findViewById(R.id.chTraumatologia)
        chEndocrinologia = findViewById(R.id.chEndocrinologia)
        chOncologia = findViewById(R.id.chOncologia)
        chHematologia = findViewById(R.id.chHematologia)

        // enlace a los botones en el loyout activity_form
        spinnerGenero = findViewById(R.id.spinnerGenero)
        btnAgregar = findViewById(R.id.btnAgregar)
        btnRegresar = findViewById(R.id.btnRegresar)
        btnSalir = findViewById(R.id.btnSalir)
        btnLimpiar = findViewById(R.id.btnLimpiar)
        btnModificar = findViewById(R.id.btnModificar)


        // para la seleccion de la imagen del hospital
        btnSelecionar = findViewById(R.id.btnSelecionar)
        imgHospital = findViewById(R.id.imgHospital)

        // ListView y adaptador
        rvHospitales = findViewById(R.id.rvHospitales)
        rvHospitales.layoutManager = LinearLayoutManager(this)

        adapter = HospitalAdapter(
            listaHospitales,
            onItemClick = { hospitalSeleccionado ->
                cargarHospitalEnFormulario(hospitalSeleccionado)
            },
            onItemLongClick = { hospitalAEliminar ->
                eliminarHospital(hospitalAEliminar)
            }
        )
        rvHospitales.adapter = adapter

        // launcher para escoger imagen desde la galeria
        val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val selectedImage: Uri? = result.data?.data
                selectedImage?.let {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                    mBitmap = bitmap
                    imgHospital.setImageBitmap(bitmap)
                    // si el usuario selecciona nueva imagen, ya no queremos usar la imagen previa
                    imagenBase64Original = null
                }
            }
        }

        // boton de cargar imagen desde la galeria del movil
        btnSelecionar.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(galleryIntent)
        }

        // Listener Firebase (carga hospitales)
        cargarHospitalesFirebase()

        // guarda hospital nuevo
        btnAgregar.setOnClickListener {
            guardarHospital()
        }

        // boton regresar a pantalla inicial
        btnRegresar.setOnClickListener {
            finish()
        }

        // boton salir del sistema
        btnSalir.setOnClickListener {
            finishAffinity()
        }

        //boton borrar campos
        btnLimpiar.setOnClickListener {
            limpiarFormulario()
            Toast.makeText(this, "Formulario limpiado", Toast.LENGTH_SHORT).show()
        }

        //boton que modifica formulario
        btnModificar.setOnClickListener {
            modificarHospital()
        }
    }

    // redimensionar imagenes grandres
    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int = 600): Bitmap {   // CAMBIO AQUÍ
        val width = bitmap.width
        val height = bitmap.height

        val ratio: Float = width.toFloat() / height.toFloat()
        var newWidth = maxSize
        var newHeight = maxSize

        if (ratio > 1) {
            newHeight = (newWidth / ratio).toInt()
        } else {
            newWidth = (newHeight * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    // funcion para pasar a base64 una imagen
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val resized = resizeBitmap(bitmap, 600)
        val baos = java.io.ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        return android.util.Base64.encodeToString(
            baos.toByteArray(),
            android.util.Base64.DEFAULT
        )
    }
    private fun modificarHospital() {

        if (keySeleccionada == null) {
            Toast.makeText(this, "Seleccione un hospital antes de modificar", Toast.LENGTH_SHORT).show()
            return
        }

        val nombre = etNombre.text.toString().trim()
        val direccion = etDireccion.text.toString().trim()
        val horario = spinnerGenero.selectedItem.toString().trim()
        val latitudStr = etLatitud.text.toString().trim()
        val longitudStr = etLongitud.text.toString().trim()

        // Validación de campos vacíos
        if (nombre.isEmpty() || direccion.isEmpty() || horario.isEmpty() ||
            latitudStr.isEmpty() || longitudStr.isEmpty()) {

            Toast.makeText(this, "Faltan datos por llenar antes de modificar", Toast.LENGTH_SHORT).show()
            return
        }

        val especialidades = mutableListOf<String>()
        if (chCardiologia.isChecked) especialidades.add("Cardiología")
        if (chNeumologia.isChecked) especialidades.add("Neumología")
        if (chTraumatologia.isChecked) especialidades.add("Traumatología")
        if (chEndocrinologia.isChecked) especialidades.add("Endocrinología")
        if (chOncologia.isChecked) especialidades.add("Oncología")
        if (chHematologia.isChecked) especialidades.add("Hematología")
        if (especialidades.isEmpty()) especialidades.add("-")

        val latitud = latitudStr.toDouble()
        val longitud = longitudStr.toDouble()

        // usar la key del hospital seleccionado
        val imgBase64Final =
            if (mBitmap != null) bitmapToBase64(mBitmap!!)
            else imagenBase64Original ?: ""

        val datosActualizados = mapOf(
            "nombre" to nombre,
            "direccion" to direccion,
            "horario_atencion" to horario,
            "latitud" to latitud,
            "longitud" to longitud,
            "especialidades" to especialidades,
            "imgBase64" to imgBase64Final
        )

        //  actualizar solo usando la keySeleccionada
        database.child(keySeleccionada!!).updateChildren(datosActualizados)
            .addOnSuccessListener {
                evitarRecargaFormulario = false
                Toast.makeText(this, "Hospital modificado", Toast.LENGTH_SHORT).show()


                // vuelve a leer el hospital desde Firebase
                database.child(keySeleccionada!!)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val h = snapshot.getValue(Hospital::class.java)
                            if (h != null) {
                                h.key = snapshot.key
                                cargarHospitalEnFormulario(h) // recarga de datos
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                limpiarFormulario()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al modificar", Toast.LENGTH_SHORT).show()
            }
    }


    private fun limpiarFormulario() {

        // Limpiar EditText
        etNombre.setText("")
        etDireccion.setText("")
        etLatitud.setText("")
        etLongitud.setText("")

        // Reiniciar Spinner al primer ítem
        spinnerGenero.setSelection(0)

        // Desmarcar CheckBoxes
        chCardiologia.isChecked = false
        chNeumologia.isChecked = false
        chTraumatologia.isChecked = false
        chEndocrinologia.isChecked = false
        chOncologia.isChecked = false
        chHematologia.isChecked = false

        // Reiniciar imagen a la predeterminada
        imgHospital.setImageResource(android.R.drawable.ic_menu_mylocation)
        mBitmap = null
        imagenBase64Original = null
        keySeleccionada = null
    }

    private fun cargarHospitalEnFormulario(h: Hospital) {

        mBitmap = null
        imagenBase64Original = h.imgBase64
        keySeleccionada = h.key

        // Cargar datos básicos
        etNombre.setText(h.nombre)
        etDireccion.setText(h.direccion)
        etLatitud.setText(h.latitud.toString())
        etLongitud.setText(h.longitud.toString())

        // Cargar horario en spinner
        val index = resources.getStringArray(R.array.horario_atencion)
            .indexOf(h.horario_atencion)
        if (index >= 0) spinnerGenero.setSelection(index)

        // Resetear checkboxes
        chCardiologia.isChecked = false
        chNeumologia.isChecked = false
        chTraumatologia.isChecked = false
        chEndocrinologia.isChecked = false
        chOncologia.isChecked = false
        chHematologia.isChecked = false

        // Activar especialidades
        for (esp in h.especialidades) {
            when (esp) {
                "Cardiología" -> chCardiologia.isChecked = true
                "Neumología" -> chNeumologia.isChecked = true
                "Traumatología" -> chTraumatologia.isChecked = true
                "Endocrinología" -> chEndocrinologia.isChecked = true
                "Oncología" -> chOncologia.isChecked = true
                "Hematología" -> chHematologia.isChecked = true
            }
        }
        // carga imagen Base64 si existe
        if (!h.imgBase64.isNullOrEmpty()) {
            try {
                val bytes = android.util.Base64.decode(h.imgBase64, android.util.Base64.DEFAULT)
                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imgHospital.setImageBitmap(bmp)
                mBitmap = bmp
                imagenBase64Original = h.imgBase64
            } catch (e: Exception) {
                imgHospital.setImageResource(android.R.drawable.ic_menu_mylocation)
            }
        } else {
            imgHospital.setImageResource(android.R.drawable.ic_menu_mylocation)
        }
        Toast.makeText(this, "Seleccionado: ${h.nombre}", Toast.LENGTH_SHORT).show()
    }

    private fun llenarDiezHospitales() {
        val hospitalEjemplo = listOf(
            Hospital("Mañana", "Clínica Internacional - San Borja", -12.092394130759027, -77.00877581174444, "San Borja", especialidades = listOf("Cardiología", "Pediatría")),
            Hospital("Tarde", "Clínica Ricardo Palma", -12.090271538191187, -77.01827168970503, "San Isidro", especialidades = listOf("Oncología", "Ginecología")),
            Hospital("Mañana", "Clínica Anglo Americana", -12.108902937825892, -77.04021317277648, "San Isidro", especialidades = listOf("Traumatología", "Dermatología")),
            Hospital("Tarde", "Clínica Jesús del Norte", -11.989277075616334, -77.05855272291802, "Independencia", especialidades = listOf("Cardiología", "Neumología")),
            Hospital("Mañana", "Clínica Good Hope", -12.125542036680015, -77.03430103498826, "Miraflores", especialidades = listOf("Pediatría", "Ginecología")),
            Hospital("Tarde", "Clínica San Gabriel", -12.076312926328379, -77.09383232819557, "San Miguel", especialidades = listOf("Dermatología", "Traumatología")),
            Hospital("Tarde", "Clínica Maison de Santé", -12.057277629542211, -77.0333867750317, "Lima", especialidades = listOf("Cardiología", "Oncología")),
            Hospital("Mañana", "Clínica Cayetano Heredia", -12.023466984272142, -77.05610751631065, "San Martin de Porres", especialidades = listOf("Neumología", "Pediatría")),
            Hospital("Tarde", "Clinica Oncologica Y Del Riñon Cayetano Heredia", -12.019470374636995, -77.05306637630909, "Surco", especialidades = listOf("Cardiología", "Ginecología")),
            Hospital("Mañana", "Clínica Madrid de Sur", -12.024560949853615, -77.03924112321701, "Jesús María", especialidades = listOf("Dermatología"))
        )

        hospitalEjemplo.forEach { hospital ->
            val key = database.push().key
            if (key != null) {
                database.child(key).setValue(hospital)
            }
        }
    }


    // funcion que conecta la base de datos, lo lee y luego lo envia a ListView
    private fun cargarHospitalesFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                listaHospitales.clear()

                for (registro in snapshot.children) {
                    val h = registro.getValue(Hospital::class.java)
                    if (h != null) {
                        h.key = registro.key
                        listaHospitales.add(h)
                    }
                }

                adapter.notifyDataSetChanged()

            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    // funcion eliminar en Firebase con click largo
    private fun eliminarHospital(h: Hospital) {
        h.key?.let {
            database.child(it).removeValue()
        }
    }


    // funcion que guarda un hospital en la base de datos realtime
    private fun guardarHospital() {
        val nombre = etNombre.text.toString().trim()
        val direccion = etDireccion.text.toString().trim()
        val horario = spinnerGenero.selectedItem.toString().trim()
        val latitudStr = etLatitud.text.toString().trim()
        val longitudStr = etLongitud.text.toString().trim()

        if (nombre.isEmpty() || direccion.isEmpty() || horario.isEmpty() ||
            latitudStr.isEmpty() || longitudStr.isEmpty()) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        // recoge especiaidades selecionadad con los checkbox
        val especialidades = mutableListOf<String>()
        if (chCardiologia.isChecked) especialidades.add("Cardiología")
        if (chNeumologia.isChecked) especialidades.add("Neumología")
        if (chTraumatologia.isChecked) especialidades.add("Traumatología")
        if (chEndocrinologia.isChecked) especialidades.add("Endocrinología")
        if (chOncologia.isChecked) especialidades.add("Oncología")
        if (chHematologia.isChecked) especialidades.add("Hematología")

        // si no hay ninguna selecion en el checkbox, se guardara con "-"
        if (especialidades.isEmpty()) {
            especialidades.add("-")
        }

        val latitud = latitudStr.toDouble()
        val longitud = longitudStr.toDouble()

        val imgBase64Final = if (mBitmap != null) bitmapToBase64(mBitmap!!) else ""

        val hospital = Hospital(
            horario_atencion = horario,
            nombre = nombre,
            latitud = latitud,
            longitud = longitud,
            direccion = direccion,
            especialidades = especialidades,
            imgBase64 = imgBase64Final
        )

        val key = database.push().key
        if (key != null) {
            database.child(key).setValue(hospital)
                .addOnSuccessListener {
                    Toast.makeText(this, "Hospital guardado", Toast.LENGTH_SHORT).show()
                    limpiarFormulario()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
        }

    }
}