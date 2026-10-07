package com.example.nightsound
import android.app.*
import android.content.Intent
import android.media.*
import android.os.*
import androidx.core.app.NotificationCompat
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.sqrt
class MonitorService: Service(){
 private var recorder:AudioRecord?=null; @Volatile private var running=false
 private val rate=44100; private val silenceMs=3000L; private val preMs=5000L
 override fun onStartCommand(i:Intent?,f:Int,s:Int):Int{ if(i?.action==ACTION_STOP){stopMonitoring();stopSelf();return START_NOT_STICKY}; val threshold=i?.getIntExtra(EXTRA_THRESHOLD,55)?:55; channel();startForeground(NOTIF,notification());if(!running)startMonitoring(threshold);return START_STICKY }
 private fun startMonitoring(threshold:Int){ val min=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);if(min<=0){stopSelf();return};val audio=AudioRecord(MediaRecorder.AudioSource.MIC,rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,maxOf(min,rate/2));if(audio.state!=AudioRecord.STATE_INITIALIZED){audio.release();stopSelf();return};recorder=audio;running=true;MonitorState.setMonitoring(true);Thread({val buf=ShortArray(2048);val pre=ArrayDeque<ByteArray>();var preBytes=0;val maxPre=rate*2*preMs/1000;var recording=false;var last=0L;var start=0L;var stream=ByteArrayOutputStream();try{audio.startRecording();while(running){val n=audio.read(buf,0,buf.size);if(n<=0)continue;val bytes=ByteArray(n*2);var sum=0.0;for(x in 0 until n){val v=buf[x].toInt();sum+=v.toDouble()*v;bytes[x*2]=(v and 255).toByte();bytes[x*2+1]=(v shr 8 and 255).toByte()};val rms=sqrt(sum/n);val db=if(rms<=0)0.0 else 20*log10(rms/32768.0)+100;val now=System.currentTimeMillis();pre.addLast(bytes);preBytes+=bytes.size;while(preBytes>maxPre&&!pre.isEmpty())preBytes-=pre.removeFirst().size;if(db>=threshold){last=now;if(!recording){recording=true;start=now;stream=ByteArrayOutputStream();pre.forEach{stream.write(it)}}else stream.write(bytes)}else if(recording){stream.write(bytes);if(now-last>=silenceMs){save(stream.toByteArray(),start,now);recording=false;stream=ByteArrayOutputStream()}}}}catch(_:Exception){}finally{if(recording&&stream.size()>0)save(stream.toByteArray(),start,System.currentTimeMillis());try{audio.stop()}catch(_:Exception){};audio.release();recorder=null;running=false;MonitorState.setMonitoring(false);stopSelf()}},"NightSoundRecorder").start() }
 private fun save(pcm:ByteArray,start:Long,end:Long){try{val dir=File(getExternalFilesDir(null),"events");dir.mkdirs();val stamp=SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault()).format(Date(start));val file=File(dir,"sound_$stamp.wav");WavWriter.write(file,pcm,rate);val label=SimpleDateFormat("dd/MM HH:mm:ss",Locale.getDefault()).format(Date(start));MonitorState.addEvent(SoundEvent(file.name,label,maxOf(1,((end-start)/1000).toInt())))}catch(_:Exception){}}
 private fun stopMonitoring(){running=false;try{recorder?.stop()}catch(_:Exception){};MonitorState.setMonitoring(false);stopForeground(STOP_FOREGROUND_REMOVE)}
 private fun channel(){if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Surveillance sonore",NotificationManager.IMPORTANCE_LOW))}
 private fun notification():Notification=NotificationCompat.Builder(this,CHANNEL).setContentTitle("Night Sound").setContentText("Détection sonore en cours").setSmallIcon(android.R.drawable.ic_btn_speak_now).setOngoing(true).build()
 override fun onBind(i:Intent?):IBinder?=null
 override fun onDestroy(){stopMonitoring();super.onDestroy()}
 companion object{const val ACTION_START="com.example.nightsound.START";const val ACTION_STOP="com.example.nightsound.STOP";const val EXTRA_THRESHOLD="threshold";private const val CHANNEL="night_sound_channel";private const val NOTIF=73}
}
