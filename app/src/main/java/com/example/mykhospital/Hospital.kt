package com.example.mykhospital

class Hospitak {
    var nombre:String = ""
    var posX: Double = 0.0
    var posY: Double = 0.0
    var idcontacto:Int = 0
    var key:String = ""
    var codigo:String = ""

    constructor(nombre: String, posX: Double,posY: Double, idcontacto: Int, key: String, codigo: String) {
        this.nombre = nombre
        this.posX = posX
        this.posY = posY
        this.idcontacto = idcontacto
        this.key = key
        this.codigo = codigo
    }
}