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
import android.view.LayoutInflater
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import it.alwe.sensorify.databinding.ActivityPedometerBinding

class PedometerActivity : BaseBlockActivity<ActivityPedometerBinding>(),
    ActivityCompat.OnRequestPermissionsResultCallback {
    private var pedometerManager: SensorManager? = null
    private var pedometer: Sensor? = null
    private var distanceUnit: String? = "m"
    private var steps: Long = 0
    //private var firstStepTime: Long = 0L
    //private var secondStepTime: Long = 0L

    override fun setupViewBinding(inflater: LayoutInflater): ActivityPedometerBinding {
        return ActivityPedometerBinding.inflate(inflater)
    }

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

            content.stepsValue.text = steps.toString()

            when (distanceUnit) {
                "m" -> content.distanceValue.text = "${decimalPrecision?.format((steps * 74) / 100.0)} m"
                "ft" -> content.distanceValue.text = "${decimalPrecision?.format(((steps * 74) / 100.0) * 3.281)} ft"
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

        content.sensorName.text = pedometer?.name?.replaceFirstChar { it.uppercase() }
        content.sensorVendor.text = pedometer?.vendor.toString().replaceFirstChar { it.uppercase() }
        content.sensorVersion.text = pedometer?.version.toString()
        content.sensorPowerUsage.text = "${pedometer?.power.toString()} mA"
        content.sensorResolution.text = convertSuperScript(pedometer?.resolution.toString())
        content.sensorRange.text = convertSuperScript(pedometer?.maximumRange.toString())

        if (Build.VERSION.SDK_INT >= 29) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 1)
        }
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = "${getString(R.string.pedometer_page)} :\n" +
            "${getString(R.string.stepsText)} ${content.stepsValue.text}\n" +
            "${getString(R.string.distanceText)} ${content.distanceValue.text}\n" +
            "${getString(R.string.sensorName)} ${content.sensorName.text}\n" +
            "${getString(R.string.sensorVendor)} ${content.sensorVendor.text}\n" +
            "${getString(R.string.sensorVersion)} ${content.sensorVersion.text}\n" +
            "${getString(R.string.sensorPowerUsage)} ${content.sensorPowerUsage.text}\n" +
            "${getString(R.string.sensorResolution)} ${content.sensorResolution.text}\n" +
            "${getString(R.string.sensorRange)} ${content.sensorRange.text}"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.pedometer_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}