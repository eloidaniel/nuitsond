package com.example.nightsound
import java.io.File
import java.io.FileOutputStream
object WavWriter {
    fun write(file: File, pcm: ByteArray, sampleRate: Int = 44100) {
        FileOutputStream(file).use { out ->
            val dataLength = pcm.size
            out.write("RIFF".toByteArray()); writeInt(out, dataLength + 36); out.write("WAVE".toByteArray()); out.write("fmt ".toByteArray()); writeInt(out,16); writeShort(out,1); writeShort(out,1); writeInt(out,sampleRate); writeInt(out,sampleRate*2); writeShort(out,2); writeShort(out,16); out.write("data".toByteArray()); writeInt(out,dataLength); out.write(pcm)
        }
    }
    private fun writeInt(o: FileOutputStream,v:Int){o.write(v and 255);o.write(v shr 8 and 255);o.write(v shr 16 and 255);o.write(v shr 24 and 255)}
    private fun writeShort(o: FileOutputStream,v:Int){o.write(v and 255);o.write(v shr 8 and 255)}
}
