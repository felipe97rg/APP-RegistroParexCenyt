package com.cenyt.registrodedatos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update // <-- Importante

@Dao
interface RegistroDao {
    @Insert
    suspend fun insertar(registro: Registro)

    @Query("SELECT * FROM registros ORDER BY id DESC")
    suspend fun obtenerTodos(): List<Registro>

    @Query("SELECT * FROM registros WHERE id = :id")
    suspend fun obtenerPorId(id: Int): Registro?

    @Update
    suspend fun actualizar(registro: Registro) // <-- Nuestra nueva función
}