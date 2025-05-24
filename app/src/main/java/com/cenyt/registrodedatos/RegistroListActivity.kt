package com.cenyt.registrodedatos

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp


class RegistroListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch {
            val registros = db.registroDao().obtenerTodos()
            setContent {
                RegistroListScreen(registros = registros) {
                    finish() // 🔙 Esto cierra la pantalla y vuelve a MainActivity
                }
            }
        }
    }
}


@Composable
fun RegistroListScreen(registros: List<Registro>, onVolver: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        // Lista de registros
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(registros) { registro ->
                RegistroItem(registro)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Botón de volver al final
        Button(
            onClick = onVolver,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Volver")
        }
    }
}



@Composable
fun RegistroItem(registro: Registro) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Nombre: ${registro.nombre}")
            Text(text = "Turno: ${registro.turno}")
            Text(text = "Ubicación: ${registro.ubicacion}")
            Text(text = "Observaciones: ${registro.observaciones}")
            registro.latitud?.let { Text("Latitud: $it") }
            registro.longitud?.let { Text("Longitud: $it") }

            registro.fotoPath?.let {
                val imageBitmap = remember(it) {
                    BitmapFactory.decodeFile(it)?.asImageBitmap()
                }
                imageBitmap?.let { img ->
                    Image(
                        bitmap = img,
                        contentDescription = "Imagen adjunta",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(top = 8.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}


