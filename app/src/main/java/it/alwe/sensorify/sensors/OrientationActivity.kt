package it.alwe.sensorify.sensors

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import kotlinx.android.synthetic.main.activity_orientation.*

class OrientationActivity : BaseBlockActivity() {
    private var orientationManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var orientationValues = FloatArray(3)
    private var accelleration = FloatArray(3)
    private var magneticfield = FloatArray(3)
    private var rMatrix = FloatArray(9)
    private var iMatrix = FloatArray(9)
    private var angleUnit: String? = "°"

    override val contentView: Int
        get() = R.layout.activity_orientation

    override fun onResume() {
        super.onResume()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        angleUnit = sharedPrefs.getString("angleUnit", "°")
        orientationManager?.registerListener(orientationListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        orientationManager?.registerListener(orientationListener, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)
        toggleThread(true)
    }

    override fun onPause() {
        super.onPause()
        orientationManager?.unregisterListener(orientationListener)
        toggleThread(false)
    }

    private val orientationListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {

            if (event.sensor == accelerometer) accelleration = event.values
            else if (event.sensor == magnetometer) magneticfield = event.values

            if (SensorManager.getRotationMatrix(rMatrix, iMatrix, accelleration, magneticfield)) {
                SensorManager.getOrientation(rMatrix, orientationValues)

                val orientationArray = FloatArray(3)
                orientationArray[0] = (Math.toDegrees(orientationValues[0].toDouble()) + 360  % 360).toFloat()
                orientationArray[1] = (Math.toDegrees(orientationValues[1].toDouble()) + 360  % 360).toFloat()
                orientationArray[2] = (Math.toDegrees(orientationValues[2].toDouble()) + 360  % 360).toFloat()

                addEntry(1, 3, orientationArray, arrayOf("Azimuth", "Pitch", "Roll"), arrayOf(
                        "#" + Integer.toHexString(ContextCompat.getColor(applicationContext, R.color.xAxisColor)),
                        "#" + Integer.toHexString(ContextCompat.getColor(applicationContext, R.color.yAxisColor)),
                        "#" + Integer.toHexString(ContextCompat.getColor(applicationContext, R.color.zAxisColor))
                    )
                )

                val azimuth = (Math.toDegrees(orientationValues[0].toDouble()) + 360  % 360)
                var pointText = ""

                compassImage.rotation = -azimuth.toFloat()

                if (azimuth >= 350 || azimuth <= 10) pointText = "N"
                else if (azimuth in 281f..349f) pointText = "NW"
                else if (azimuth in 261f..280f) pointText = "W"
                else if (azimuth in 191f..260f) pointText = "SW"
                else if (azimuth in 171f..190f) pointText = "S"
                else if (azimuth in 101f..170f) pointText = "SE"
                else if (azimuth in 81f..100f) pointText = "E"
                else if (azimuth in 11f..80f) pointText = "NE"

                compassValue.text = "${azimuth.toInt()} ° $pointText"

                when (angleUnit) {
                    "°" -> {
                        xValue.text = getString(R.string.azimuth, "${decimalPrecision?.format(orientationArray[0])} °")
                        yValue.text = getString(R.string.pitch, "${decimalPrecision?.format(orientationArray[1])} °")
                        zValue.text = getString(R.string.roll, "${decimalPrecision?.format(orientationArray[2])} °")
                    }
                    "rad" -> {
                        xValue.text = getString(R.string.azimuth, "${decimalPrecision?.format(orientationArray[0] * Math.PI / 180)} rad")
                        yValue.text = getString(R.string.pitch, "${decimalPrecision?.format(orientationArray[1] * Math.PI / 180)} rad")
                        zValue.text = getString(R.string.roll, "${decimalPrecision?.format(orientationArray[2] * Math.PI / 180)} rad")
                    }
                }
            }
        }
    }

    override fun addCode() {
        xValue.text = getString(R.string.azimuth, getString(R.string.unknownValue))
        yValue.text = getString(R.string.pitch, getString(R.string.unknownValue))
        zValue.text = getString(R.string.roll, getString(R.string.unknownValue))

        orientationManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = orientationManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = orientationManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        createChart(10f, 3f, 0, legend = true, negative = true)
        startLiveChart()
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = "${getString(R.string.orientation_page)} :\n" +
            "${xValue.text}\n" +
            "${yValue.text}\n" +
            "${zValue.text}\n" +
            "${getString(R.string.compassTitle)} : ${compassValue.text}\n"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.orientation_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}
