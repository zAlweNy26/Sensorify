package it.alwe.sensorify.sensors

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import kotlinx.android.synthetic.main.activity_magnetometer.*

class MagnetometerActivity : BaseBlockActivity() {
    private var magnetometerManager: SensorManager? = null
    private var magnetometer: Sensor? = null

    override val contentView: Int
        get() = R.layout.activity_magnetometer

    override fun onResume() {
        super.onResume()
        setListenerRegistered(true, magnetometerManager!!, magnetometerListener, magnetometer!!)
        toggleThread(true)
    }

    override fun onPause() {
        super.onPause()
        setListenerRegistered(false, magnetometerManager!!, magnetometerListener, magnetometer!!)
        toggleThread(false)
    }

    private val magnetometerListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            xValue.text = getString(R.string.valueX, "${decimalPrecision?.format(event.values[0])} μT")
            yValue.text = getString(R.string.valueY, "${decimalPrecision?.format(event.values[1])} μT")
            zValue.text = getString(R.string.valueZ, "${decimalPrecision?.format(event.values[2])} μT")

            addEntry(1, 3, event.values, arrayOf("X", "Y", "Z"), arrayOf(
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

        magnetometerManager = getSystemService(SENSOR_SERVICE) as SensorManager
        magnetometer = magnetometerManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        sensorName.text = magnetometer?.name?.replaceFirstChar { it.uppercase() }
        sensorVendor.text = magnetometer?.vendor.toString().replaceFirstChar { it.uppercase() }
        sensorVersion.text = magnetometer?.version.toString()
        sensorPowerUsage.text = "${magnetometer?.power.toString()} mA"
        sensorResolution.text = "${convertSuperScript(magnetometer?.resolution.toString())} μT"
        sensorRange.text = "${convertSuperScript(magnetometer?.maximumRange.toString())} μT"

        createChart(10f, 3f, 0, legend = true, negative = true)
        startLiveChart()
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = "${getString(R.string.magnetometer_page)} :\n" +
            "${xValue.text}\n" +
            "${yValue.text}\n" +
            "${zValue.text}\n" +
            "${getString(R.string.sensorName)} ${sensorName.text}\n" +
            "${getString(R.string.sensorVendor)} ${sensorVendor.text}\n" +
            "${getString(R.string.sensorVersion)} ${sensorVersion.text}\n" +
            "${getString(R.string.sensorPowerUsage)} ${sensorPowerUsage.text}\n" +
            "${getString(R.string.sensorResolution)} ${sensorResolution.text}\n" +
            "${getString(R.string.sensorRange)} ${sensorRange.text}"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.magnetometer_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}
