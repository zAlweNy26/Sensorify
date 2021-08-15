package it.alwe.sensorify.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import kotlinx.android.synthetic.main.activity_pedometer.*

class PedometerActivity : BaseBlockActivity(), ActivityCompat.OnRequestPermissionsResultCallback {
    private var pedometerManager: SensorManager? = null
    private var pedometer: Sensor? = null
    private var distanceUnit: String? = "m"
    private var steps: Long = 0
    //private var firstStepTime: Long = 0L
    //private var secondStepTime: Long = 0L

    override val contentView: Int
        get() = R.layout.activity_pedometer

    override fun onResume() {
        super.onResume()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        distanceUnit = sharedPrefs.getString("distanceUnit", "m")
        setListenerRegistered(true, pedometerManager!!, pedometerListener, pedometer!!)
    }

    override fun onPause() {
        super.onPause()
        setListenerRegistered(false, pedometerManager!!, pedometerListener, pedometer!!)
    }

    // TODO : https://stackoverflow.com/questions/43441054/how-to-count-speed-based-on-steps

    private val pedometerListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                /*when {
                    firstStepTime == 0L -> firstStepTime = System.currentTimeMillis() / 1000L
                    secondStepTime == 0L -> secondStepTime = System.currentTimeMillis() / 1000L
                    else -> {
                        firstStepTime = secondStepTime
                        secondStepTime = System.currentTimeMillis() / 1000L
                    }
                }*/
                steps++
            }

            /*if (firstStepTime != 0L && secondStepTime != 0L) {
                val timeDiff = secondStepTime - firstStepTime
                if (timeDiff >= 1L) speedValue.text = "${((steps * 74) / 100.0) / timeDiff} m/s"
                else speedValue.text = "0 m/s"
            }*/

            stepsValue.text = steps.toString()

            when (distanceUnit) {
                "m" -> distanceValue.text = "${decimalPrecision?.format((steps * 74) / 100.0)} m"
                "ft" -> distanceValue.text = "${decimalPrecision?.format(((steps * 74) / 100.0) * 3.281)} ft"
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) if (grantResults[0] == 0) recreate()
    }

    override fun addCode() {
        pedometerManager = getSystemService(SENSOR_SERVICE) as SensorManager
        pedometer = pedometerManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        sensorName.text = pedometer?.name?.replaceFirstChar { it.uppercase() }
        sensorVendor.text = pedometer?.vendor.toString().replaceFirstChar { it.uppercase() }
        sensorVersion.text = pedometer?.version.toString()
        sensorPowerUsage.text = "${pedometer?.power.toString()} mA"
        sensorResolution.text = convertSuperScript(pedometer?.resolution.toString())
        sensorRange.text = convertSuperScript(pedometer?.maximumRange.toString())

        if (Build.VERSION.SDK_INT >= 29) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 1)
        }
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = "${getString(R.string.pedometer_page)} :\n" +
            "${getString(R.string.stepsText)} ${stepsValue.text}\n" +
            "${getString(R.string.distanceText)} ${distanceValue.text}\n" +
            "${getString(R.string.sensorName)} ${sensorName.text}\n" +
            "${getString(R.string.sensorVendor)} ${sensorVendor.text}\n" +
            "${getString(R.string.sensorVersion)} ${sensorVersion.text}\n" +
            "${getString(R.string.sensorPowerUsage)} ${sensorPowerUsage.text}\n" +
            "${getString(R.string.sensorResolution)} ${sensorResolution.text}\n" +
            "${getString(R.string.sensorRange)} ${sensorRange.text}"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.pedometer_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}