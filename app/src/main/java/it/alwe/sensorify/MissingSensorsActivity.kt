package it.alwe.sensorify

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_missing_sensors.*

class MissingSensorsActivity : CommonActivity() {
    private var missingSensors: MutableList<MissingSensor> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_missing_sensors)

        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow)

        val oSM = baseContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val supportedSensors: List<Sensor> = oSM.getSensorList(Sensor.TYPE_ALL)
        val supportedSensorsList: MutableList<Int> = mutableListOf()
        for (i in supportedSensors.indices) supportedSensorsList.add(supportedSensors[i].type)

        val sensorsMap: HashMap<Int, MissingSensor> = hashMapOf(
            Sensor.TYPE_LINEAR_ACCELERATION to MissingSensor(getString(R.string.linear_acceleration_page), getString(R.string.linearAccelerationInfo), R.drawable.ic_line),
            Sensor.TYPE_ACCELEROMETER to MissingSensor(getString(R.string.accelerometer_page), getString(R.string.accelerometerInfo), R.drawable.ic_accelerometer),
            Sensor.TYPE_MAGNETIC_FIELD to MissingSensor(getString(R.string.magnetometer_page), getString(R.string.magnetometerInfo), R.drawable.ic_magnet),
            Sensor.TYPE_GYROSCOPE to MissingSensor(getString(R.string.gyroscope_page), getString(R.string.gyroscopeInfo), R.drawable.ic_gyroscope),
            Sensor.TYPE_LIGHT to MissingSensor(getString(R.string.luminosity_page), getString(R.string.luminosityInfo), R.drawable.ic_lightbulb_off),
            Sensor.TYPE_PRESSURE to MissingSensor(getString(R.string.barometer_page), getString(R.string.barometerInfo), R.drawable.ic_gauge),
            Sensor.TYPE_PROXIMITY to MissingSensor(getString(R.string.proximity_page), getString(R.string.proximityInfo), R.drawable.ic_ruler),
            Sensor.TYPE_STEP_DETECTOR to MissingSensor(getString(R.string.pedometer_page), getString(R.string.pedometerInfo), R.drawable.ic_pedometer),
            Sensor.TYPE_AMBIENT_TEMPERATURE to MissingSensor(getString(R.string.temperature_page), getString(R.string.tempInfo), R.drawable.ic_thermometer),
            Sensor.TYPE_RELATIVE_HUMIDITY to MissingSensor(getString(R.string.humidity_page), getString(R.string.humidityInfo), R.drawable.ic_humidity),
            Sensor.TYPE_ROTATION_VECTOR to MissingSensor(getString(R.string.rotation_vectors_page), getString(R.string.rotationVectorsInfo), R.drawable.ic_rotation),
            Sensor.TYPE_GRAVITY to MissingSensor(getString(R.string.gravity_page), getString(R.string.gravityInfo), R.drawable.ic_globe),
        )

        (sensorsMap.keys - supportedSensorsList.toSet()).forEach {
            sensorsMap[it]?.let { it1 -> missingSensors.add(it1) }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = MissingListAdapter(baseContext, missingSensors)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}