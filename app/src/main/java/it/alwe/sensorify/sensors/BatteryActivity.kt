package it.alwe.sensorify.sensors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.BatteryManager
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.preference.PreferenceManager
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import it.alwe.sensorify.databinding.ActivityBatteryBinding

class BatteryActivity : BaseBlockActivity<ActivityBatteryBinding>() {
    private var intentFilter: IntentFilter? = null
    private var tempUnit: String? = "°C"
    private var expandToCollapse: AnimatedVectorDrawable? = null
    private var collapseToExpand: AnimatedVectorDrawable? = null
    private var expandOrCollapse: Boolean = false

    var btTemp = 0.0f
    var btVoltage = 0
    var btLevel = 0
    var btCapacity = 0
    var btCurrentCapacity = 0
    var btAmperage = 0
    var btWattage = 0.0f
    var btRealPerc = 0

    override fun setupViewBinding(inflater: LayoutInflater): ActivityBatteryBinding {
        return ActivityBatteryBinding.inflate(inflater)
    }

    override fun onResume() {
        super.onResume()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        tempUnit = sharedPrefs.getString("tempUnit", "°C")
        setBroadCastRegistered(true, batteryListener, intentFilter!!)
    }

    override fun onPause() {
        super.onPause()
        setBroadCastRegistered(false, batteryListener, intentFilter!!)
    }

    private val batteryListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager

            btLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            content.batteryPercentage.text = getString(R.string.battery_percentage, btLevel)
            //batteryPercentageValue.text = "$btLevel %"

            btCurrentCapacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) / 1000

            content.batteryCurrentCapacity.text = getString(R.string.battery_current_capacity, btCurrentCapacity)

            //batteryCurrentCapacityValue.text = if (btCurrentCapacity == 0) getString(R.string.unavailableValue) else "$btCurrentCapacity mAh"
            content.realPercentageLayout.visibility = if (btCurrentCapacity == 0) View.GONE else View.VISIBLE

            if (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING)
                content.batteryIcon.setImageResource(R.drawable.ic_battery_charging_60)
            else content.batteryIcon.setImageResource(R.drawable.ic_battery_60)

            //val adviceString = getString(R.string.battery_advice)
            //val beforeDiff = adviceString.substring(0, adviceString.indexOf('('))
            //val afterDiff = adviceString.substring(adviceString.indexOf(')') + 1, adviceString.length).replace("10", "<b>10</b>")
            btRealPerc = ((btCurrentCapacity * 100) / btCapacity)
            //val afterText = getString(R.string.battery_real_percentage).indexOf(':')
            //real_Percentage.text = "${getString(R.string.battery_real_percentage).substring(0, afterText + 1)} $btRealPerc %"
            //battery_Advice.text = Html.fromHtml("$beforeDiff<b>($btDiff)</b>$afterDiff")

            content.batteryRealPercentage.text = getString(R.string.battery_real_percentage, btRealPerc)
            content.batteryAdvice.text = HtmlCompat.fromHtml(getString(R.string.battery_advice, btLevel - btRealPerc), HtmlCompat.FROM_HTML_MODE_LEGACY)

            content.batteryTechnology.text = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)!!

            content.batteryChargeMode.text = when(intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
                BatteryManager.BATTERY_PLUGGED_AC -> resources.getStringArray(R.array.battery_charge_modes)[1]
                BatteryManager.BATTERY_PLUGGED_USB -> resources.getStringArray(R.array.battery_charge_modes)[2]
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> resources.getStringArray(R.array.battery_charge_modes)[3]
                else -> resources.getStringArray(R.array.battery_charge_modes)[0]
            }

            btTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0).toFloat() / 10
            when (tempUnit) {
                "°C" -> content.batteryTemperature.text = "${decimalPrecision?.format(btTemp)} $tempUnit"
                "°F" -> content.batteryTemperature.text = "${decimalPrecision?.format(((btTemp * 1.8) + 32))} $tempUnit"
                "K" -> content.batteryTemperature.text = "${decimalPrecision?.format((btTemp + 273.15))} $tempUnit"
            }

            btVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            content.batteryVoltage.text = "$btVoltage mV"

            btAmperage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000
            content.batteryAmperage.text = "$btAmperage mA"

            btWattage = (btVoltage.toFloat() * btAmperage.toFloat()) / (1000 * 1000)
            content.batteryWattage.text = "${decimalPrecision?.format(btWattage)} W"

            content.batteryHealth.text = when(intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)) {
                BatteryManager.BATTERY_HEALTH_GOOD -> resources.getStringArray(R.array.battery_health_types)[0]
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> resources.getStringArray(R.array.battery_health_types)[1]
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> resources.getStringArray(R.array.battery_health_types)[2]
                BatteryManager.BATTERY_HEALTH_COLD -> resources.getStringArray(R.array.battery_health_types)[3]
                BatteryManager.BATTERY_HEALTH_DEAD -> resources.getStringArray(R.array.battery_health_types)[4]
                else -> resources.getStringArray(R.array.battery_health_types)[5]
            }

            content.batteryState.text = when(intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                BatteryManager.BATTERY_STATUS_FULL -> resources.getStringArray(R.array.battery_states)[0]
                BatteryManager.BATTERY_STATUS_CHARGING -> resources.getStringArray(R.array.battery_states)[1]
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> resources.getStringArray(R.array.battery_states)[2]
                BatteryManager.BATTERY_STATUS_DISCHARGING -> resources.getStringArray(R.array.battery_states)[2]
                else -> resources.getStringArray(R.array.battery_states)[3]
            }
        }
    }

    private fun getBatteryCapacity(context: Context?): Int {
        val mPowerProfile: Any
        var capacity = 0.0
        try {
            mPowerProfile = Class.forName("com.android.internal.os.PowerProfile")
                .getConstructor(Context::class.java)
                .newInstance(context)
            capacity = Class
                .forName("com.android.internal.os.PowerProfile")
                .getMethod("getBatteryCapacity")
                .invoke(mPowerProfile) as Double
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return capacity.toInt()
    }

    // TODO : AGGIUNGERE TEMPO RIMANENTE STIMATO (https://github.com/greenhub-project/batteryhub/blob/master/app/src/main/java/com/hmatalonga/greenhub/models/Battery.java)

    override fun addCode() {
        content.batteryPercentage.text = getString(R.string.battery_percentage, 0)
        content.batteryRealPercentage.text = getString(R.string.battery_real_percentage, 0)
        content.batteryCurrentCapacity.text = getString(R.string.battery_current_capacity, 0)

        btCapacity = getBatteryCapacity(baseContext)
        content.batteryCapacity.text = "$btCapacity mAh"

        intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

        expandToCollapse = ContextCompat.getDrawable(baseContext, R.drawable.expand_to_collapse) as AnimatedVectorDrawable
        collapseToExpand = ContextCompat.getDrawable(baseContext, R.drawable.collapse_to_expand) as AnimatedVectorDrawable

        content.batteryButton.setOnClickListener {
            val batteryIntent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            startActivity(batteryIntent)
        }

        content.showGuideButton.setOnClickListener {
            val drawable = if (expandOrCollapse) collapseToExpand else expandToCollapse
            content.showGuideButton.setImageDrawable(drawable)
            drawable?.start()

            expandOrCollapse = !expandOrCollapse
            content.batteryGuideLayout.toggle()
        }
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = "${getString(R.string.battery_page)} :\n" +
            "${content.batteryPercentage.text}\n" +
            "${content.batteryRealPercentage.text}\n" +
            "${content.batteryCurrentCapacity.text}\n" +
            "${getString(R.string.battery_capacity)} ${content.batteryCapacity.text}\n" +
            "${getString(R.string.battery_technology)} ${content.batteryTechnology.text}\n" +
            "${getString(R.string.battery_state)} ${content.batteryState.text}\n" +
            "${getString(R.string.battery_charge_mode)} ${content.batteryChargeMode.text}\n" +
            "${getString(R.string.battery_temperature)} ${content.batteryTemperature.text}\n" +
            "${getString(R.string.battery_volt)} ${content.batteryVoltage.text}\n" +
            "${getString(R.string.battery_amps)} ${content.batteryAmperage.text}\n" +
            "${getString(R.string.battery_watt)} ${content.batteryWattage.text}\n" +
            "${getString(R.string.battery_health)} ${content.batteryHealth.text}\n"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.battery_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}