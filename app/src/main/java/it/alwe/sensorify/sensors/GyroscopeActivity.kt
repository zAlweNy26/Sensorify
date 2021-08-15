package it.alwe.sensorify.sensors

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import kotlinx.android.synthetic.main.activity_gyroscope.*

class GyroscopeActivity : BaseBlockActivity() {
    private var gyroscopeManager: SensorManager? = null
    private var gyroscope: Sensor? = null

    override val contentView: Int
        get() = R.layout.activity_gyroscope

    override fun onResume() {
        super.onResume()
        setListenerRegistered(true, gyroscopeManager!!, gyroscopeListener, gyroscope!!)
        toggleThread(true)
    }

    override fun onPause() {
        super.onPause()
        setListenerRegistered(false, gyroscopeManager!!, gyroscopeListener, gyroscope!!)
        toggleThread(false)
    }

    private val gyroscopeListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {

            xValue.text = getString(R.string.valueX, "${decimalPrecision?.format(event.values[0])} rad/s")
            yValue.text = getString(R.string.valueY, "${decimalPrecision?.format(event.values[1])} rad/s")
            zValue.text = getString(R.string.valueZ, "${decimalPrecision?.format(event.values[2])} rad/s")

            addEntry(1, 3, event.values, arrayOf("X", "Y", "Z"), arrayOf(
                    "#" + Integer.toHexString(ContextCompat.getColor(applicationContext, R.color.xAxisColor)),
                    "#" + Integer.toHexString(ContextCompat.getColor(applicationContext, R.color.yAxisColor)),
                    "#" + Integer.toHexString(ContextCompat.getColor(applicationContext, R.color.zAxisColor))
                )
            )
        }
    }

    override fun addCode() {
        gyroscopeManager = getSystemService(SENSOR_SERVICE) as SensorManager
        gyroscope = gyroscopeManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        sensorName.text = gyroscope?.name?.replaceFirstChar { it.uppercase() }
        sensorVendor.text = gyroscope?.vendor.toString().replaceFirstChar { it.uppercase() }
        sensorVersion.text = gyroscope?.version.toString()
        sensorPowerUsage.text = "${gyroscope?.power.toString()} mA"
        sensorResolution.text = "${convertSuperScript(gyroscope?.resolution.toString())} rad/s"
        sensorRange.text = "${convertSuperScript(gyroscope?.maximumRange.toString())} rad/s"

        createChart(10f, 3f, 0, legend = true, negative = true)
        startLiveChart()
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = "${getString(R.string.gyroscope_page)} :\n" +
            "${xValue.text}\n" +
            "${yValue.text}\n" +
            "${zValue.text}\n" +
            "${getString(R.string.sensorName)} ${sensorName.text}\n" +
            "${getString(R.string.sensorVendor)} ${sensorVendor.text}\n" +
            "${getString(R.string.sensorVersion)} ${sensorVersion.text}\n" +
            "${getString(R.string.sensorPowerUsage)} ${sensorPowerUsage.text}\n" +
            "${getString(R.string.sensorResolution)} ${sensorResolution.text}\n" +
            "${getString(R.string.sensorRange)} ${sensorRange.text}"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.gyroscope_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}
