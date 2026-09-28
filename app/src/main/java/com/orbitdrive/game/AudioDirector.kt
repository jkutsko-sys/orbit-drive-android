package com.orbitdrive.game

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.exp

/** Original soft electric-key melody and warm sustained chords, with three area variations. */
internal class AudioDirector(context: Context) {
    private val prefs = context.getSharedPreferences("orbit_audio", Context.MODE_PRIVATE)
    var musicEnabled by mutableStateOf(prefs.getBoolean("music", true)); private set
    var effectsEnabled by mutableStateOf(prefs.getBoolean("effects", true)); private set
    var musicVolume by mutableFloatStateOf(prefs.getFloat("musicVolume", .16f)); private set
    var effectsVolume by mutableFloatStateOf(prefs.getFloat("effectsVolume", .45f)); private set
    @Volatile private var playing = true
    @Volatile private var area = 0
    @Volatile private var accent = 0
    private val rate = 22050
    private val notes = arrayOf(
        intArrayOf(262, 330, 392, 523, 392, 330, 294, 392, 262, 330, 440, 523, 440, 392, 330, 294),
        intArrayOf(220, 330, 440, 587, 440, 330, 247, 392, 220, 349, 440, 659, 440, 349, 294, 392),
        intArrayOf(196, 294, 392, 587, 523, 392, 247, 370, 196, 330, 494, 659, 587, 494, 330, 247)
    )
    private val track: AudioTrack? = runCatching {
        val minimum = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        require(minimum > 0)
        AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(maxOf(minimum * 2, rate * 2))
            .setTransferMode(AudioTrack.MODE_STREAM).build()
    }.getOrNull()
    private val worker = Thread({ loop() }, "orbit-audio").apply { isDaemon = true; start() }

    fun setArea(distance: Double) { area = if (distance < 7000) 0 else if (distance < 300000) 1 else 2 }
    fun cue(name: String) { accent = when (name) { "swing" -> 1; "lightning" -> 2; "planet" -> 3; "plane" -> 4; "purchase" -> 5; "land" -> 6; else -> 0 } }
    fun setMusic(value: Boolean) { musicEnabled = value; prefs.edit().putBoolean("music", value).apply() }
    fun setEffects(value: Boolean) { effectsEnabled = value; prefs.edit().putBoolean("effects", value).apply() }
    fun adjustMusicVolume(value: Float) { musicVolume = value; prefs.edit().putFloat("musicVolume", value).apply() }
    fun adjustEffectsVolume(value: Float) { effectsVolume = value; prefs.edit().putFloat("effectsVolume", value).apply() }
    private fun loop() {
        val output = track ?: return
        output.play()
        val buffer = ShortArray(1024)
        var sample = 0L
        var effect = 0
        var effectAge = 0
        while (playing) {
            for (i in buffer.indices) {
                val incoming = accent
                if (incoming != 0) { effect = incoming; effectAge = 0; accent = 0 }
                val time = sample.toDouble() / rate
                val stepLength=.78 // Relaxed ~77 BPM phrase, no square-wave lead or kick drum.
                val phraseStep=((time/stepLength).toLong()%16).toInt()
                val note=notes[area][phraseStep].toDouble()
                val age=time%stepLength
                val envelope=(1-exp(-age*24))*exp(-age*3.5)
                val keys=(sin(2*PI*note*time)+.18*sin(2*PI*note*2*time))*.075*envelope
                val chord=notes[area][(phraseStep/4)*4]/2.0
                val pad=(sin(2*PI*chord*time)+.4*sin(2*PI*chord*1.5*time)+.25*sin(2*PI*chord*2*time))*.026
                val swell=sin(PI*(time%(stepLength*4))/(stepLength*4))
                val music=if(musicEnabled) (keys+pad*swell)*musicVolume else 0.0
                val freq = when (effect) { 1 -> 180.0 + effectAge * .04; 2 -> 780.0 - effectAge * .06; 3 -> 90.0 + effectAge * .015; 4 -> 340.0; 5 -> 660.0; else -> 150.0 }
                val duration = if (effect == 3) rate / 2 else rate / 5
                val fx = if (effectsEnabled && effect != 0 && effectAge < duration)
                    sin(2 * PI * freq * effectAge / rate) * (1.0 - effectAge.toDouble() / duration) * effectsVolume * .35 else 0.0
                buffer[i] = ((music + fx).coerceIn(-.9, .9) * 32767).toInt().toShort()
                if (effect != 0 && ++effectAge >= duration) effect = 0
                sample++
            }
            output.write(buffer, 0, buffer.size)
        }
        output.stop(); output.release()
    }
    fun release() { playing = false; worker.interrupt() }
}
