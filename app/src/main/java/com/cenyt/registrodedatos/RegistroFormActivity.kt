package com.cenyt.registrodedatos
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RegistroFormActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var currentPhotoPath: String? = null
    private val multiplePhotoPaths = mutableListOf<String>()
    private lateinit var fechaHoraActual: String
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_GALLERY = 2
    private val REQUEST_MULTIPLE_IMAGES = 3
    private var registroActual: Registro? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_form)

        db = AppDatabase.getDatabase(this)

        val registroId = intent.getIntExtra("REGISTRO_ID", -1)

        if (registroId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                registroActual = db.registroDao().obtenerPorId(registroId)
                runOnUiThread {
                    registroActual?.let { cargarDatosEnFormulario(it) }
                }
            }
        }

        // --- Configuración de Spinners ---
        val spinnerApoyoTipo = findViewById<Spinner>(R.id.spinnerApoyoTipo)
        val opcionesApoyo = listOf("P", "T", "Otro")
        spinnerApoyoTipo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opcionesApoyo)

        val spinnerConfiguracion = findViewById<Spinner>(R.id.spinnerConfiguracion)
        val opcionesConfiguracion = listOf("Retención", "Suspensión", "Pullover", "Otro")
        spinnerConfiguracion.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opcionesConfiguracion)

        val spinnerDisposicion = findViewById<Spinner>(R.id.spinnerDisposicion)
        val opcionesDisposicion = listOf("Canadiense", "Triangular", "Semibandera", "Otro")
        spinnerDisposicion.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opcionesDisposicion)

        val spinnerAisladores = findViewById<Spinner>(R.id.spinnerAisladores)
        val opcionesAisaladores = listOf("Aislador" ,"Cadenas de aisladores", "Pin", "Linepost", "Otro")
        spinnerAisladores.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opcionesAisaladores)

        val spinnerbayoneta = findViewById<Spinner>(R.id.spinnerbayoneta)
        val opcionesbayoneta = listOf("Sencilla" , "Doble")
        spinnerbayoneta.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opcionesbayoneta)

        // --- Configuración de otros elementos y Listeners ---
        val tvFechaHora = findViewById<TextView>(R.id.tvFechaHora)
        fechaHoraActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        tvFechaHora.text = "Fecha y hora: $fechaHoraActual"

        // LÓGICA RESTAURADA para el botón de adjuntar foto
        findViewById<Button>(R.id.btnAdjuntarFoto).setOnClickListener {
            val opciones = arrayOf("Tomar Foto", "Elegir desde Galería")
            AlertDialog.Builder(this)
                .setTitle("Selecciona una opción")
                .setItems(opciones) { _, which ->
                    when (which) {
                        0 -> abrirCamara()
                        1 -> abrirGaleria()
                    }
                }.show()
        }

        // LÓGICA RESTAURADA para adjuntar múltiples fotos
        findViewById<Button>(R.id.btnAdjuntarMultiplesFotos).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            startActivityForResult(Intent.createChooser(intent, "Selecciona imágenes"), REQUEST_MULTIPLE_IMAGES)
        }

        // LÓGICA RESTAURADA para el botón de ubicación (GPS)
        // LÓGICA MEJORADA para el botón de ubicación (GPS)
        findViewById<Button>(R.id.btnUbicacion).setOnClickListener {
            val btnUbicacion = it as Button // Obtenemos una referencia al botón mismo

            // PASO 1: Dar retroalimentación al usuario
            btnUbicacion.isEnabled = false // Desactivamos el botón para que no lo presionen de nuevo
            Toast.makeText(this, "Buscando ubicación...", Toast.LENGTH_SHORT).show()

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            // Verificamos los permisos como antes
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
                btnUbicacion.isEnabled = true // Lo volvemos a activar si no hay permisos
                return@setOnClickListener
            }

            // PASO 2: Usar getCurrentLocation en lugar de lastLocation
            fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    // PASO 3: Volver a activar el botón y mostrar el resultado
                    btnUbicacion.isEnabled = true
                    if (location != null) {
                        findViewById<EditText>(R.id.etLatitud).setText(location.latitude.toString())
                        findViewById<EditText>(R.id.etLongitud).setText(location.longitude.toString())
                    } else {
                        Toast.makeText(this, "No se pudo obtener la ubicación. Intenta de nuevo.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    // En caso de error, también reactivamos el botón
                    btnUbicacion.isEnabled = true
                    Toast.makeText(this, "Error al obtener la ubicación: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        // Listener del botón Guardar
        findViewById<Button>(R.id.btnGuardar).setOnClickListener {
            guardarRegistro()
        }
    }

    private fun abrirCamara() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
            return
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = createImageFile()

        val photoURI = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            photoFile
        )

        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }


    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_GALLERY)
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }


    private fun guardarRegistro() {
        val getText = { id: Int -> findViewById<EditText>(id).text.toString() }
        val getInt = { id: Int -> getText(id).toIntOrNull() }
        val getCheck = { id: Int -> findViewById<CheckBox>(id).isChecked }

        // Creamos un objeto con los datos actuales del formulario
        val datosFormulario = Registro(
            id = registroActual?.id ?: 0, // Mantenemos el ID original si estamos editando
            fechaHora = fechaHoraActual,
            nombreResponsable = getText(R.id.etNombre),
            area = getText(R.id.etArea),
            circuito = getText(R.id.etCircuito),
            estructuraNumero = getText(R.id.etEstructura),
            latitud = getText(R.id.etLatitud),
            longitud = getText(R.id.etLongitud),
            observaciones = getText(R.id.etObservaciones),
            apoyoTipo = findViewById<Spinner>(R.id.spinnerApoyoTipo).selectedItem.toString(),
            apoyoCantidad = getInt(R.id.etApoyoCantidad),
            configuracion = findViewById<Spinner>(R.id.spinnerConfiguracion).selectedItem.toString(),
            disposicion = findViewById<Spinner>(R.id.spinnerDisposicion).selectedItem.toString(),
            altura = getInt(R.id.etAltura),
            característicasPlaca = getInt(R.id.etcaracterísticasPlaca),
            avifaunaEstructura = getCheck(R.id.checkAvifaunaEstructura),
            avifaunaEquipos = getText(R.id.etavifaunaEquipos),
            crucetaSuperior = getInt(R.id.etCrucetaSuperior),
            crucetaInferiorTipo = getText(R.id.etCrucetaInferiorTipo),
            bayonetaTipo = findViewById<Spinner>(R.id.spinnerbayoneta).selectedItem.toString(),
            bayonetaObservaciones = getText(R.id.etbayonetaObservaciones),
            templeteCantidad = getInt(R.id.etTempleteCantidad),
            templeteAvifauna = getInt(R.id.etTempleteAvifauna),
            aisladorTipo = findViewById<Spinner>(R.id.spinnerAisladores).selectedItem.toString(),
            aisladorA = getInt(R.id.etAisladorA),
            aisladorB = getInt(R.id.etAisladorB),
            aisladorC = getInt(R.id.etAisladorC),
            dpsA = getInt(R.id.etDpsA),
            dpsB = getInt(R.id.etDpsB),
            dpsC = getInt(R.id.etDpsC),
            seccionador = getCheck(R.id.checkSeccionador),
            equiposAdicionales = getText(R.id.etequiposAdicionales),
            sptBajante = getInt(R.id.etSptBajante),
            sptConexion = getInt(R.id.etSptConexion),
            sptCantidad = getInt(R.id.etSptSpt),
            sptEstado = getInt(R.id.etSptEstado),
            fotoPath = multiplePhotoPaths.joinToString(";"),
        )

        // Lanzamos la corutina para la operación de base de datos
        CoroutineScope(Dispatchers.IO).launch {
            if (registroActual != null) {
                // MODO EDICIÓN: Usamos la función actualizar
                db.registroDao().actualizar(datosFormulario)
            } else {
                // MODO CREACIÓN: Usamos la función insertar
                db.registroDao().insertar(datosFormulario)
            }

            runOnUiThread {
                Toast.makeText(this@RegistroFormActivity, "Registro guardado exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val imageView = findViewById<ImageView>(R.id.imagePreview)
        val scrollView = findViewById<HorizontalScrollView>(R.id.multipleImagesScroll)
        val layout = findViewById<LinearLayout>(R.id.multipleImagesLayout)

        var nuevaRutaSeleccionada: String? = null

        // 📷 Foto tomada con la cámara
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val file = File(currentPhotoPath ?: return)
            multiplePhotoPaths.add(file.absolutePath)
            nuevaRutaSeleccionada = file.absolutePath
        }

        // 🖼 Imagen individual desde galería
        if (requestCode == REQUEST_IMAGE_GALLERY && resultCode == RESULT_OK && data != null) {
            data.data?.let { uri ->
                val inputStream = contentResolver.openInputStream(uri)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "IMG_${timeStamp}.jpg")
                inputStream?.copyTo(file.outputStream())
                inputStream?.close()
                multiplePhotoPaths.add(file.absolutePath)
                nuevaRutaSeleccionada = file.absolutePath
            }
        }

        // 📂 Múltiples imágenes desde galería
        if (requestCode == REQUEST_MULTIPLE_IMAGES && resultCode == RESULT_OK && data != null) {
            if (data.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val uri = data.clipData!!.getItemAt(i).uri
                    val inputStream = contentResolver.openInputStream(uri)
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "IMG_${timeStamp}_$i.jpg")
                    inputStream?.copyTo(file.outputStream())
                    inputStream?.close()
                    multiplePhotoPaths.add(file.absolutePath)
                    nuevaRutaSeleccionada = file.absolutePath
                }
            } else if (data.data != null) {
                val uri = data.data!!
                val inputStream = contentResolver.openInputStream(uri)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "IMG_${timeStamp}.jpg")
                inputStream?.copyTo(file.outputStream())
                inputStream?.close()
                multiplePhotoPaths.add(file.absolutePath)
                nuevaRutaSeleccionada = file.absolutePath
            }

            Toast.makeText(this, "${multiplePhotoPaths.size} imágenes totales", Toast.LENGTH_SHORT).show()
        }

        // 🖼 Mostrar la imagen principal y miniaturas si hay nuevas imágenes
        if (nuevaRutaSeleccionada != null && multiplePhotoPaths.isNotEmpty()) {
            renderMiniaturas()
        }
    }

    private fun renderMiniaturas() {
        val layout = findViewById<LinearLayout>(R.id.multipleImagesLayout)
        val scrollView = findViewById<HorizontalScrollView>(R.id.multipleImagesScroll)
        val imageView = findViewById<ImageView>(R.id.imagePreview)

        layout.removeAllViews()

        if (multiplePhotoPaths.isEmpty()) {
            scrollView.visibility = View.GONE
            imageView.visibility = View.GONE
            return
        }

        scrollView.visibility = View.VISIBLE
        imageView.visibility = View.VISIBLE

        // Mostrar la última imagen como principal
        imageView.setImageURI(Uri.fromFile(File(multiplePhotoPaths.last())))

        multiplePhotoPaths.forEach { path ->
            val preview = ImageView(this)
            preview.setImageURI(Uri.fromFile(File(path)))
            val params = LinearLayout.LayoutParams(300, 300)
            params.setMargins(16, 0, 16, 0)
            preview.layoutParams = params
            preview.scaleType = ImageView.ScaleType.CENTER_CROP

            preview.setOnClickListener {
                imageView.setImageURI(Uri.fromFile(File(path)))
            }

            preview.setOnLongClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Eliminar imagen")
                    .setMessage("¿Deseas eliminar esta imagen del registro?")
                    .setPositiveButton("Sí") { _, _ ->
                        multiplePhotoPaths.remove(path)
                        File(path).delete()
                        renderMiniaturas()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
                true
            }

            layout.addView(preview)
        }
    }


    private fun cargarDatosEnFormulario(registro: Registro) {
        // --- Textos (EditText) ---
        findViewById<TextView>(R.id.tvFechaHora).text = "Fecha y hora: ${registro.fechaHora}"
        findViewById<EditText>(R.id.etNombre).setText(registro.nombreResponsable)
        findViewById<EditText>(R.id.etArea).setText(registro.area)
        findViewById<EditText>(R.id.etCircuito).setText(registro.circuito)
        findViewById<EditText>(R.id.etEstructura).setText(registro.estructuraNumero)
        findViewById<EditText>(R.id.etLatitud).setText(registro.latitud ?: "")
        findViewById<EditText>(R.id.etLongitud).setText(registro.longitud ?: "")
        findViewById<EditText>(R.id.etObservaciones).setText(registro.observaciones)

        // --- Números (EditText) ---
        findViewById<EditText>(R.id.etApoyoCantidad).setText(registro.apoyoCantidad?.toString() ?: "")
        findViewById<EditText>(R.id.etAltura).setText(registro.altura?.toString() ?: "")
        findViewById<EditText>(R.id.etcaracterísticasPlaca).setText(registro.característicasPlaca?.toString() ?: "")
        findViewById<EditText>(R.id.etavifaunaEquipos).setText(registro.avifaunaEquipos ?: "")
        findViewById<EditText>(R.id.etCrucetaSuperior).setText(registro.crucetaSuperior?.toString() ?: "")
        findViewById<EditText>(R.id.etCrucetaInferiorTipo).setText(registro.crucetaInferiorTipo ?: "")
        findViewById<EditText>(R.id.etbayonetaObservaciones).setText(registro.bayonetaObservaciones ?: "")
        findViewById<EditText>(R.id.etTempleteCantidad).setText(registro.templeteCantidad?.toString() ?: "")
        findViewById<EditText>(R.id.etTempleteAvifauna).setText(registro.templeteAvifauna?.toString() ?: "")
        findViewById<EditText>(R.id.etAisladorA).setText(registro.aisladorA?.toString() ?: "")
        findViewById<EditText>(R.id.etAisladorB).setText(registro.aisladorB?.toString() ?: "")
        findViewById<EditText>(R.id.etAisladorC).setText(registro.aisladorC?.toString() ?: "")
        findViewById<EditText>(R.id.etDpsA).setText(registro.dpsA?.toString() ?: "")
        findViewById<EditText>(R.id.etDpsB).setText(registro.dpsB?.toString() ?: "")
        findViewById<EditText>(R.id.etDpsC).setText(registro.dpsC?.toString() ?: "")
        findViewById<EditText>(R.id.etequiposAdicionales).setText(registro.equiposAdicionales ?: "")
        findViewById<EditText>(R.id.etSptBajante).setText(registro.sptBajante?.toString() ?: "")
        findViewById<EditText>(R.id.etSptConexion).setText(registro.sptConexion?.toString() ?: "")
        findViewById<EditText>(R.id.etSptSpt).setText(registro.sptCantidad?.toString() ?: "")
        findViewById<EditText>(R.id.etSptEstado).setText(registro.sptEstado?.toString() ?: "")

        // --- Casillas de verificación (CheckBox) ---
        findViewById<CheckBox>(R.id.checkAvifaunaEstructura).isChecked = registro.avifaunaEstructura
        findViewById<CheckBox>(R.id.checkSeccionador).isChecked = registro.seccionador

        // --- Listas desplegables (Spinner) ---
        // Esta lógica es un poco más compleja.
        // Buscamos la posición del valor guardado y la seleccionamos.
        val spinnerApoyoTipo = findViewById<Spinner>(R.id.spinnerApoyoTipo)
        val adapterApoyo = spinnerApoyoTipo.adapter as ArrayAdapter<String>
        spinnerApoyoTipo.setSelection(adapterApoyo.getPosition(registro.apoyoTipo))

        val spinnerConfiguracion = findViewById<Spinner>(R.id.spinnerConfiguracion)
        val adapterConfiguracion = spinnerConfiguracion.adapter as ArrayAdapter<String>
        spinnerConfiguracion.setSelection(adapterConfiguracion.getPosition(registro.configuracion))

        val spinnerDisposicion = findViewById<Spinner>(R.id.spinnerDisposicion)
        val adapterDisposicion = spinnerDisposicion.adapter as ArrayAdapter<String>
        spinnerDisposicion.setSelection(adapterDisposicion.getPosition(registro.disposicion))

        val spinnerAisladores = findViewById<Spinner>(R.id.spinnerAisladores)
        val adapterAisladores = spinnerAisladores.adapter as ArrayAdapter<String>
        spinnerAisladores.setSelection(adapterAisladores.getPosition(registro.aisladorTipo))

        val spinnerbayoneta = findViewById<Spinner>(R.id.spinnerbayoneta)
        val adapterBayoneta = spinnerbayoneta.adapter as ArrayAdapter<String>
        spinnerbayoneta.setSelection(adapterBayoneta.getPosition(registro.bayonetaTipo))

        // --- Fotos ---
        if (!registro.fotoPath.isNullOrEmpty()) {
            multiplePhotoPaths.addAll(registro.fotoPath.split(";").filter { it.isNotBlank() })
            renderMiniaturas()
        }
    }



}
