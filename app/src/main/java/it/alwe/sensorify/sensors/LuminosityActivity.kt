package it.alwe.sensorify.sensors

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import kotlinx.android.synthetic.main.activity_luminosity.*

class LuminosityActivity : BaseBlockActivity() {
    private var lightSensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null

    override val contentView: Int
        get() = R.layout.activity_luminosity

    override fun onResume() {
        super.onResume()
        setListenerRegistered(true, lightSensorManager!!, lightSensorListener, lightSensor!!)
        toggleThread(true)
    }

    override fun onPause() {
        super.onPause()
        setListenerRegistered(false, lightSensorManager!!, lightSensorListener, lightSensor!!)
        toggleThread(false)
    }

    private val lightSensorListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            if (event.values[0] == 0.0f) sensorIcon.setImageResource(R.drawable.ic_lightbulb_off)
            else sensorIcon.setImageResource(R.drawable.ic_lightbulb_on)
            blockMainInfoText.text = getString(R.string.luminosityText, decimalPrecision?.format(event.values[0]))

            addEntry(2, 1, event.values, arrayOf("lx"), arrayOf("#" + Integer.toHexString(ContextCompat.getColor(applicationContext, R.color.monoAxisColor))))
        }
    }

    override fun addCode() {
        blockMainInfoText.text = getString(R.string.luminosityText, "0")

        lightSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = lightSensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)

        sensorName.text = lightSensor?.name?.replaceFirstChar { it.uppercase() }
        sensorVendor.text = lightSensor?.vendor.toString().replaceFirstChar { it.uppercase() }
        sensorVersion.text = lightSensor?.version.toString()
        sensorPowerUsage.text = "${lightSensor?.power.toString()} mA"
        sensorResolution.text = "${convertSuperScript(lightSensor?.resolution.toString())} lx"
        sensorRange.text = "${convertSuperScript(lightSensor?.maximumRange.toString())} lx"

        createChart(10f, 1f, 0, legend = false, negative = false)
        startLiveChart()
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = "${getString(R.string.luminosity_page)} :\n" +
            "${blockMainInfoText.text}\n" +
            "${getString(R.string.sensorName)} ${sensorName.text}\n" +
            "${getString(R.string.sensorVendor)} ${sensorVendor.text}\n" +
            "${getString(R.string.sensorVersion)} ${sensorVersion.text}\n" +
            "${getString(R.string.sensorPowerUsage)} ${sensorPowerUsage.text}\n" +
            "${getString(R.string.sensorResolution)} ${sensorResolution.text}\n" +
            "${getString(R.string.sensorRange)} ${sensorRange.text}"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.luminosity_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}
