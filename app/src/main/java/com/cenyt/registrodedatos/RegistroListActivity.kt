package com.cenyt.registrodedatos

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import android.app.DatePickerDialog
import java.util.*


import androidx.compose.runtime.*

import coil.compose.rememberAsyncImagePainter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.material3.OutlinedTextField
import java.io.File

class RegistroListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(this)

        setContent {
            val registrosState = remember { mutableStateOf<List<Registro>>(emptyList()) }

            // Carga asincrónica
            LaunchedEffect(Unit) {
                registrosState.value = db.registroDao().obtenerTodos()
            }

            RegistroListScreen(registros = registrosState.value) {
                finish()
            }
        }
    }






@Composable
fun RegistroListScreen(registros: List<Registro>, onVolver: () -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }

    val registrosFiltrados = remember(query, selectedDate, registros) {
        registros.filter { registro ->
            val coincideTexto = query.isBlank() || listOfNotNull(
                registro.estructuraNumero,
                registro.area,
                registro.circuito,
                registro.avifaunaEquipos,
                registro.disposicion,
                registro.configuracion,
                registro.observaciones
            ).any { it.contains(query, ignoreCase = true) }

            val coincideFecha = selectedDate.isBlank() || registro.fechaHora?.startsWith(selectedDate) == true

            coincideTexto && coincideFecha
        }
    }

    // Picker de fecha
    val calendar = Calendar.getInstance()
    val datePicker = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth) // formato yyyy-MM-dd
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Buscar por nombre, área o circuito") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(
                onClick = { datePicker.show() },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (selectedDate.isBlank()) "📅 Filtrar por fecha" else "Fecha: $selectedDate")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { selectedDate = "" },
                modifier = Modifier.weight(1f)
            ) {
                Text("❌ Limpiar fecha")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Mostrando ${registrosFiltrados.size} de ${registros.size} registros", modifier = Modifier.padding(vertical = 4.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(registrosFiltrados) { registro ->
                RegistroItem(registro)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Column {
            Button(
                onClick = { exportarRegistrosCSV(context, registrosFiltrados) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("📤 Exportar Registros")
            }

            Button(
                onClick = onVolver,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text("Volver")
            }
        }
    }
}



