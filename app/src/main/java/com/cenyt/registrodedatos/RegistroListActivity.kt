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
import coil.compose.rememberAsyncImagePainter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import java.io.File

class RegistroListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch {
            val registros = db.registroDao().obtenerTodos()
            setContent {
                RegistroListScreen(registros = registros) {
                    finish()
                }
            }
        }
    }
}

@Composable
fun RegistroListScreen(registros: List<Registro>, onVolver: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(registros) { registro ->
                RegistroItem(registro)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Column {
            Button(
                onClick = { exportarRegistrosCSV(context, registros) },
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
            Text("Tipo norma: ${registro.tipoNorma ?: "-"}")
            Text("Distancia: ${registro.distancia ?: "-"}")
            Text("Resistencia: ${registro.resistencia ?: "-"}")
            Text("Avifauna: ${if (registro.avifaunaEstructura) "Sí" else "No"}")

            Text("Cruceta superior: ${registro.crucetaSuperior ?: "-"}")
            Text("Cruceta inferior tipo: ${registro.crucetaInferiorTipo ?: "-"}")

            Text("Bayoneta izquierda: ${registro.bayonetaIzquierda ?: "-"}")
            Text("Bayoneta derecha: ${registro.bayonetaDerecha ?: "-"}")

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

            Text("Amortiguador atrás: ${registro.amortiguadorAtras ?: "-"}")
            Text("Amortiguador adelante: ${registro.amortiguadorAdelante ?: "-"}")

            Text("SPT Bajante: ${registro.sptBajante ?: "-"}")
            Text("SPT Conexión: ${registro.sptConexion ?: "-"}")
            Text("SPT: ${registro.sptCantidad ?: "-"}")
            Text("SPT Estado: ${registro.sptEstado ?: "-"}")
            Text("Medición R: ${if (registro.medicionR) "Sí" else "No"}")
            Text("Medición P: ${if (registro.medicionP) "Sí" else "No"}")

            registro.fotoPath?.let { path ->
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
                            .height(200.dp),
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
        // Encabezado completo
        writer.write("fecha,nombre,area,circuito,estructura,lat,long,observaciones," +
                "apoyoTipo,apoyoCantidad,tipoNorma,distancia,resistencia,avifauna," +
                "crucetaSup,crucetaInf,bayonetaIzq,bayonetaDer," +
                "templeteCant,templeteAvifauna,aisladorTipo,aisladorA,aisladorB,aisladorC," +
                "dpsA,dpsB,dpsC,seccionador,amortAtras,amortAdelante," +
                "sptBajante,sptConexion,sptCantidad,sptEstado,medR,medP,fotoPath\n")

        registros.forEach { r ->
            val fotoDestinoPath = r.fotoPath?.let {
                val origen = File(it)
                if (origen.exists()) {
                    val destino = File(exportDir, origen.name)
                    origen.copyTo(destino, overwrite = true)
                    destino.absolutePath
                } else ""
            } ?: ""

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
                r.tipoNorma,
                r.distancia,
                r.resistencia,
                if (r.avifaunaEstructura) "Sí" else "No",
                r.crucetaSuperior,
                r.crucetaInferiorTipo,
                r.bayonetaIzquierda,
                r.bayonetaDerecha,
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
                r.amortiguadorAtras,
                r.amortiguadorAdelante,
                r.sptBajante,
                r.sptConexion,
                r.sptCantidad,
                r.sptEstado,
                if (r.medicionR) "Sí" else "No",
                if (r.medicionP) "Sí" else "No",
                fotoDestinoPath
            ).joinToString(",")

            writer.write("$fila\n")
        }
    }

    Toast.makeText(context, "Exportado en ${csvFile.absolutePath}", Toast.LENGTH_LONG).show()
}
