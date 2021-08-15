package it.alwe.sensorify.sensors

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.preference.PreferenceManager
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import kotlinx.android.synthetic.main.activity_proximity.*

class ProximityActivity : BaseBlockActivity() {
    private var proximityManager: SensorManager? = null
    private var proximity: Sensor? = null
    private var distanceUnit: String? = "m"

    override val contentView: Int
        get() = R.layout.activity_proximity

    override fun onResume() {
        super.onResume()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        distanceUnit = sharedPrefs.getString("distanceUnit", "m")
        setListenerRegistered(true, proximityManager!!, proximityListener, proximity!!)
    }

    override fun onPause() {
        super.onPause()
        setListenerRegistered(false, proximityManager!!, proximityListener, proximity!!)
    }

    private val proximityListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            val resolution = proximity?.resolution.toString()
            val maxRange = proximity?.maximumRange.toString()

            when (distanceUnit) {
                "m" -> {
                    sensorResolution.text = "${convertSuperScript(resolution)} cm"
                    sensorRange.text = "${convertSuperScript(maxRange)} cm"
                    blockMainInfoText.text = getString(R.string.proximityText, "${decimalPrecision?.format(event.values[0])} cm")
                }
                "ft" -> {
                    sensorResolution.text = "${convertSuperScript((resolution.toDouble() / 2.54).toString())} in"
                    sensorRange.text = "${convertSuperScript((maxRange.toDouble() / 2.54).toString())} in"
                    blockMainInfoText.text = getString(R.string.proximityText, "${decimalPrecision?.format(event.values[0] / 2.54)} in")
                }
            }
        }
    }

    override fun addCode() {
        blockMainInfoText.text = getString(R.string.proximityText, getString(R.string.unknownValue))

        proximityManager = getSystemService(SENSOR_SERVICE) as SensorManager
        proximity = proximityManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        sensorName.text = proximity?.name?.replaceFirstChar { it.uppercase() }
        sensorVendor.text = proximity?.vendor.toString().replaceFirstChar { it.uppercase() }
        sensorVersion.text = proximity?.version.toString()
        sensorPowerUsage.text = "${proximity?.power.toString()} mA"
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = "${getString(R.string.proximity_page)} :\n" +
            "${blockMainInfoText.text}\n" +
            "${getString(R.string.sensorName)} ${sensorName.text}\n" +
            "${getString(R.string.sensorVendor)} ${sensorVendor.text}\n" +
            "${getString(R.string.sensorVersion)} ${sensorVersion.text}\n" +
            "${getString(R.string.sensorPowerUsage)} ${sensorPowerUsage.text}\n" +
            "${getString(R.string.sensorResolution)} ${sensorResolution.text}\n" +
            "${getString(R.string.sensorRange)} ${sensorRange.text}"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.proximity_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}