@Composable
fun RegistroItem(registro: Registro) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text("DATOS DEL SITIO", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Fecha y hora: ${registro.fechaHora}")
            Text("Nombre: ${registro.nombreResponsable}")
            Text("Área: ${registro.area}")
            Text("Circuito: ${registro.circuito}")
            Text("Estructura número: ${registro.estructuraNumero}")
            registro.latitud?.let { Text("Latitud: $it") }
            registro.longitud?.let { Text("Longitud: $it") }
            Text("Observaciones: ${registro.observaciones}")

            Spacer(modifier = Modifier.height(12.dp))
            Text("ESTADO DE LOS ELEMENTOS", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            Text("Apoyo Tipo: ${registro.apoyoTipo ?: "-"}")
            Text("Apoyo Cantidad: ${registro.apoyoCantidad ?: "-"}")
            Text("Disposicion: ${registro.disposicion ?: "-"}")
            Text("Configuracion : ${registro.configuracion ?: "-"}")
            Text("Altura: ${registro.altura ?: "-"}")
            Text("Características Placa: ${registro.característicasPlaca ?: "-"}")
            Text("Avifauna: ${if (registro.avifaunaEstructura) "Sí" else "No"}")
            Text("Equipos relacionados avifauna: ${registro.avifaunaEquipos ?: "-"}")

            Text("Cruceta superior: ${registro.crucetaSuperior ?: "-"}")
            Text("Cruceta inferior tipo: ${registro.crucetaInferiorTipo ?: "-"}")

            Text("Bayoneta Tipo: ${registro.bayonetaTipo ?: "-"}")
            Text("Bayoneta observaciones: ${registro.bayonetaObservaciones ?: "-"}")

            Text("Templete cantidad: ${registro.templeteCantidad ?: "-"}")
            Text("Templete avifauna: ${registro.templeteAvifauna ?: "-"}")

            Text("Aislador tipo: ${registro.aisladorTipo ?: "-"}")
            Text("Aislador A: ${registro.aisladorA ?: "-"}")
            Text("Aislador B: ${registro.aisladorB ?: "-"}")
            Text("Aislador C: ${registro.aisladorC ?: "-"}")

            Text("DPS A: ${registro.dpsA ?: "-"}")
            Text("DPS B: ${registro.dpsB ?: "-"}")
            Text("DPS C: ${registro.dpsC ?: "-"}")

            Text("Seccionador/Cortacircuito: ${if (registro.seccionador) "Sí" else "No"}")

            Text("Equipos Adicionales: ${registro.equiposAdicionales ?: "-"}")

            Text("SPT Bajante: ${registro.sptBajante ?: "-"}")
            Text("SPT Conexión: ${registro.sptConexion ?: "-"}")
            Text("SPT: ${registro.sptCantidad ?: "-"}")
            Text("SPT Estado: ${registro.sptEstado ?: "-"}")
            // Text("Medición R: ${if (registro.medicionR) "Sí" else "No"}")
            // Text("Medición P: ${if (registro.medicionP) "Sí" else "No"}")

            registro.fotoPath
                ?.split(";")
                ?.filter { it.isNotBlank() }
                ?.forEach { path ->
                    val imageBitmap = remember(path) {
                        try {
                            BitmapFactory.decodeFile(path)?.asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    imageBitmap?.let { img ->
                        Image(
                            bitmap = img,
                            contentDescription = "Imagen adjunta",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(vertical = 4.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
        }
    }
}

fun exportarRegistrosCSV(context: Context, registros: List<Registro>) {
    val exportDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "RegistrosExportados"
    )
    if (!exportDir.exists()) exportDir.mkdirs()

    val csvFile = File(exportDir, "registros_exportados.csv")

    csvFile.bufferedWriter().use { writer ->
        // Encabezado
        writer.write("fecha;nombre;area;circuito;estructura;lat;long;observaciones;" +
                "apoyoTipo;apoyoCantidad;configuracion;disposicion;altura;característicasPlaca;avifauna;avifaunaEquipos;" +
                "crucetaSup;crucetaInf;bayonetaTipo;bayonetaObservaciones;" +
                "templeteCant;templeteAvifauna;aisladorTipo;aisladorA;aisladorB;aisladorC;" +
                "dpsA;dpsB;dpsC;seccionador;equiposAdicionales;" +
                "sptBajante;sptConexion;sptCantidad;sptEstado;fotoPath\n")

        registros.forEach { r ->
            val fotoDestinoPath = r.fotoPath?.split(";")
                ?.filter { it.isNotBlank() }
                ?.map { ruta ->
                    val origen = File(ruta)
                    if (origen.exists()) {
                        val destino = File(exportDir, origen.name)
                        origen.copyTo(destino, overwrite = true)
                        destino.absolutePath
                    } else ""
                }?.joinToString(";") ?: ""

            val fila = listOf(
                r.fechaHora,
                r.nombreResponsable,
                r.area,
                r.circuito,
                r.estructuraNumero,
                r.latitud,
                r.longitud,
                r.observaciones,
                r.apoyoTipo,
                r.apoyoCantidad,
                r.configuracion,
                r.disposicion,
                r.altura,
                r.característicasPlaca,
                if (r.avifaunaEstructura) "Sí" else "No",
                r.avifaunaEquipos,
                r.crucetaSuperior,
                r.crucetaInferiorTipo,
                r.bayonetaTipo,
                r.bayonetaObservaciones,
                r.templeteCantidad,
                r.templeteAvifauna,
                r.aisladorTipo,
                r.aisladorA,
                r.aisladorB,
                r.aisladorC,
                r.dpsA,
                r.dpsB,
                r.dpsC,
                if (r.seccionador) "Sí" else "No",
                r.equiposAdicionales,
                r.sptBajante,
                r.sptConexion,
                r.sptCantidad,
                r.sptEstado,
                fotoDestinoPath
            ).joinToString(";") { campo ->
                campo?.toString()
                    ?.replace("\"", "'")  // reemplaza comillas dobles por simples para evitar romper formato
                    ?.replace("\n", " ")  // elimina saltos de línea
                    ?.replace("\r", "")   // elimina retornos de carro
                    ?: ""
            }

            writer.write("$fila\n")
        }
    }

    Toast.makeText(context, "Exportado en ${csvFile.absolutePath}", Toast.LENGTH_LONG).show()
}
}