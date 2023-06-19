package it.alwe.sensorify.sensors

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import it.alwe.sensorify.databinding.ActivityGravityBinding

class GravityActivity : BaseBlockActivity<ActivityGravityBinding>() {
    private var gravityManager: SensorManager? = null
    private var gravity: Sensor? = null
    private var distanceUnit: String? = "m"

    override fun setupViewBinding(inflater: LayoutInflater): ActivityGravityBinding {
        return ActivityGravityBinding.inflate(inflater)
    }

    override fun onResume() {
        super.onResume()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        distanceUnit = sharedPrefs.getString("distanceUnit", "m")
        setListenerRegistered(true, gravityManager!!, gravityListener, gravity!!)
        //toggleThread(true)
    }

    override fun onPause() {
        super.onPause()
        setListenerRegistered(false, gravityManager!!, gravityListener, gravity!!)
        //toggleThread(false)
    }

    private val gravityListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {

            val gravityArray = FloatArray(3)
            var resolution = gravity?.resolution.toString()
            var maxRange = gravity?.maximumRange.toString()

            when (distanceUnit) {
                "m" -> {
                    gravityArray[0] = event.values[0]
                    gravityArray[1] = event.values[1]
                    gravityArray[2] = event.values[2]
                }
                "ft" -> {
                    gravityArray[0] = event.values[0] * 3.281f
                    gravityArray[1] = event.values[1] * 3.281f
                    gravityArray[2] = event.values[2] * 3.281f

                    resolution = (resolution.toDouble() * 3.281).toString()
                    maxRange = (maxRange.toDouble() * 3.281).toString()
                }
            }

            content.xValue.text = getString(R.string.valueX, "${decimalPrecision?.format(gravityArray[0])} $distanceUnit/s²")
            content.yValue.text = getString(R.string.valueY, "${decimalPrecision?.format(gravityArray[1])} $distanceUnit/s²")
            content.zValue.text = getString(R.string.valueZ, "${decimalPrecision?.format(gravityArray[2])} $distanceUnit/s²")

            content.sensorResolution.text = "${convertSuperScript(resolution)} $distanceUnit/s²"
            content.sensorRange.text = "${convertSuperScript(maxRange)} $distanceUnit/s²"

            addEntry(1, 3, gravityArray, arrayOf("X", "Y", "Z"), arrayOf(
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

        gravityManager = getSystemService(SENSOR_SERVICE) as SensorManager
        gravity = gravityManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)

        content.sensorName.text = gravity?.name?.replaceFirstChar { it.uppercase() }
        content.sensorVendor.text = gravity?.vendor.toString().replaceFirstChar { it.uppercase() }
        content.sensorVersion.text = gravity?.version.toString()
        content.sensorPowerUsage.text = "${gravity?.power.toString()} mA"

        createChart(10f, 3f, 0, legend = true, negative = true)
        startLiveChart()
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val sb = StringBuilder()
        sb.append(getString(R.string.gravity_page) + " :\n")
        sb.append("${content.xValue.text}\n" +
            "${content.yValue.text}\n" +
            "${content.zValue.text}\n" +
            "${getString(R.string.sensorName)} ${content.sensorName.text}\n" +
            "${getString(R.string.sensorVendor)} ${content.sensorVendor.text}\n" +
            "${getString(R.string.sensorVersion)} ${content.sensorVersion.text}\n" +
            "${getString(R.string.sensorPowerUsage)} ${content.sensorPowerUsage.text}\n" +
            "${getString(R.string.sensorResolution)} ${content.sensorResolution.text}\n" +
            "${getString(R.string.sensorRange)} ${content.sensorRange.text}")
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.gravity_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, sb.toString())
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}
