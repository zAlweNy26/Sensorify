package it.alwe.sensorify.sensors

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.LayoutInflater
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import it.alwe.sensorify.databinding.ActivityHumidityBinding
import kotlin.math.exp

class HumidityActivity : BaseBlockActivity<ActivityHumidityBinding>() {
    private var tempHumidityManager: SensorManager? = null
    private var temperatureSensor: Sensor? = null
    private var humiditySensor: Sensor? = null
    private var hasTemp = false
    private var tempValue = 0f
    private var humValue = 0f

    override fun setupViewBinding(inflater: LayoutInflater): ActivityHumidityBinding {
        return ActivityHumidityBinding.inflate(inflater)
    }

    override fun onResume() {
        super.onResume()
        tempHumidityManager?.registerListener(tempHumidityListener, humiditySensor, SensorManager.SENSOR_DELAY_NORMAL)
        if (hasTemp) tempHumidityManager?.registerListener(tempHumidityListener, temperatureSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        tempHumidityManager?.unregisterListener(tempHumidityListener)
    }

    private val tempHumidityListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor == temperatureSensor) tempValue = event.values[0]
            else if (event.sensor == humiditySensor) humValue = event.values[0]

            content.humidityText.text = getString(R.string.humidityText, "$humValue %")

            if (tempValue != 0f && humValue != 0f) {
                val absHumidity = 216.7 * (humValue / 100.0 * 6.112 * exp(17.62 * tempValue / (243.12 + tempValue)) / (273.15 + tempValue))
                content.absHumidityText.text = getString(R.string.absHumidityText, "${decimalPrecision?.format(absHumidity)} g/m³")
            } else content.absHumidityText.text = getString(R.string.absHumidityText, "0 g/m³")
        }
    }

    override fun addCode() {
        content.humidityText.text = getString(R.string.humidityText, getString(R.string.unknownValue))
        content.absHumidityText.text = getString(R.string.absHumidityText, getString(R.string.unknownValue))

        tempHumidityManager = getSystemService(SENSOR_SERVICE) as SensorManager
        humiditySensor = tempHumidityManager?.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)

        content.sensorName.text = humiditySensor?.name?.replaceFirstChar { it.uppercase() }
        content.sensorVendor.text = humiditySensor?.vendor.toString().replaceFirstChar { it.uppercase() }
        content.sensorVersion.text = humiditySensor?.version.toString()
        content.sensorPowerUsage.text = "${humiditySensor?.power.toString()} mA"
        content.sensorResolution.text = "${convertSuperScript(humiditySensor?.resolution.toString())} %"
        content.sensorRange.text = "${convertSuperScript(humiditySensor?.maximumRange.toString())} %"

        if (tempHumidityManager?.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
            hasTemp = true
            temperatureSensor = tempHumidityManager?.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        }
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val sb = StringBuilder()
        sb.append(getString(R.string.humidity_page) + " :\n")
        sb.append("${content.humidityText.text}\n" +
            "${content.absHumidityText.text}\n" +
            "${getString(R.string.sensorName)} ${content.sensorName.text}\n" +
            "${getString(R.string.sensorVendor)} ${content.sensorVendor.text}\n" +
            "${getString(R.string.sensorVersion)} ${content.sensorVersion.text}\n" +
            "${getString(R.string.sensorPowerUsage)} ${content.sensorPowerUsage.text}\n" +
            "${getString(R.string.sensorResolution)} ${content.sensorResolution.text}\n" +
            "${getString(R.string.sensorRange)} ${content.sensorRange.text}")
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.humidity_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, sb.toString())
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}
