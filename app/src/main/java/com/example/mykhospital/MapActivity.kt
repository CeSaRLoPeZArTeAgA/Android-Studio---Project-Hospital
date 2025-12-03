package com.example.mykhospital

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.*

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.widget.Toast
import androidx.core.app.ActivityCompat

// Google Maps
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.RoundCap
import com.google.android.gms.maps.model.*

// GPS
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

// Retrofit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.*

// base de datos
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
class MapActivity : AppCompatActivity(),OnMapReadyCallback {

    private lateinit var btnRegresar: Button
    private lateinit var btnCalcular: Button
    private lateinit var btnSalir: Button
    private lateinit var gmap: GoogleMap  // mapa listo para usar

    private lateinit var database: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    lateinit var Miposicion: LatLng  // mi posicion GPS

    private var UniPosicion = LatLng(-12.061638, -76.977782)  // Puerta 5 UNI

    // Polilíneas independientes
    private var polyUNI: Polyline? = null
    private var polyHospital: Polyline? = null  //para manejo de la distancia de mi GPS al hospital mas cercano

    // lista donde guardamos hospitales desde Firebase y se usara para los calculos
    private val listaHospitales = mutableListOf<Hospital>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Firebase
        database = FirebaseDatabase.getInstance().reference.child("hospital")

        // inicializar mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //boton para regresar a la pantalla principal
        btnRegresar = findViewById<Button>(R.id.btnRegresar)
        btnRegresar.setOnClickListener {
            finish()
        }

        //boton para calcular la ruta al hospital mas cerca
        btnCalcular = findViewById<Button>(R.id.btnCalcular)
        btnCalcular.setOnClickListener {
            // Limpia polilíneas previas
            polyUNI?.remove()
            polyHospital?.remove()
            calcularRutaMasCercana()
            calcularDistanciaUNI()
        }

        //boton salir del sistema
        btnSalir = findViewById<Button>(R.id.btnSalir)
        btnSalir.setOnClickListener {
            finishAffinity()
        }
    }

    // aqui ya podemos trabajar con el mapa - metodo obligatorio
    override fun onMapReady(googleMap: GoogleMap) {
        gmap = googleMap
        obtenerMiUbicacion()
        // cargar hospitales desde Firebase
        cargarHospitalesEnMapa()
    }

    //  carga los marcadores y llena listaHospitales
    private fun cargarHospitalesEnMapa() {

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                listaHospitales.clear() // evitar duplicados

                for (registro in snapshot.children) {

                    val h = registro.getValue(Hospital::class.java)

                    if (h != null) {

                        // se agrega a la lista real
                        listaHospitales.add(h)

                        // dibuja marcador
                        val pos = LatLng(h.latitud, h.longitud)

                        gmap.addMarker(
                            MarkerOptions()
                                .position(pos)
                                .title(h.nombre)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        )
                    }
                }

                Toast.makeText(
                    this@MapActivity,
                    "Hospitales cargados: ${listaHospitales.size}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // obtener mi posicion GPS
    private fun obtenerMiUbicacion() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 10
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {

                Miposicion = LatLng(location.latitude, location.longitude)

                // Marcador de MI GPS
                gmap.addMarker(
                    MarkerOptions()
                        .position(Miposicion)
                        .title("Mi ubicación")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )

                // Marcador UNI
                gmap.addMarker(
                    MarkerOptions()
                        .position(UniPosicion)
                        .title("Puerta 5 UNI")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )

                gmap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(Miposicion, 14f),
                    2000,
                    null
                )
            }
        }
    }

     // funcion para obtener la ruta entre dos puntos
    private suspend fun obtenerRutaEntrePuntos(destino: LatLng): Pair<Double, List<LatLng>?> {

        val start = "${Miposicion.longitude},${Miposicion.latitude}"
        val end = "${destino.longitude},${destino.latitude}"

        val api = getRetrofit().create(ApiService::class.java)
        val call = api.getRoute(
            "5b3ce3597851110001cf624826138ef006c74296adca414c449bc21e",
            start, end
        )

        return if (call.isSuccessful) {

            val feature = call.body()?.features?.firstOrNull()

            val dist = feature?.properties?.summary?.distance ?: 1e12
            val coords = feature?.geometry?.coordinates

            Pair(dist, coords?.map { LatLng(it[1], it[0]) })

        } else {
            Pair(1e12, null)
        }
    }


    //  calculo de la ruta minima a la UNI
    private fun calcularDistanciaUNI() {

        CoroutineScope(Dispatchers.IO).launch {

            val (distUNI, rutaUNI) = obtenerRutaEntrePuntos(UniPosicion)

            if (rutaUNI == null) {
                runOnUiThread {
                    Toast.makeText(
                        this@MapActivity,
                        "No se pudo obtener ruta hacia la UNI",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@launch
            }

            runOnUiThread {

                // borrar polilinea previa
                polyUNI?.remove()

                // dibujar nueva polilinea
                val polyLineOptions = PolylineOptions()
                    .width(12f)
                    .color(Color.RED)
                    .startCap(RoundCap())
                    .endCap(RoundCap())
                    .jointType(JointType.ROUND)

                rutaUNI.forEach { polyLineOptions.add(it) }

                polyUNI  = gmap.addPolyline(polyLineOptions)

                Toast.makeText(
                    this@MapActivity,
                    "Distancia a la UNI: ${"%.2f".format(distUNI / 1000)} km",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // ruta al hospital mas cercana (ruta real)
    private fun calcularRutaMasCercana() {

        CoroutineScope(Dispatchers.IO).launch {

            if (listaHospitales.isEmpty()) {
                runOnUiThread {
                    Toast.makeText(
                        this@MapActivity,
                        "No hay hospitales cargados",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@launch
            }

            var hospitalGanador: Hospital? = null
            var mejorDist = 1e12
            var mejorRuta: List<LatLng>? = null

            for (h in listaHospitales) {

                val (dist, ruta) = obtenerRutaEntrePuntos(h.getLatLng())

                if (ruta != null && dist < mejorDist) {
                    mejorDist = dist
                    mejorRuta = ruta
                    hospitalGanador = h
                }
            }

            if (hospitalGanador == null || mejorRuta == null) {
                runOnUiThread {
                    Toast.makeText(
                        this@MapActivity,
                        "No se pudo calcular rutas",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@launch
            }

            // Dibujar ruta
            runOnUiThread {

                polyHospital?.remove()

                val polyLineOptions = PolylineOptions()
                    .width(12f)
                    .color(Color.BLUE)
                    .startCap(RoundCap())
                    .endCap(RoundCap())
                    .jointType(JointType.ROUND)

                mejorRuta!!.forEach { polyLineOptions.add(it) }

                polyHospital = gmap.addPolyline(polyLineOptions)

                Toast.makeText(
                    this@MapActivity,
                    "Hospital más cercano: ${hospitalGanador.nombre}\n" +
                            "Distancia: ${"%.2f".format(mejorDist / 1000)} km",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openrouteservice.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

}