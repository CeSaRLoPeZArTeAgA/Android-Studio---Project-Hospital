package com.example.mykhospital

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.Exclude

data class Hospital(
        val horario_atencion: String = "",
        val nombre: String = "",
        val latitud: Double = 0.0,
        val longitud: Double = 0.0,
        val direccion: String = "",
        val especialidades: List<String> = emptyList()
) {
        constructor() : this("", "", 0.0, 0.0, "",emptyList())

        // Clave de Firebase (no se guarda en la BD)
        @get:Exclude
        var key: String? = null

        @Exclude
        fun getLatLng(): LatLng {
            return LatLng(latitud, longitud)
        }
    }
