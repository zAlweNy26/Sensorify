package it.alwe.sensorify.sensors

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import it.alwe.sensorify.databinding.ActivityAccelerometerBinding

class AccelerometerActivity : BaseBlockActivity<ActivityAccelerometerBinding>() {
    private var accelerometerManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var distanceUnit: String? = "m"

    override fun setupViewBinding(inflater: LayoutInflater): ActivityAccelerometerBinding {
        return ActivityAccelerometerBinding.inflate(inflater)
    }

    override fun onResume() {
        super.onResume()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        distanceUnit = sharedPrefs.getString("distanceUnit", "m")
        setListenerRegistered(true, accelerometerManager!!, accelerometerListener, accelerometer!!)
        //toggleThread(true)
    }

    override fun onPause() {
        super.onPause()
        setListenerRegistered(false, accelerometerManager!!, accelerometerListener, accelerometer!!)
        //toggleThread(false)
    }

    private val accelerometerListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            val accelerometerArray = FloatArray(3)
            var resolution = accelerometer?.resolution.toString()
            var maxRange = accelerometer?.maximumRange.toString()

            when (distanceUnit) {
                "m" -> {
                    accelerometerArray[0] = event.values[0]
                    accelerometerArray[1] = event.values[1]
                    accelerometerArray[2] = event.values[2]
                }
                "ft" -> {
                    accelerometerArray[0] = event.values[0] * 3.281f
                    accelerometerArray[1] = event.values[1] * 3.281f
                    accelerometerArray[2] = event.values[2] * 3.281f
                    resolution = (resolution.toDouble() * 3.281).toString()
                    maxRange = (maxRange.toDouble() * 3.281).toString()
                }
            }

            content.xValue.text = getString(R.string.valueX, "${decimalPrecision?.format(accelerometerArray[0])} $distanceUnit/s²")
            content.yValue.text = getString(R.string.valueY, "${decimalPrecision?.format(accelerometerArray[1])} $distanceUnit/s²")
            content.zValue.text = getString(R.string.valueZ, "${decimalPrecision?.format(accelerometerArray[2])} $distanceUnit/s²")
            content.sensorResolution.text = "${convertSuperScript(resolution)} $distanceUnit/s²"
            content.sensorRange.text = "${convertSuperScript(maxRange)} $distanceUnit/s²"

            addEntry(1, 3, accelerometerArray, arrayOf("X", "Y", "Z"), arrayOf(
                    "#" + Integer.toHexString(ContextCompat.getColor(applicationContext, R.color.xAxisColor)),
                    "#" + Integer.toHexString(ContextCompat.getColor(applicationContext, R.color.yAxisColor)),
                    "#" + Integer.toHexString(ContextCompat.getColor(applicationContext, R.color.zAxisColor))
                )
            )
        }
    }

    override fun addCode() {
        content.xValue.text = getString(R.string.valueX, getString(R.string.unknownValue))
        content.yValue.text = getString(R.string.valueY, getString(R.string.unknownValue))
        content.zValue.text = getString(R.string.valueZ, getString(R.string.unknownValue))

        accelerometerManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = accelerometerManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        content.sensorName.text = accelerometer?.name?.replaceFirstChar { it.uppercase() }
        content.sensorVendor.text = accelerometer?.vendor.toString().replaceFirstChar { it.uppercase() }
        content.sensorVersion.text = accelerometer?.version.toString()
        content.sensorPowerUsage.text = "${accelerometer?.power.toString()} mA"

        createChart(10f, 3f, 0, legend = true, negative = true)
        startLiveChart()
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val sb = StringBuilder()
        sb.append("${getString(R.string.accelerometer_page)} :\n" +
            "${content.xValue.text}\n" +
            "${content.yValue.text}\n" +
            "${content.zValue.text}\n" +
            "${getString(R.string.sensorName)} ${content.sensorName.text}\n" +
            "${getString(R.string.sensorVendor)} ${content.sensorVendor.text}\n" +
            "${getString(R.string.sensorVersion)} ${content.sensorVersion.text}\n" +
            "${getString(R.string.sensorPowerUsage)} ${content.sensorPowerUsage.text}\n" +
            "${getString(R.string.sensorResolution)} ${content.sensorResolution.text}\n" +
            "${getString(R.string.sensorRange)} ${content.sensorRange.text}")
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.accelerometer_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, sb.toString())
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}
