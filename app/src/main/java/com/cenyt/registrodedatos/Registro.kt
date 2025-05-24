package com.cenyt.registrodedatos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registros")
data class Registro(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val turno: String,
    val ubicacion: String,
    val observaciones: String,
    val latitud: String? = null,
    val longitud: String? = null,
    val fotoPath: String? = null

)

