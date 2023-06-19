import dev.flutter.pigeon.*
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import io.flutter.embedding.android.FlutterActivity

class SensorsPlugin(private var context: Context) : SensorsApi {

    private fun getBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    override fun getBattery(): Battery {
        val batteryLevel = getBatteryLevel().toLong()

        return Battery(
            batteryLevel,
            0, 0, 0, 0, 0, 0.toDouble(), 0.toDouble(), "0", BatteryCharge.NONE, BatteryHealth.UNKNOWN, BatteryState.UNKNOWN
        )
    }
}
