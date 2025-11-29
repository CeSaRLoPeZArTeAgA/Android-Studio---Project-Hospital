package com.example.mykhospital

import com.google.android.gms.maps.model.LatLng

class Posiciones {
    lateinit var posicion:LatLng
    lateinit var direccion: String
    var distancia: Double = 0.0
    var yo:Boolean = false
}