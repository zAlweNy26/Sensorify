package it.alwe.sensorify.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var isUsingCamera: Boolean = false
    private lateinit var cameraExecutor: ExecutorService

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
    private var timer = Timer()

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
        heartbeatValue.text = "0\nbpm"

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
            enableButton(false)
        }

        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED))
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        else enableButton(true)
    }

    private fun enableButton(enable: Boolean) {
        startMeasuring.visibility = if (enable) View.VISIBLE else View.INVISIBLE
        startMeasuring.isEnabled = enable
        startMeasuring.isClickable = enable
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) if (grantResults[0] == 0 && !isUsingCamera) enableButton(true)
    }

    override fun onResume() {
        super.onResume()
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED))
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        else if (!isUsingCamera) enableButton(true)
    }

    override fun onPause() {
        super.onPause()
        stopUsingCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun stopUsingCamera() {
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (isUsingCamera) {
                        cameraProvider?.unbindAll()
                        enableButton(true)
                        isUsingCamera = false
                        displayFinalValue()
                    }
                }
            }
        }, 1000L * 10L) // 10 secondi
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
                        runOnUiThread {
                            timer.cancel()
                            cameraProvider?.unbindAll()
                            enableButton(true)
                            heartbeatValue.text = getString(R.string.insufficientData)
                            isUsingCamera = false
                            Snackbar.make(
                                findViewById(R.id.blockLayout), getString(R.string.stopMeasuring),
                                Snackbar.LENGTH_INDEFINITE
                            ).apply {
                                view.findViewById<TextView>(R.id.snackbar_text).maxLines = 3
                                setAction(getString(android.R.string.ok)) {}
                                setActionTextColor(ContextCompat.getColor(context, R.color.yAxisColor))
                                show()
                            }
                        }
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
            enableButton(false)
            heartbeatValue.text = getString(R.string.isMeasuring)
            isUsingCamera = true
        }, ContextCompat.getMainExecutor(this))

        stopUsingCamera()
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
            val beatsAvg = beatsArrayAvg / beatsArrayCnt
            heartbeatValue.text = "$beatsAvg\nbpm"
            beats = 0.0
        } else {
            beats = 0.0
            processing.set(false)
        }

        processing.set(false)
    }
}