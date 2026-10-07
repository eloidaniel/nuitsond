package com.eloi.nightsound

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it[Manifest.permission.RECORD_AUDIO] == true) startMonitoring()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NightSoundTheme {
                NightSoundScreen(
                    onStart = {
                        if (ContextCompat.checkSelfPermission(
                                this, Manifest.permission.RECORD_AUDIO
                            ) != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(arrayOf(
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.POST_NOTIFICATIONS
                            ))
                        } else startMonitoring()
                    },
                    onStop = { stopMonitoring() }
                )
            }
        }
    }

    private fun startMonitoring() {
        ContextCompat.startForegroundService(
            this, Intent(this, SoundMonitorService::class.java).setAction("START")
        )
    }

    private fun stopMonitoring() {
        startService(Intent(this, SoundMonitorService::class.java).setAction("STOP"))
    }
}

@Composable
fun NightSoundScreen(onStart: () -> Unit, onStop: () -> Unit) {
    var running by remember { mutableStateOf(false) }
    var sensitivity by remember { mutableFloatStateOf(65f) }

    Surface(Modifier.fillMaxSize(), color = Color(0xFF080D1A)) {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(25.dp))
            Icon(Icons.Default.NightsStay, null,
                tint = Color(0xFF9CAEFF), modifier = Modifier.size(50.dp))
            Text("Night Sound", color = Color.White, fontSize = 30.sp,
                fontWeight = FontWeight.Bold)
            Text("Enregistre uniquement les bruits détectés",
                color = Color(0xFF9BA4B8))

            Spacer(Modifier.height(35.dp))

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(Color(0xFF121A2B)),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (running) Icons.Default.Mic else Icons.Default.History,
                            null,
                            tint = if (running) Color(0xFF69E6A5) else Color(0xFF9CAEFF)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                if (running) "Écoute en cours" else "Prêt pour la nuit",
                                color = Color.White, fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                if (running) "Le micro surveille le niveau sonore"
                                else "Aucun enregistrement en cours",
                                color = Color(0xFF9BA4B8)
                            )
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = {
                            running = !running
                            if (running) onStart() else onStop()
                        },
                        Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (running)
                                Color(0xFF9B3B55) else Color(0xFF6678FF)
                        )
                    ) {
                        Icon(if (running) Icons.Default.Stop else Icons.Default.Mic, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (running) "Arrêter la nuit" else "Démarrer la nuit")
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(Color(0xFF121A2B)),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text("Sensibilité", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        "Seuil de déclenchement : ${sensitivity.toInt()}",
                        color = Color(0xFF9BA4B8), fontSize = 13.sp
                    )
                    Slider(
                        value = sensitivity,
                        onValueChange = { sensitivity = it },
                        valueRange = 40f..90f
                    )
                    Text(
                        "Une sensibilité élevée déclenche moins facilement les petits bruits.",
                        color = Color(0xFF727B91), fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(Color(0xFF121A2B)),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text("Derniers événements", color = Color.White,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Les enregistrements apparaîtront ici après une détection.",
                        color = Color(0xFF9BA4B8), fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun NightSoundTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF6678FF),
            secondary = Color(0xFF9CAEFF),
            background = Color(0xFF080D1A)
        ),
        content = content
    )
}
