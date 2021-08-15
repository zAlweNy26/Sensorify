package it.alwe.sensorify.sensors

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import kotlinx.android.synthetic.main.activity_linear_acceration.*

class LinearAccActivity : BaseBlockActivity() {
    private var linAccManager: SensorManager? = null
    private var linearAcceleration: Sensor? = null
    private var distanceUnit: String? = "m"

    override val contentView: Int
        get() = R.layout.activity_linear_acceration

    override fun onResume() {
        super.onResume()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        distanceUnit = sharedPrefs.getString("distanceUnit", "m")
        setListenerRegistered(true, linAccManager!!, linearAccelerationListener, linearAcceleration!!)
        toggleThread(true)
    }

    override fun onPause() {
        super.onPause()
        setListenerRegistered(false, linAccManager!!, linearAccelerationListener, linearAcceleration!!)
        toggleThread(false)
    }

    private val linearAccelerationListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {

            val linearAccelerationArray = FloatArray(3)
            var resolution = linearAcceleration?.resolution.toString()
            var maxRange = linearAcceleration?.maximumRange.toString()

            when (distanceUnit) {
                "m" -> {
                    linearAccelerationArray[0] = event.values[0]
                    linearAccelerationArray[1] = event.values[1]
                    linearAccelerationArray[2] = event.values[2]
                }
                "ft" -> {
                    linearAccelerationArray[0] = event.values[0] * 3.281f
                    linearAccelerationArray[1] = event.values[1] * 3.281f
                    linearAccelerationArray[2] = event.values[2] * 3.281f

                    resolution = (resolution.toDouble() * 3.281).toString()
                    maxRange = (maxRange.toDouble() * 3.281).toString()
                }
            }

            xValue.text = getString(R.string.valueX, "${decimalPrecision?.format(linearAccelerationArray[0])} $distanceUnit/s²")
            yValue.text = getString(R.string.valueY, "${decimalPrecision?.format(linearAccelerationArray[1])} $distanceUnit/s²")
            zValue.text = getString(R.string.valueZ, "${decimalPrecision?.format(linearAccelerationArray[2])} $distanceUnit/s²")
            sensorResolution.text = "${convertSuperScript(resolution)} $distanceUnit/s²"
            sensorRange.text = "${convertSuperScript(maxRange)} $distanceUnit/s²"

            addEntry(1, 3, linearAccelerationArray, arrayOf("X", "Y", "Z"), arrayOf(
                    "#" + Integer.toHexString(ContextCompat.getColor(applicationContext, R.color.xAxisColor)),
                    "#" + Integer.toHexString(ContextCompat.getColor(applicationContext, R.color.yAxisColor)),
                    "#" + Integer.toHexString(ContextCompat.getColor(applicationContext, R.color.zAxisColor))
                )
            )
        }
    }

    override fun addCode() {
        xValue.text = getString(R.string.valueX, getString(R.string.unknownValue))
        yValue.text = getString(R.string.valueY, getString(R.string.unknownValue))
        zValue.text = getString(R.string.valueZ, getString(R.string.unknownValue))

        linAccManager = getSystemService(SENSOR_SERVICE) as SensorManager
        linearAcceleration = linAccManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        sensorName.text = linearAcceleration?.name?.replaceFirstChar { it.uppercase() }
        sensorVendor.text = linearAcceleration?.vendor.toString().replaceFirstChar { it.uppercase() }
        sensorVersion.text = linearAcceleration?.version.toString()
        sensorPowerUsage.text = "${linearAcceleration?.power.toString()} mA"

        createChart(10f, 3f, 0, legend = true, negative = true)
        startLiveChart()
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val sb = StringBuilder()
        sb.append(getString(R.string.linear_acceleration_page) + " :\n")
        sb.append("${xValue.text}\n" +
            "${yValue.text}\n" +
            "${zValue.text}\n" +
            "${getString(R.string.sensorName)} ${sensorName.text}\n" +
            "${getString(R.string.sensorVendor)} ${sensorVendor.text}\n" +
            "${getString(R.string.sensorVersion)} ${sensorVersion.text}\n" +
            "${getString(R.string.sensorPowerUsage)} ${sensorPowerUsage.text}\n" +
            "${getString(R.string.sensorResolution)} ${sensorResolution.text}\n" +
            "${getString(R.string.sensorRange)} ${sensorRange.text}")
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.linear_acceleration_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, sb.toString())
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}
