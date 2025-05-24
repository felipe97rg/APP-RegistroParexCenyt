package com.cenyt.registrodedatos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = onRegistrar, modifier = Modifier.fillMaxWidth()) {
                Text("Registrar Operador")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onVerRegistros, modifier = Modifier.fillMaxWidth()) {
                Text("Ver Registros")
            }
        }
    }
}
