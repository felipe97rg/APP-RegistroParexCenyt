package com.cenyt.registrodedatos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RegistroDao {
    @Insert
    suspend fun insertar(registro: Registro)

    @Query("SELECT * FROM registros ORDER BY id DESC")
    suspend fun obtenerTodos(): List<Registro>
}