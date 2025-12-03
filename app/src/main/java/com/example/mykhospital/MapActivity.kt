package com.example.mykhospital

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
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

class MapActivity : AppCompatActivity(),OnMapReadyCallback {

    private lateinit var btnRegresar: Button
    private lateinit var btnCalcular: Button
    private lateinit var btnSalir: Button
    private lateinit var gmap: GoogleMap  // mapa listo para usar

    private lateinit var database: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    lateinit var Miposicion: LatLng  // mi posicion GPS

    private var UniPosicion = LatLng(-12.061638, -76.977782)  // Puerta 5 UNI

    var poly: Polyline? = null //para manejo de la distancia de mi GPS al hospital mas cercano

    //parametros geograficos
    private var Pi = 3.14159265375
    private var r = 6371.0 //radio de la tierra


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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
            poly?.remove()
            calcularRuta()
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
        // Aquí ya puedes colocar marcadores, zoom, etc.
    }

    // Obtener mi GPS real
    private fun obtenerMiUbicacion() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 10
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                Miposicion = LatLng(location.latitude, location.longitude)

                // Marker de MI POSICIÓN
                gmap.addMarker(
                    MarkerOptions()
                        .position(Miposicion)
                        .title("Mi posición")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )

                // Marker de UNI
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
            } else {
                Toast.makeText(this, "No se pudo obtener ubicación", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Cálculo de la ruta usando OpenRouteService
    private fun calcularRuta() {

        val start = "${Miposicion.longitude},${Miposicion.latitude}"
        val end = "${UniPosicion.longitude},${UniPosicion.latitude}"

        CoroutineScope(Dispatchers.IO).launch {

            try {
                val call = getRetrofit()
                    .create(ApiService::class.java)
                    .getRoute(
                        "5b3ce3597851110001cf624826138ef006c74296adca414c449bc21e",
                        start,
                        end
                    )

                if (call.isSuccessful) {
                    val route = call.body()
                    val coords = route?.features?.firstOrNull()?.geometry?.coordinates

                    if (coords != null) {
                        val polyLineOptions = PolylineOptions()
                            .width(12f)
                            .color(Color.BLUE)
                            .startCap(RoundCap())
                            .endCap(RoundCap())
                            .jointType(JointType.ROUND)

                        coords.forEach {
                            polyLineOptions.add(LatLng(it[1], it[0]))
                        }

                        runOnUiThread {
                            poly = gmap.addPolyline(polyLineOptions)

                            // Distancia real de carretera
                            val distKm =
                                route.features[0].properties.summary.distance / 1000.0

                            Toast.makeText(
                                this@MapActivity,
                                "Ruta hacia la UNI: ${"%.2f".format(distKm)} km",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MapActivity, "Error al calcular ruta", Toast.LENGTH_LONG).show()
                }
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