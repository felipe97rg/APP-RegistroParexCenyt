package com.cenyt.registrodedatos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registros")
data class Registro(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    // DATOS DEL SITIO
    val fechaHora: String,
    val nombreResponsable: String,
    val area: String,
    val circuito: String,
    val estructuraNumero: String,
    val latitud: String?,
    val longitud: String?,
    val observaciones: String,

    // ESTADO DE LOS ELEMENTOS
    val apoyoTipo: String?,
    val apoyoCantidad: Int?,
    val configuracion: String?, // nueva propiedad
    val disposicion: String?,  // nueva propiedad

    val altura: Int?,
    val característicasPlaca: Int?,

    val avifaunaEstructura: Boolean,
    val avifaunaEquipos: String?,

    val crucetaSuperior: Int?,
    val crucetaInferiorTipo: String?,

    val bayonetaTipo: String?,
    val bayonetaObservaciones: String?,

    val templeteCantidad: Int?,
    val templeteAvifauna: Int?,

    val aisladorTipo: String?,
    val aisladorA: Int?,
    val aisladorB: Int?,
    val aisladorC: Int?,

    val dpsA: Int?,
    val dpsB: Int?,
    val dpsC: Int?,

    val seccionador: Boolean,

    val equiposAdicionales: String?,

    val sptBajante: Int?,
    val sptConexion: Int?,
    val sptCantidad: Int?,
    val sptEstado: Int?,
    // val medicionR: Boolean,
    // val medicionP: Boolean,

    val fotoPath: String?
)