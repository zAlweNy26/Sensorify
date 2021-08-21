package it.alwe.sensorify.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable
import com.google.android.material.snackbar.Snackbar
import it.alwe.sensorify.AvgRedAnalyzer
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import kotlinx.android.synthetic.main.activity_heartbeat.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

// TODO : https://stackoverflow.com/questions/28115049/android-heart-rate-monitor-code-explanation
// TODO : https://github.com/YahyaOdeh/HealthWatcher

class HeartbeatActivity : BaseBlockActivity(), ActivityCompat.OnRequestPermissionsResultCallback {
    private var heartbeatBar: SeekableAnimatedVectorDrawable? = null
    private var heartbeatPulseCompat: AnimatedVectorDrawableCompat? = null
    private var heartbeatPulse: AnimatedVectorDrawable? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var isUsingCamera: Boolean = false
    private var errorSnackbar: Snackbar? = null
    private lateinit var cameraExecutor: ExecutorService

    enum class STATE { ENABLE, DISABLE, CHANGE, GONE }
    enum class TYPE { GREEN, RED }

    private val processing: AtomicBoolean = AtomicBoolean(false)
    private var averageIndex = 0
    private val averageArraySize = 4
    private val averageArray = IntArray(averageArraySize)
    private var currentType = TYPE.GREEN
    private var beatsIndex = 0
    private val beatsArraySize = 3
    private val beatsArray = IntArray(beatsArraySize)
    private var beats = 0.0
    private var cameraTimer = Timer()
    private var textTimer = Timer()

