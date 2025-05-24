package com.cenyt.registrodedatos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registros")
data class Registro(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fechaHora: String,
    val nombreResponsable: String,
    val area: String,
    val circuito: String,
    val estructuraNumero: String,
    val latitud: String?,
    val longitud: String?,
    val observaciones: String,
    val fotoPath: String?
)


