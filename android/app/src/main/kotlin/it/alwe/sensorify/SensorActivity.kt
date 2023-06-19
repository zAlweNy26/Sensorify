import android.hardware.SensorManager
import android.hardware.SensorEventListener
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.util.Log
import io.flutter.plugin.common.EventChannel

class SensorActivity(private val sensorManager: SensorManager) : SensorEventListener, EventChannel.StreamHandler {
    private var mHeartRateSensor: Sensor? = null
    private var mEventSink: EventChannel.EventSink? = null
    var isSensorAvailable: Boolean = false

    init {
        Log.i("Initing SensorActivity", "Hurray!!!!!")
        val sensorsAvailable: List<Sensor>? = sensorManager.getSensorList(Sensor.TYPE_ALL)
        Log.i("All Sensors Available", sensorsAvailable.toString())

        if (sensorsAvailable != null && sensorsAvailable.any { it.type == Sensor.TYPE_LIGHT }) {
            isSensorAvailable = true
            mHeartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        } else {
            isSensorAvailable = false
            Log.e("Sensor Error:", "Device does not support Light Sensor")
        }
    }

    fun registerListeners() {
        if (mEventSink == null) return
        sensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun unRegisterListeners() {
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("AccuracyChanged", accuracy.toString())
    }

    override fun onSensorChanged(event: SensorEvent) {
        Log.d("SensorChanged", event.values[0].toString())
        mEventSink?.success(event.values[0])
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        mEventSink = events
        registerListeners()
    }

    override fun onCancel(arguments: Any?) {
        mEventSink = null
    }

}