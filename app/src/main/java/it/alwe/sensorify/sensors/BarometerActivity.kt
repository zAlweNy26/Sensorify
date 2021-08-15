package it.alwe.sensorify.sensors

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.preference.PreferenceManager
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import kotlinx.android.synthetic.main.activity_barometer.*

class BarometerActivity : BaseBlockActivity() {
    private var barometerManager: SensorManager? = null
    private var barometer: Sensor? = null

    private var pressureUnit: String? = "hPa"
    private var distanceUnit: String? = "m"

    override val contentView: Int
        get() = R.layout.activity_barometer

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
                    pressureText.text = getString(R.string.pressureText, "${decimalPrecision?.format(event.values[0])} hPa")
                    sensorResolution.text = "${convertSuperScript(barometer?.resolution.toString())} hPa"
                    sensorRange.text = "${convertSuperScript(barometer?.maximumRange.toString())} hPa"
                }
                "atm" -> {
                    pressureText.text =  getString(R.string.pressureText, "${decimalPrecision?.format(event.values[0] / 1013)} atm")
                    sensorResolution.text = "${convertSuperScript((resolution!! / 1013).toString())} atm"
                    sensorRange.text = "${convertSuperScript((maximumRange!! / 1013).toString())} atm"
                }
                "torr" -> {
                    pressureText.text =  getString(R.string.pressureText, "${decimalPrecision?.format(event.values[0] / 1.333)} torr")
                    sensorResolution.text = "${convertSuperScript((resolution!! / 1.333).toString())} torr"
                    sensorRange.text = "${convertSuperScript((maximumRange!! / 1.333).toString())} torr"
                }
            }

            val altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, event.values[0])

            when (distanceUnit) {
                "m" -> altitudeValue.text = "${altitude.toInt()} m"
                "ft" -> altitudeValue.text = "${(altitude * 3.281).toInt()} ft"
            }
        }
    }

    override fun addCode() {
        pressureText.text = getString(R.string.pressureText, getString(R.string.unknownValue))

        barometerManager = getSystemService(SENSOR_SERVICE) as SensorManager
        barometer = barometerManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)

        sensorName.text = barometer?.name?.replaceFirstChar { it.uppercase() }
        sensorVendor.text = barometer?.vendor.toString().replaceFirstChar { it.uppercase() }
        sensorVersion.text = barometer?.version.toString()
        sensorPowerUsage.text = "${barometer?.power.toString()} mA"
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = "${getString(R.string.barometer_page)} :\n" +
            "${pressureText.text}\n" +
            "${getString(R.string.altitude)} ${altitudeValue.text}\n" +
            "${getString(R.string.sensorName)} ${sensorName.text}\n" +
            "${getString(R.string.sensorVendor)} ${sensorVendor.text}\n" +
            "${getString(R.string.sensorVersion)} ${sensorVersion.text}\n" +
            "${getString(R.string.sensorPowerUsage)} ${sensorPowerUsage.text}\n" +
            "${getString(R.string.sensorResolution)} ${sensorResolution.text}\n" +
            "${getString(R.string.sensorRange)} ${sensorRange.text}"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.barometer_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}