    override val contentView: Int
        get() = R.layout.activity_heartbeat

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = "${getString(R.string.heartbeat_page)} : ${heartbeatValue.text.toString().replace("\n", "")}"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.heartbeat_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }

    override fun addCode() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        heartbeatBar = SeekableAnimatedVectorDrawable.create(this, R.drawable.heartbeat_bar)

        val mainHandler = Handler(Looper.getMainLooper())
        if (Build.VERSION.SDK_INT >= 23) {
            heartbeatPulse = ContextCompat.getDrawable(this, R.drawable.heartbeat_pulse) as AnimatedVectorDrawable
            heartbeatPulse?.registerAnimationCallback(object: Animatable2.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    mainHandler.post { heartbeatPulse?.start() }
                    super.onAnimationEnd(drawable)
                }
            })
        } else {
            heartbeatPulseCompat = ContextCompat.getDrawable(this, R.drawable.heartbeat_pulse) as AnimatedVectorDrawableCompat
            heartbeatPulseCompat?.registerAnimationCallback(object: Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    mainHandler.post { heartbeatPulseCompat?.start() }
                    super.onAnimationEnd(drawable)
                }
            })
        }

        errorSnackbar = Snackbar.make(findViewById(R.id.blockLayout), getString(R.string.stopMeasuring), Snackbar.LENGTH_INDEFINITE).apply {
            view.findViewById<TextView>(R.id.snackbar_text).maxLines = 3
            setAction(getString(android.R.string.ok)) {}
            setActionTextColor(ContextCompat.getColor(context, R.color.yAxisColor))
        }

        val dm = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) this.display?.getRealMetrics(dm)
        else {
            @Suppress("DEPRECATION")
            this.windowManager.defaultDisplay.getMetrics(dm)
        }

        if (dm.widthPixels >= dm.heightPixels) { // landscape
            cameraPreview.layoutParams.width = dm.heightPixels / 2
            cameraPreview.layoutParams.height = dm.heightPixels / 2
            pulseLayout.layoutParams.width = dm.heightPixels - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30f, resources.displayMetrics).toInt()
            pulseLayout.layoutParams.height = (pulseLayout.layoutParams.width * 75) / 200
        } else { // portrait
            cameraPreview.layoutParams.width = dm.widthPixels / 2
            cameraPreview.layoutParams.height = dm.widthPixels / 2
            pulseLayout.layoutParams.width = dm.widthPixels - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30f, resources.displayMetrics).toInt()
            pulseLayout.layoutParams.height = (pulseLayout.layoutParams.width * 75) / 200
        }

        containerView.radius = cameraPreview.layoutParams.width / 2f

        val heartParams = heartbeatValue.layoutParams as ViewGroup.MarginLayoutParams
        heartParams.bottomMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30f, resources.displayMetrics).toInt()
        heartbeatValue.layoutParams = heartParams

        cameraExecutor = Executors.newSingleThreadExecutor()

        startMeasuring.setOnClickListener {
            startUsingCamera()
            manageButton(STATE.GONE)
            pulseLayout.visibility = View.VISIBLE
            errorSnackbar?.dismiss()
            manageBar(false)
        }

        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED))
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        else manageButton(STATE.ENABLE)
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) {
            if (grantResults[0] == 0 && !isUsingCamera) manageButton(STATE.ENABLE)
            else if (grantResults[0] == PackageManager.PERMISSION_DENIED) manageButton(STATE.DISABLE)
        }
    }

    override fun onResume() {
        super.onResume()
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED))
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        else if (!isUsingCamera) manageButton(STATE.ENABLE)
    }

    override fun onPause() {
        super.onPause()
        stopUsingCamera(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun manageButton(state: STATE) {
        val buttonParams = startMeasuring.layoutParams as ViewGroup.MarginLayoutParams
        buttonParams.width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, resources.displayMetrics).toInt()
        buttonParams.height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, resources.displayMetrics).toInt()
        startMeasuring.layoutParams = buttonParams
        val btnDrawable = if (state == STATE.CHANGE) AppCompatResources.getDrawable(this, R.drawable.ic_refresh)
            else AppCompatResources.getDrawable(this, R.drawable.play_to_pause)
        btnDrawable!!.setTint(ContextCompat.getColor(this, if (state == STATE.DISABLE) R.color.colorPrimaryDark else R.color.colorPrimary))
        startMeasuring.setImageDrawable(btnDrawable)
        startMeasuring.visibility = if (state == STATE.GONE) View.GONE else View.VISIBLE
        startMeasuring.isEnabled = state != STATE.DISABLE
        startMeasuring.isClickable = state != STATE.DISABLE
    }

    private fun manageBar(stop: Boolean) {
        pulseLayout.background = heartbeatBar
        if (stop) {
            heartbeatBar?.pause()
            textTimer.cancel()
        } else {
            heartbeatBar?.stop()
            heartbeatBar?.start()
            textTimer = Timer()
            textTimer.scheduleAtFixedRate(object : TimerTask() {
                val dotsText = arrayOf(".", "..", "...")
                var i = 0

                override fun run() {
                    runOnUiThread {
                        if (i == 3) i = 0
                        heartbeatValue.text = dotsText[i++]
                    }
                }
            }, 0, 750)
        }
    }

    private fun stopUsingCamera(full: Boolean) {
        if (full) {
            cameraTimer = Timer()
            cameraTimer.schedule(object : TimerTask() {
                override fun run() {
                    runOnUiThread {
                        if (isUsingCamera) {
                            cameraProvider?.unbindAll()
                            manageButton(STATE.CHANGE)
                            isUsingCamera = false
                            displayFinalValue()
                        }
                    }
                }
            }, 1000L * 10L) // 10 secondi
        } else if (isUsingCamera) {
            runOnUiThread {
                cameraTimer.cancel()
                cameraProvider?.unbindAll()
                manageButton(STATE.CHANGE)
                manageBar(true)
                heartbeatValue.text = "?"
                isUsingCamera = false
            }
        }
    }

    private fun startUsingCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(cameraPreview.surfaceProvider) }
            imageCapture = ImageCapture.Builder().build()
            val imageAnalyzer = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(cameraExecutor, AvgRedAnalyzer { avgRed ->
                    if (avgRed < 200) { // il dito non è sulla fotocamera
                        stopUsingCamera(false)
                        errorSnackbar?.show()
                    } else if (processing.compareAndSet(false, true) && !(avgRed.toInt() == 0 || avgRed.toInt() == 255)) {
                        var averageArrayAvg = 0
                        var averageArrayCnt = 0
                        for (i in averageArray.indices) {
                            if (averageArray[i] > 0) {
                                averageArrayAvg += averageArray[i]
                                averageArrayCnt++
                            }
                        }

                        val rollingAverage = if (averageArrayCnt > 0) averageArrayAvg / averageArrayCnt else 0
                        var newType = currentType

                        if (avgRed.toInt() < rollingAverage) {
                            newType = TYPE.RED
                            if (newType !== currentType) beats++
                        } else if (avgRed.toInt() > rollingAverage) newType = TYPE.GREEN

                        if (averageIndex == averageArraySize) averageIndex = 0
                        averageArray[averageIndex] = avgRed.toInt()
                        averageIndex++

                        if (newType !== currentType) currentType = newType
                    } else processing.set(false)
                })
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider?.unbindAll()
                val camera = cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)
                camera?.cameraControl?.enableTorch(true)
            } catch(exc: Exception) { exc.printStackTrace() }
            isUsingCamera = true
        }, ContextCompat.getMainExecutor(this))
        stopUsingCamera(true)
    }

    private fun displayFinalValue() {
        val bps = beats / 10
        val dpm = (bps * 60.0).toInt()

        if (!(dpm < 30 || dpm > 180)) {
            if (beatsIndex == beatsArraySize) beatsIndex = 0
            beatsArray[beatsIndex] = dpm
            beatsIndex++
            var beatsArrayAvg = 0
            var beatsArrayCnt = 0
            for (i in beatsArray.indices) {
                if (beatsArray[i] > 0) {
                    beatsArrayAvg += beatsArray[i]
                    beatsArrayCnt++
                }
            }
            beats = 0.0
            val beatsAvg = beatsArrayAvg / beatsArrayCnt

            manageBar(true)
            heartbeatValue.text = "$beatsAvg\nbpm"
            if (Build.VERSION.SDK_INT >= 23) {
                pulseLayout.background = heartbeatPulse
                heartbeatPulse?.start()
            } else {
                pulseLayout.background = heartbeatPulseCompat
                heartbeatPulseCompat?.start()
            }
            // https://stackoverflow.com/questions/4371105/how-can-i-change-the-duration-for-an-android-animationdrawable-animation-on-the
            // duration = (1000L * 60L) / beatsAvg
            //val firstSlideLeft: Animation = AnimationUtils.loadAnimation(baseContext, R.anim.from_left)
            //firstSlideLeft.duration = (1000L * 60L) / beatsAvg
            //pulseLayout.startAnimation(firstSlideLeft)
        } else {
            beats = 0.0
            processing.set(false)
        }

        processing.set(false)
    }
}