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
import it.alwe.sensorify.databinding.ActivityProximityBinding

class ProximityActivity : BaseBlockActivity<ActivityProximityBinding>() {
    private var proximityManager: SensorManager? = null
    private var proximity: Sensor? = null
    private var distanceUnit: String? = "m"

    override fun setupViewBinding(inflater: LayoutInflater): ActivityProximityBinding {
        return ActivityProximityBinding.inflate(inflater)
    }

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
                    content.sensorResolution.text = "${convertSuperScript(resolution)} cm"
                    content.sensorRange.text = "${convertSuperScript(maxRange)} cm"
                    content.blockMainInfoText.text = getString(R.string.proximityText, "${decimalPrecision?.format(event.values[0])} cm")
                }
                "ft" -> {
                    content.sensorResolution.text = "${convertSuperScript((resolution.toDouble() / 2.54).toString())} in"
                    content.sensorRange.text = "${convertSuperScript((maxRange.toDouble() / 2.54).toString())} in"
                    content.blockMainInfoText.text = getString(R.string.proximityText, "${decimalPrecision?.format(event.values[0] / 2.54)} in")
                }
            }
        }
    }

    override fun addCode() {
        content.blockMainInfoText.text = getString(R.string.proximityText, getString(R.string.unknownValue))

        proximityManager = getSystemService(SENSOR_SERVICE) as SensorManager
        proximity = proximityManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        content.sensorName.text = proximity?.name?.replaceFirstChar { it.uppercase() }
        content.sensorVendor.text = proximity?.vendor.toString().replaceFirstChar { it.uppercase() }
        content.sensorVersion.text = proximity?.version.toString()
        content.sensorPowerUsage.text = "${proximity?.power.toString()} mA"
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = "${getString(R.string.proximity_page)} :\n" +
            "${content.blockMainInfoText.text}\n" +
            "${getString(R.string.sensorName)} ${content.sensorName.text}\n" +
            "${getString(R.string.sensorVendor)} ${content.sensorVendor.text}\n" +
            "${getString(R.string.sensorVersion)} ${content.sensorVersion.text}\n" +
            "${getString(R.string.sensorPowerUsage)} ${content.sensorPowerUsage.text}\n" +
            "${getString(R.string.sensorResolution)} ${content.sensorResolution.text}\n" +
            "${getString(R.string.sensorRange)} ${content.sensorRange.text}"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.proximity_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}