package com.cenyt.registrodedatos

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.google.android.gms.location.LocationServices
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import android.graphics.BitmapFactory
import android.view.View
import androidx.compose.ui.graphics.asImageBitmap

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale




class RegistroFormActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var currentPhotoPath: String
    private val REQUEST_IMAGE_CAPTURE = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_form)

        db = AppDatabase.getDatabase(this)

        val imageView = findViewById<ImageView>(R.id.imagePreview)
        val btnFoto = findViewById<Button>(R.id.btnAdjuntarFoto)

        btnFoto.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
                return@setOnClickListener
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



        // CONFIGURAR UBICACIÓN GPS
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




        // CONFIGURAR SPINNER
        val spinner = findViewById<Spinner>(R.id.spinnerTurno)
        val turnos = listOf("Mañana", "Tarde", "Noche")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, turnos)

        // BOTÓN GUARDAR
        findViewById<Button>(R.id.btnGuardar).setOnClickListener {
            guardarRegistro()
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }


    private fun guardarRegistro() {
        val nombre = findViewById<EditText>(R.id.etNombre).text.toString()
        val ubicacion = findViewById<EditText>(R.id.etUbicacion).text.toString()
        val observaciones = findViewById<EditText>(R.id.etObservaciones).text.toString()
        val turno = findViewById<Spinner>(R.id.spinnerTurno).selectedItem.toString()
        val latitud = findViewById<EditText>(R.id.etLatitud).text.toString()
        val longitud = findViewById<EditText>(R.id.etLongitud).text.toString()

        if (nombre.isBlank() || ubicacion.isBlank()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }



        val nuevoRegistro = Registro(
            nombre = nombre,
            turno = turno,
            ubicacion = ubicacion,
            observaciones = observaciones,
            latitud = latitud,
            longitud = longitud,
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

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageView = findViewById<ImageView>(R.id.imagePreview)
            imageView.setImageURI(Uri.fromFile(File(currentPhotoPath)))
            imageView.visibility = View.VISIBLE
        }
    }
}
