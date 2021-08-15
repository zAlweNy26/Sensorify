package it.alwe.sensorify.sensors

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.preference.PreferenceManager
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import kotlinx.android.synthetic.main.activity_temp.*
import kotlin.math.ln

class TempActivity : BaseBlockActivity() {
    private var tempHumidityManager: SensorManager? = null
    private var temperatureSensor: Sensor? = null
    private var humiditySensor: Sensor? = null
    private var tempUnit: String? = "°C"
    private var hasHumidity = false
    private var tempValue = 0f
    private var humidityValue = 0f

    override val contentView: Int
        get() = R.layout.activity_temp

    override fun onResume() {
        super.onResume()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        tempUnit = sharedPrefs.getString("tempUnit", "°C")
        tempHumidityManager?.registerListener(tempHumidityListener, temperatureSensor, SensorManager.SENSOR_DELAY_NORMAL)
        if (hasHumidity) tempHumidityManager?.registerListener(tempHumidityListener, humiditySensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        tempHumidityManager?.unregisterListener(tempHumidityListener)
    }

    private val tempHumidityListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor == temperatureSensor) tempValue = event.values[0]
            else if (event.sensor == humiditySensor) humidityValue = event.values[0]

            when (tempUnit) {
                "°C" -> tempText.text = getString(R.string.tempText, "${decimalPrecision?.format(tempValue)} $tempUnit")
                "°F" -> tempText.text = getString(R.string.tempText, "${decimalPrecision?.format(((tempValue * 1.8) + 32))} $tempUnit")
                "K" -> tempText.text = getString(R.string.tempText, "${decimalPrecision?.format((tempValue + 273.15))} $tempUnit")
            }

            if (tempValue != 0f && humidityValue != 0f) {
                val h = ln(humidityValue / 100.0) + (17.62 * tempValue) / (243.12 + tempValue)
                val dewPoint = 243.12 * h / (17.62 - h)

                when (tempUnit) {
                    "°C" -> dewPointText.text = getString(R.string.dewPointText, "${decimalPrecision?.format(dewPoint)} $tempUnit")
                    "°F" -> dewPointText.text = getString(R.string.dewPointText, "${decimalPrecision?.format(((dewPoint * 1.8) + 32))} $tempUnit")
                    "K" -> dewPointText.text = getString(R.string.dewPointText, "${decimalPrecision?.format(((dewPoint + 273.15)))} $tempUnit")
                }
            } else dewPointText.text = getString(R.string.dewPointText, "0 $tempUnit")
        }
    }

    override fun addCode() {
        tempText.text = getString(R.string.tempText, getString(R.string.unknownValue))
        dewPointText.text = getString(R.string.dewPointText, getString(R.string.unknownValue))

        tempHumidityManager = getSystemService(SENSOR_SERVICE) as SensorManager
        temperatureSensor = tempHumidityManager?.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

        sensorName.text = temperatureSensor?.name?.replaceFirstChar { it.uppercase() }
        sensorVendor.text = temperatureSensor?.vendor.toString().replaceFirstChar { it.uppercase() }
        sensorVersion.text = temperatureSensor?.version.toString()
        sensorPowerUsage.text = "${temperatureSensor?.power.toString()} mA"
        sensorResolution.text = "${convertSuperScript(temperatureSensor?.resolution.toString())} °C"
        sensorRange.text = "${convertSuperScript(temperatureSensor?.maximumRange.toString())} °C"

        if (tempHumidityManager?.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) != null) {
            hasHumidity = true
            humiditySensor = tempHumidityManager?.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)
        }
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val sb = StringBuilder()
        sb.append(getString(R.string.temperature_page) + " :\n" +
            "${tempText.text}\n" +
            "${dewPointText.text}\n" +
            "${getString(R.string.sensorName)} ${sensorName.text}\n" +
            "${getString(R.string.sensorVendor)} ${sensorVendor.text}\n" +
            "${getString(R.string.sensorVersion)} ${sensorVersion.text}\n" +
            "${getString(R.string.sensorPowerUsage)} ${sensorPowerUsage.text}\n" +
            "${getString(R.string.sensorResolution)} ${sensorResolution.text}\n" +
            "${getString(R.string.sensorRange)} ${sensorRange.text}")
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.temperature_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, sb.toString())
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}