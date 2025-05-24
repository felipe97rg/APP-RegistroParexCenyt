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

        // UBICACIÓN
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


        // GUARDAR
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
        val fechaHora = fechaHoraActual // desde arriba
        val area = findViewById<EditText>(R.id.etArea).text.toString()
        val circuito = findViewById<EditText>(R.id.etCircuito).text.toString()
        val estructura = findViewById<EditText>(R.id.etEstructura).text.toString()
        val nombreResponsable = findViewById<EditText>(R.id.etNombre).text.toString()
        val observaciones = findViewById<EditText>(R.id.etObservaciones).text.toString()
        val latitud = findViewById<EditText>(R.id.etLatitud).text.toString()
        val longitud = findViewById<EditText>(R.id.etLongitud).text.toString()

        if (nombreResponsable.isBlank()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val nuevoRegistro = Registro(
            fechaHora = fechaHoraActual,
            nombreResponsable = findViewById<EditText>(R.id.etNombre).text.toString(),
            area = findViewById<EditText>(R.id.etArea).text.toString(),
            circuito = findViewById<EditText>(R.id.etCircuito).text.toString(),
            estructuraNumero = findViewById<EditText>(R.id.etEstructura).text.toString(),
            latitud = findViewById<EditText>(R.id.etLatitud).text.toString(),
            longitud = findViewById<EditText>(R.id.etLongitud).text.toString(),
            observaciones = findViewById<EditText>(R.id.etObservaciones).text.toString(),
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
                val imageView = findViewById<ImageView>(R.id.imagePreview)

                // Copiar la imagen a tu carpeta de fotos internas
                val inputStream = contentResolver.openInputStream(uri)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "IMG_${timeStamp}.jpg")
                val outputStream = file.outputStream()

                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                currentPhotoPath = file.absolutePath
                imageView.setImageURI(Uri.fromFile(file))
                imageView.visibility = View.VISIBLE
            }
        }

    }
}
