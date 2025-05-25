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
    private lateinit var fechaHoraActual: String
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_GALLERY = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_form)

        db = AppDatabase.getDatabase(this)

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
        val photoURI = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", photoFile)
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
            apoyoTipo = getText(R.id.etApoyoTipo),
            apoyoCantidad = getInt(R.id.etApoyoCantidad),
            tipoNorma = getText(R.id.etTipoNorma),
            distancia = getInt(R.id.etDistancia),
            resistencia = getInt(R.id.etResistencia),
            avifaunaEstructura = getCheck(R.id.checkAvifaunaEstructura),
            crucetaSuperior = getInt(R.id.etCrucetaSuperior),
            crucetaInferiorTipo = getText(R.id.etCrucetaInferiorTipo),
            bayonetaIzquierda = getInt(R.id.etBayonetaIzquierda),
            bayonetaDerecha = getInt(R.id.etBayonetaDerecha),
            templeteCantidad = getInt(R.id.etTempleteCantidad),
            templeteAvifauna = getInt(R.id.etTempleteAvifauna),
            aisladorTipo = getText(R.id.etAisladorTipo),
            aisladorA = getInt(R.id.etAisladorA),
            aisladorB = getInt(R.id.etAisladorB),
            aisladorC = getInt(R.id.etAisladorC),
            dpsA = getInt(R.id.etDpsA),
            dpsB = getInt(R.id.etDpsB),
            dpsC = getInt(R.id.etDpsC),
            seccionador = getCheck(R.id.checkSeccionador),
            amortiguadorAtras = getInt(R.id.etAmortiguadorAtras),
            amortiguadorAdelante = getInt(R.id.etAmortiguadorAdelante),
            sptBajante = getInt(R.id.etSptBajante),
            sptConexion = getInt(R.id.etSptConexion),
            sptCantidad = getInt(R.id.etSptSpt),
            sptEstado = getInt(R.id.etSptEstado),
            medicionR = getCheck(R.id.checkMedicionR),
            medicionP = getCheck(R.id.checkMedicionP),
            fotoPath = currentPhotoPath
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
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            imageView.setImageURI(Uri.fromFile(File(currentPhotoPath)))
            imageView.visibility = View.VISIBLE
        }
        if (requestCode == REQUEST_IMAGE_GALLERY && resultCode == RESULT_OK && data != null) {
            val selectedImageUri = data.data
            selectedImageUri?.let { uri ->
                val inputStream = contentResolver.openInputStream(uri)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "IMG_${timeStamp}.jpg")
                inputStream?.copyTo(file.outputStream())
                inputStream?.close()
                currentPhotoPath = file.absolutePath
                imageView.setImageURI(Uri.fromFile(file))
                imageView.visibility = View.VISIBLE
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) abrirCamara()
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de ubicación concedido", Toast.LENGTH_SHORT).show()
        }
    }
}
