package com.cenyt.registrodedatos
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import java.io.File
import java.util.Calendar
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import coil.compose.AsyncImage

class RegistroListActivity : ComponentActivity() {

    // --- PASO 1: Mover el estado aquí ---
    private val registrosState = mutableStateOf<List<Registro>>(emptyList())
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = AppDatabase.getDatabase(this)

        // Ahora solo llamamos a la función de carga la primera vez
        cargarRegistros()

        setContent {
            // El setContent ahora es más limpio
            RegistroListScreen(registros = registrosState.value) {
                finish()
            }
        }
    }

    // --- PASO 3 (Parte A): Sobrescribir onResume ---
    override fun onResume() {
        super.onResume()
        // Cada vez que volvemos a la pantalla, recargamos los datos
        cargarRegistros()
    }

    // --- PASO 2: Crear la función de carga ---
    private fun cargarRegistros() {
        lifecycleScope.launch {
            registrosState.value = db.registroDao().obtenerTodos()
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

            // CORRECCIÓN DE BUG: Añadimos un chequeo de nulidad para evitar crashes
            val coincideFecha = selectedDate.isBlank() || registro.fechaHora?.startsWith(selectedDate) == true

            coincideTexto && coincideFecha
        }
    }

    val calendar = Calendar.getInstance()
    val datePicker = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD), // Azul muy claro
                        Color(0xFF1F6FE1)  // Azul un poco más oscuro
                    )
                )
            )
            .padding(16.dp)
    ) {
        // --- NUEVA TARJETA PARA LOS CONTROLES ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Campo de búsqueda (movido aquí dentro)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar por nombre, área o circuito") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // Botones de fecha (movidos aquí dentro)
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
            }
        } // --- FIN DE LA NUEVA TARJETA ---

        Spacer(modifier = Modifier.height(16.dp)) // Aumentamos el espacio

        Text("Mostrando ${registrosFiltrados.size} de ${registros.size} registros", modifier = Modifier.padding(vertical = 4.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 0.dp) // Quitamos el padding de aquí para que las tarjetas lleguen a los bordes del padding principal
        ) {
            items(registrosFiltrados) { registro ->
                RegistroItem(
                    registro = registro,
                    onEditarClick = {
                        val intent = Intent(context, RegistroFormActivity::class.java).apply {
                            putExtra("REGISTRO_ID", registro.id)
                        }
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp)) // Espacio entre tarjetas de la lista
            }

            item {
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
    }
}



@Composable
fun RegistroItem(registro: Registro, onEditarClick: () -> Unit) {
    // Necesitarás este import:
    // import androidx.compose.material3.CardDefaults

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // <-- AÑADE ESTO
    ) {
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

            // Primero, preparamos la lista de rutas de fotos
            val photoPaths = remember(registro.fotoPath) {
                registro.fotoPath?.split(";")?.filter { it.isNotBlank() } ?: emptyList()
            }

            // Si hay fotos, mostramos el carrusel horizontal
            if (photoPaths.isNotEmpty()) {
                // Necesitarás estos imports:
                // import androidx.compose.foundation.lazy.LazyRow
                // import androidx.compose.foundation.lazy.items
                // import androidx.compose.foundation.shape.RoundedCornerShape
                // import androidx.compose.ui.draw.clip
                // import androidx.compose.foundation.layout.size

                LazyRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(photoPaths) { path ->
                        // Cargamos el bitmap de la imagen
                        // Necesitarás este import:
                        // import coil.compose.AsyncImage

                        // Coil se encarga de todo
                        AsyncImage(
                            model = File(path),
                            contentDescription = "Miniatura de imagen adjunta",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp)) // Un poco de espacio extra

            Button(
                onClick = onEditarClick ,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("✏️ Editar")
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
        writer.write(
            "fecha;nombre;area;circuito;estructura;lat;long;observaciones;" +
                    "apoyoTipo;apoyoCantidad;configuracion;disposicion;altura;característicasPlaca;avifauna;avifaunaEquipos;" +
                    "crucetaSup;crucetaInf;bayonetaTipo;bayonetaObservaciones;" +
                    "templeteCant;templeteAvifauna;aisladorTipo;aisladorA;aisladorB;aisladorC;" +
                    "dpsA;dpsB;dpsC;seccionador;equiposAdicionales;" +
                    "sptBajante;sptConexion;sptCantidad;sptEstado;fotoPath\n"
        )

        registros.forEach { r ->
            val fotoDestinoPath = r.fotoPath?.split(";")
                ?.filter { it.isNotBlank() }?.joinToString(";") { ruta ->
                    val origen = File(ruta)
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
                    ?.replace(
                        "\"",
                        "'"
                    )  // reemplaza comillas dobles por simples para evitar romper formato
                    ?.replace("\n", " ")  // elimina saltos de línea
                    ?.replace("\r", "")   // elimina retornos de carro
                    ?: ""
            }

            writer.write("$fila\n")
        }
    }
    Toast.makeText(/* context = */ context, /* text = */
        "Exportado en ${csvFile.absolutePath}", /* duration = */
        Toast.LENGTH_LONG
    ).show()

}
