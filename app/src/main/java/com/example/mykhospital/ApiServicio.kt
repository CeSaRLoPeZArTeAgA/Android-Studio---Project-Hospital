package com.example.mykhospital

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("/v2/directions/driving-car")//con vehiculo
    //@GET("/v2/directions/foot-walking")//caminando
    suspend fun getRoute(
        @Query("api_key") key: String,
        @Query("start", encoded = true) start: String,
        @Query("end", encoded = true) end: String
    ):Response<RouteResponse>
}