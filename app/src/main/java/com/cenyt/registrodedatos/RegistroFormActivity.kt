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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_form)

        db = AppDatabase.getDatabase(this)

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




        val imageView = findViewById<ImageView>(R.id.imagePreview)
        val btnFoto = findViewById<Button>(R.id.btnAdjuntarFoto)
        val tvFechaHora = findViewById<TextView>(R.id.tvFechaHora)
        fechaHoraActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        tvFechaHora.text = "Fecha y hora: $fechaHoraActual"

        btnFoto.setOnClickListener {
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

        val btnMultiplesFotos = findViewById<Button>(R.id.btnAdjuntarMultiplesFotos)
        btnMultiplesFotos.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            startActivityForResult(Intent.createChooser(intent, "Selecciona imágenes"), REQUEST_MULTIPLE_IMAGES)
        }


        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val etLatitud = findViewById<EditText>(R.id.etLatitud)
        val etLongitud = findViewById<EditText>(R.id.etLongitud)
        val btnUbicacion = findViewById<Button>(R.id.btnUbicacion)

        btnUbicacion.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
                return@setOnClickListener
            }
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    etLatitud.setText(location.latitude.toString())
                    etLongitud.setText(location.longitude.toString())
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
                }
            }
        }

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

        val nuevoRegistro = Registro(
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
            // medicionR = getCheck(R.id.checkMedicionR),
            // medicionP = getCheck(R.id.checkMedicionP),
            fotoPath = multiplePhotoPaths.joinToString(";"),
        )

        CoroutineScope(Dispatchers.IO).launch {
            db.registroDao().insertar(nuevoRegistro)
            runOnUiThread {
                Toast.makeText(this@RegistroFormActivity, "Registro guardado", Toast.LENGTH_SHORT).show()
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
}
