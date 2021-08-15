package it.alwe.sensorify.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimatedVectorDrawable
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import kotlinx.android.synthetic.main.activity_sound_meter.*
import java.io.IOException
import java.util.*
import kotlin.math.log10

class SoundMeterActivity : BaseBlockActivity(), ActivityCompat.OnRequestPermissionsResultCallback {
    private var playToPause: AnimatedVectorDrawable? = null
    private var pauseToPlay: AnimatedVectorDrawable? = null
    private var playOrPause: Boolean = false
    private lateinit var soundRecorder: MediaRecorder
    private lateinit var runnable: Runnable
    private var mediaPlayer: MediaPlayer? = null
    private var handler: Handler = Handler(Looper.getMainLooper())
    private var recorderState = false
    private var timer = Timer()

    override val contentView: Int
        get() = R.layout.activity_sound_meter

    override fun onResume() {
        super.onResume()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        else {
            if (!recorderState) {
                startRecorder()
                toggleThread(true)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (recorderState) {
            toggleThread(false)
            recorderState = false
            try {
                timer.cancel()
                soundRecorder.stop()
                soundRecorder.release()
            } catch (e: IllegalStateException) { e.printStackTrace() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer != null) {
            mediaPlayer?.apply {
                stop()
                reset()
                release()
                handler.removeCallbacks(runnable)
            }
            mediaPlayer = null
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) {
            if (grantResults[0] == 0 && !recorderState) {
                startRecorder()
                toggleThread(true)
            }
        }
    }

    override fun addCode() {
        amplitudeText.text = getString(R.string.amplitudeText, 0)
        noiseText.text = getString(R.string.noiseText, "0")

        createChart(10f, 1f, 0, legend = false, negative = true)

        playToPause = ContextCompat.getDrawable(baseContext, R.drawable.play_to_pause) as AnimatedVectorDrawable
        pauseToPlay = ContextCompat.getDrawable(baseContext, R.drawable.pause_to_play) as AnimatedVectorDrawable

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        else {
            startRecorder()
            toggleThread(true)
        }

        audioButton.setOnClickListener {
            val drawable = if (playOrPause) pauseToPlay else playToPause
            audioButton.setImageDrawable(drawable)
            drawable?.start()

            if (!playOrPause) {
                mediaPlayer = MediaPlayer.create(this, R.raw.clean_speaker)
                audioBar.max = mediaPlayer!!.duration / 1000
                runnable = Runnable {
                    try {
                        audioBar.progress = mediaPlayer!!.currentPosition / 1000
                        handler.postDelayed(runnable, 1000)
                    } catch (e: Exception) {
                        audioBar.progress = 0
                    }
                }
                handler.postDelayed(runnable,1000)
                mediaPlayer?.start()
            } else mediaPlayer?.pause()

            playOrPause = !playOrPause
        }

        audioBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress * 1000)
                if (progress == audioBar.max) {
                    mediaPlayer?.seekTo(0)

                    val drawable = if (playOrPause) pauseToPlay else playToPause
                    audioButton.setImageDrawable(drawable)
                    drawable?.start()

                    mediaPlayer?.stop()
                    mediaPlayer?.reset()
                    mediaPlayer?.release()
                    handler.removeCallbacks(runnable)

                    playOrPause = !playOrPause
                }
            }

            override fun onStartTrackingTouch(seek: SeekBar) {}

            override fun onStopTrackingTouch(seek: SeekBar) {}
        })
    }

    private fun startRecorder() {
        soundRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(/*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) MediaRecorder.OutputFormat.MPEG_2_TS else */MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setOutputFile(File.createTempFile("audio.ts", null, baseContext.cacheDir))
            else */setOutputFile("${externalCacheDir?.absolutePath}/audio.3gp")

            try { prepare() } catch (e: IOException) { e.printStackTrace() }

            start()
        }
        timer = Timer()
        timer.scheduleAtFixedRate(recorderTask(soundRecorder), 0, 250)
        recorderState = true
    }

    private fun recorderTask(recorder: MediaRecorder) = object : TimerTask() {
        override fun run() {
            runOnUiThread {
                val soundArray = FloatArray(1)
                try { soundArray[0] = (20 * log10(recorder.maxAmplitude / 2700.0)).toFloat() }
                catch (e: IllegalStateException) { soundArray[0] = 0f }
                if (soundArray[0].isFinite()) {
                    try { amplitudeText.text = getString(R.string.amplitudeText, recorder.maxAmplitude) }
                    catch (e: IllegalStateException) { amplitudeText.text = getString(R.string.amplitudeText, 0) }
                    noiseText.text = getString(R.string.noiseText, decimalPrecision?.format(soundArray[0]))
                    addEntry(1, 1, soundArray, arrayOf("dB"), arrayOf("#" + Integer.toHexString(ContextCompat.getColor(applicationContext, R.color.monoAxisColor))))
                }
            }
        }
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = "${getString(R.string.sound_meter_page)} :\n" +
                "${noiseText.text}\n" +
                "${amplitudeText.text}"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.sound_meter_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}
