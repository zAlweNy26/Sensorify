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
import it.alwe.sensorify.databinding.ActivityLuminosityBinding

class LuminosityActivity : BaseBlockActivity<ActivityLuminosityBinding>() {
    private var lightSensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null

    override fun setupViewBinding(inflater: LayoutInflater): ActivityLuminosityBinding {
        return ActivityLuminosityBinding.inflate(inflater)
    }

    override fun onResume() {
        super.onResume()
        setListenerRegistered(true, lightSensorManager!!, lightSensorListener, lightSensor!!)
        //toggleThread(true)
    }

    override fun onPause() {
        super.onPause()
        setListenerRegistered(false, lightSensorManager!!, lightSensorListener, lightSensor!!)
        //toggleThread(false)
    }

    private val lightSensorListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            if (event.values[0] == 0.0f) content.sensorIcon.setImageResource(R.drawable.ic_lightbulb_off)
            else content.sensorIcon.setImageResource(R.drawable.ic_lightbulb_on)
            content.blockMainInfoText.text = getString(R.string.luminosityText, decimalPrecision?.format(event.values[0]))

            addEntry(2, 1, event.values, arrayOf("lx"), arrayOf("#" + Integer.toHexString(ContextCompat.getColor(applicationContext, R.color.monoAxisColor))))
        }
    }

    override fun addCode() {
        content.blockMainInfoText.text = getString(R.string.luminosityText, "0")

        lightSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = lightSensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)

        content.sensorName.text = lightSensor?.name?.replaceFirstChar { it.uppercase() }
        content.sensorVendor.text = lightSensor?.vendor.toString().replaceFirstChar { it.uppercase() }
        content.sensorVersion.text = lightSensor?.version.toString()
        content.sensorPowerUsage.text = "${lightSensor?.power.toString()} mA"
        content.sensorResolution.text = "${convertSuperScript(lightSensor?.resolution.toString())} lx"
        content.sensorRange.text = "${convertSuperScript(lightSensor?.maximumRange.toString())} lx"

        createChart(10f, 1f, 0, legend = false, negative = false)
        startLiveChart()
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = "${getString(R.string.luminosity_page)} :\n" +
            "${content.blockMainInfoText.text}\n" +
            "${getString(R.string.sensorName)} ${content.sensorName.text}\n" +
            "${getString(R.string.sensorVendor)} ${content.sensorVendor.text}\n" +
            "${getString(R.string.sensorVersion)} ${content.sensorVersion.text}\n" +
            "${getString(R.string.sensorPowerUsage)} ${content.sensorPowerUsage.text}\n" +
            "${getString(R.string.sensorResolution)} ${content.sensorResolution.text}\n" +
            "${getString(R.string.sensorRange)} ${content.sensorRange.text}"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.luminosity_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}
