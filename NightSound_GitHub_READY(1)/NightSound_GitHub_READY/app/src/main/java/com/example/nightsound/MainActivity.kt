package com.example.nightsound
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
class MainActivity:ComponentActivity(){private var pending=false;private var threshold=55
 private val launcher=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){r->val ok=r[Manifest.permission.RECORD_AUDIO]==true||ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;if(ok&&pending)startMonitor(threshold);pending=false}
 override fun onCreate(b:Bundle?){super.onCreate(b);setContent{val active by MonitorState.isMonitoring.collectAsState();val events by MonitorState.events.collectAsState();var sens by remember{mutableFloatStateOf(55f)};MaterialTheme{Surface(Modifier.fillMaxSize()){Column(Modifier.fillMaxSize().padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Text("Night Sound",style=MaterialTheme.typography.headlineLarge);Text("Surveillance sonore pendant la nuit");Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(if(active)"Surveillance active" else "Surveillance arrêtée");Text("Sensibilité : ${sens.toInt()}");Slider(sens,{sens=it},valueRange=40f..90f);Text("Valeur plus basse = détection plus sensible.");Button({if(active)stopMonitor() else request(sens.toInt())},Modifier.fillMaxWidth()){Text(if(active)"Arrêter la surveillance" else "Démarrer la surveillance")}}};Text("Derniers événements",style=MaterialTheme.typography.titleLarge);if(events.isEmpty())Text("Aucun bruit enregistré pour le moment.")else LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(events){e->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(e.timeLabel);Text("${e.fileName} · environ ${e.durationSeconds} s")}}}};Text("Les valeurs de sensibilité sont expérimentales et ne sont pas des dB calibrés.")}}}}}
 private fun request(t:Int){threshold=t;val p=mutableListOf(Manifest.permission.RECORD_AUDIO);if(Build.VERSION.SDK_INT>=33)p.add(Manifest.permission.POST_NOTIFICATIONS);val miss=p.filter{ContextCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED};if(miss.isEmpty())startMonitor(t)else{pending=true;launcher.launch(miss.toTypedArray())}}
 private fun startMonitor(t:Int){startServiceIntent(t)}
 private fun startServiceIntent(t:Int){val i=Intent(this,MonitorService::class.java).apply{action=MonitorService.ACTION_START;putExtra(MonitorService.EXTRA_THRESHOLD,t)};ContextCompat.startForegroundService(this,i)}
 private fun stopMonitor(){startService(Intent(this,MonitorService::class.java).apply{action=MonitorService.ACTION_STOP})}
}
