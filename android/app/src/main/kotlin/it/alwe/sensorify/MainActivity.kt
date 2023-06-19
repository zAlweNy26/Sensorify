package it.alwe.sensorify

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import SensorActivity
import androidx.annotation.NonNull
import dev.flutter.pigeon.SensorsApi
import SensorsPlugin

class MainActivity: FlutterActivity() {
    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        SensorsApi.setUp(flutterEngine.dartExecutor.binaryMessenger, SensorsPlugin(baseContext))
    }
}
