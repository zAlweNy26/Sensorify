package it.alwe.sensorify.sensors

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.LayoutInflater
import androidx.preference.PreferenceManager
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import it.alwe.sensorify.databinding.ActivityBarometerBinding

class BarometerActivity : BaseBlockActivity<ActivityBarometerBinding>() {
    private var barometerManager: SensorManager? = null
    private var barometer: Sensor? = null
    private var pressureUnit: String? = "hPa"
    private var distanceUnit: String? = "m"

    override fun setupViewBinding(inflater: LayoutInflater): ActivityBarometerBinding {
        return ActivityBarometerBinding.inflate(inflater)
    }

    override fun onResume() {
        super.onResume()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        pressureUnit = sharedPrefs.getString("pressureUnit", "hPa")
        distanceUnit = sharedPrefs.getString("distanceUnit", "m")
        setListenerRegistered(true, barometerManager!!, barometerListener, barometer!!)
    }

    override fun onPause() {
        super.onPause()
        setListenerRegistered(false, barometerManager!!, barometerListener, barometer!!)
    }

    private val barometerListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            val resolution = barometer?.resolution
            val maximumRange = barometer?.maximumRange

            when (pressureUnit) {
                "hPa" -> {
                    content.pressureText.text = getString(R.string.pressureText, "${decimalPrecision?.format(event.values[0])} hPa")
                    content.sensorResolution.text = "${convertSuperScript(barometer?.resolution.toString())} hPa"
                    content.sensorRange.text = "${convertSuperScript(barometer?.maximumRange.toString())} hPa"
                }
                "atm" -> {
                    content.pressureText.text =  getString(R.string.pressureText, "${decimalPrecision?.format(event.values[0] / 1013)} atm")
                    content.sensorResolution.text = "${convertSuperScript((resolution!! / 1013).toString())} atm"
                    content.sensorRange.text = "${convertSuperScript((maximumRange!! / 1013).toString())} atm"
                }
                "torr" -> {
                    content.pressureText.text =  getString(R.string.pressureText, "${decimalPrecision?.format(event.values[0] / 1.333)} torr")
                    content.sensorResolution.text = "${convertSuperScript((resolution!! / 1.333).toString())} torr"
                    content.sensorRange.text = "${convertSuperScript((maximumRange!! / 1.333).toString())} torr"
                }
            }

            val altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, event.values[0])

            when (distanceUnit) {
                "m" -> content.altitudeValue.text = "${altitude.toInt()} m"
                "ft" -> content.altitudeValue.text = "${(altitude * 3.281).toInt()} ft"
            }
        }
    }

    override fun addCode() {
        content.pressureText.text = getString(R.string.pressureText, getString(R.string.unknownValue))

        barometerManager = getSystemService(SENSOR_SERVICE) as SensorManager
        barometer = barometerManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)

        content.sensorName.text = barometer?.name?.replaceFirstChar { it.uppercase() }
        content.sensorVendor.text = barometer?.vendor.toString().replaceFirstChar { it.uppercase() }
        content.sensorVersion.text = barometer?.version.toString()
        content.sensorPowerUsage.text = "${barometer?.power.toString()} mA"
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = "${getString(R.string.barometer_page)} :\n" +
            "${content.pressureText.text}\n" +
            "${getString(R.string.altitude)} ${content.altitudeValue.text}\n" +
            "${getString(R.string.sensorName)} ${content.sensorName.text}\n" +
            "${getString(R.string.sensorVendor)} ${content.sensorVendor.text}\n" +
            "${getString(R.string.sensorVersion)} ${content.sensorVersion.text}\n" +
            "${getString(R.string.sensorPowerUsage)} ${content.sensorPowerUsage.text}\n" +
            "${getString(R.string.sensorResolution)} ${content.sensorResolution.text}\n" +
            "${getString(R.string.sensorRange)} ${content.sensorRange.text}"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.barometer_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}