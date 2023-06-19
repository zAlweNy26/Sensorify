package it.alwe.sensorify.sensors

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import it.alwe.sensorify.databinding.ActivityMagnetometerBinding

class MagnetometerActivity : BaseBlockActivity<ActivityMagnetometerBinding>() {
    private var magnetometerManager: SensorManager? = null
    private var magnetometer: Sensor? = null

    override fun setupViewBinding(inflater: LayoutInflater): ActivityMagnetometerBinding {
        return ActivityMagnetometerBinding.inflate(inflater)
    }

    override fun onResume() {
        super.onResume()
        setListenerRegistered(true, magnetometerManager!!, magnetometerListener, magnetometer!!)
        //toggleThread(true)
    }

    override fun onPause() {
        super.onPause()
        setListenerRegistered(false, magnetometerManager!!, magnetometerListener, magnetometer!!)
        //toggleThread(false)
    }

    private val magnetometerListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            content.xValue.text = getString(R.string.valueX, "${decimalPrecision?.format(event.values[0])} μT")
            content.yValue.text = getString(R.string.valueY, "${decimalPrecision?.format(event.values[1])} μT")
            content.zValue.text = getString(R.string.valueZ, "${decimalPrecision?.format(event.values[2])} μT")

            addEntry(1, 3, event.values, arrayOf("X", "Y", "Z"), arrayOf(
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

        magnetometerManager = getSystemService(SENSOR_SERVICE) as SensorManager
        magnetometer = magnetometerManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        content.sensorName.text = magnetometer?.name?.replaceFirstChar { it.uppercase() }
        content.sensorVendor.text = magnetometer?.vendor.toString().replaceFirstChar { it.uppercase() }
        content.sensorVersion.text = magnetometer?.version.toString()
        content.sensorPowerUsage.text = "${magnetometer?.power.toString()} mA"
        content.sensorResolution.text = "${convertSuperScript(magnetometer?.resolution.toString())} μT"
        content.sensorRange.text = "${convertSuperScript(magnetometer?.maximumRange.toString())} μT"

        createChart(10f, 3f, 0, legend = true, negative = true)
        startLiveChart()
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = "${getString(R.string.magnetometer_page)} :\n" +
            "${content.xValue.text}\n" +
            "${content.yValue.text}\n" +
            "${content.zValue.text}\n" +
            "${getString(R.string.sensorName)} ${content.sensorName.text}\n" +
            "${getString(R.string.sensorVendor)} ${content.sensorVendor.text}\n" +
            "${getString(R.string.sensorVersion)} ${content.sensorVersion.text}\n" +
            "${getString(R.string.sensorPowerUsage)} ${content.sensorPowerUsage.text}\n" +
            "${getString(R.string.sensorResolution)} ${content.sensorResolution.text}\n" +
            "${getString(R.string.sensorRange)} ${content.sensorRange.text}"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.magnetometer_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}
