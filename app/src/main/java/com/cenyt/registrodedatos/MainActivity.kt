package com.cenyt.registrodedatos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cenyt.registrodedatos.ui.theme.RegistroDeDatosPAREXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RegistroDeDatosPAREXTheme {
                PantallaPrincipal(
                    onRegistrar = {
                        startActivity(Intent(this, RegistroFormActivity::class.java))
                    },
                    onVerRegistros = {
                        startActivity(Intent(this, RegistroListActivity::class.java))
                    }
                )
            }
        }
    }
}
@Composable
fun PantallaPrincipal(
    onRegistrar: () -> Unit,
    onVerRegistros: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),  // azul claro arriba
                        Color(0xFF61AEFA), // intermedio 1
                        Color(0xFF2C92F8), // intermedio 2
                        Color(0xFF003366) // azul oscuro
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_cenyt),
                contentDescription = "Logo de CEN&T",
                modifier = Modifier
                    .size(300.dp)
                    .padding(bottom = 24.dp)
            )

            Text(
                text = "¡Bienvenido!",
                fontSize = 26.sp,
                color = Color.White
            )

            Text(
                text = "Registra aquí los datos de equipos en campo.",
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
                fontSize = 16.sp
            )

            Button(
                onClick = onRegistrar,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(50.dp)
            ) {
                Text("Nuevo registro", color = Color(0xFF003366), fontSize = 16.sp)
            }

            Button(
                onClick = onVerRegistros,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Ver consolidado de registros", color = Color(0xFF003366), fontSize = 16.sp)
            }
        }
    }
}

