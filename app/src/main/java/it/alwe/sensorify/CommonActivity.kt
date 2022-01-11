package it.alwe.sensorify

import android.content.*
import android.content.pm.ActivityInfo
import android.os.BatteryManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import java.util.*

abstract class CommonActivity : AppCompatActivity() {
    private var sharedPrefs: SharedPreferences? = null
    private var themeValue: Int? = null
    private var batteryValue: Boolean? = false
    private var timeValue: Boolean? = false
    private var screenValue: Boolean? = null
    private var rotationValue: Int? = null
    private var batteryRegistered = false
    private var timeRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(baseContext)

        rotationValue = sharedPrefs?.getInt("rotationMode", ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
        this.requestedOrientation = rotationValue!!

        screenValue = sharedPrefs?.getBoolean("screenMode", false)
        window.decorView.findViewById<View>(android.R.id.content).keepScreenOn = screenValue!!

        themeValue = sharedPrefs?.getInt("themeMode", R.style.LightTheme)
        batteryValue = sharedPrefs?.getBoolean("batteryMode", false)
        timeValue = sharedPrefs?.getBoolean("timeMode", false)

        super.onCreate(savedInstanceState)

        if (sharedPrefs?.getBoolean("firstOpen", true)!!) {
            FirstOpenDialog().show(supportFragmentManager, "FirstOpenDialog")

            sharedPrefs?.edit()?.putBoolean("firstOpen", false)?.apply()
            sharedPrefs?.edit()?.putString("language", Locale.getDefault().language)?.apply()
        }

        /*language = sharedPrefs?.getString("language", Locale.getDefault().language)
        Locale.setDefault(Locale(language!!))
        val config = applicationContext.resources.configuration
        config.setLocale(Locale(language!!))

        config.setLayoutDirection(Locale(language!!))
        if (Build.VERSION.SDK_INT >= 25)  applicationContext.createConfigurationContext(config)
        else applicationContext.resources.updateConfiguration(config, applicationContext.resources.displayMetrics)*/

        setTheme(themeValue!!)

        if (!batteryRegistered && batteryValue!!) {
            this.registerReceiver(batteryListener, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryRegistered = true
        }

        if (!timeRegistered && timeValue!!) {
            val timeIntent = IntentFilter()
            timeIntent.addAction(Intent.ACTION_TIME_TICK)
            timeIntent.addAction(Intent.ACTION_TIMEZONE_CHANGED)
            timeIntent.addAction(Intent.ACTION_TIME_CHANGED)

            this.registerReceiver(timeListener, timeIntent)
            timeRegistered = true
        }
    }

    private val batteryListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

            if (batteryValue!! && batteryLevel <= 30 && themeValue != R.style.DarkTheme) {
                sharedPrefs?.edit()?.putInt("themeMode", R.style.DarkTheme)?.apply()
                recreate()
            } else if (batteryValue!! && batteryLevel > 30 && themeValue == R.style.DarkTheme) {
                sharedPrefs?.edit()?.putInt("themeMode", R.style.LightTheme)?.apply()
                recreate()
            }
        }
    }

    private val timeListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            if (action == Intent.ACTION_TIME_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED) {
                val currentTime = (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 100) + Calendar.getInstance().get(Calendar.MINUTE)

                if (timeValue!! && ((currentTime >= 2000) || (currentTime < 800)) && themeValue != R.style.DarkTheme) {
                    sharedPrefs?.edit()?.putInt("themeMode", R.style.DarkTheme)?.apply()
                    recreate()
                } else if (timeValue!! && !((currentTime >= 2000) || (currentTime < 600)) && themeValue == R.style.DarkTheme) {
                    sharedPrefs?.edit()?.putInt("themeMode", R.style.LightTheme)?.apply()
                    recreate()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        themeValue = sharedPrefs?.getInt("themeMode", R.style.LightTheme)
        batteryValue = sharedPrefs?.getBoolean("batteryMode", false)
        timeValue = sharedPrefs?.getBoolean("timeMode", false)
        var batteryMode = false
        var timeMode = false

        if (batteryValue!!) {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            batteryMode = batteryLevel <= 30
        }
        if (timeValue!!) {
            val currentTime = (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 100) + Calendar.getInstance().get(Calendar.MINUTE)
            timeMode = (currentTime >= 2000) || (currentTime < 600)
        }

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES ||
            (batteryValue!! && batteryMode) || (timeValue!! && timeMode)) {
            val prefsEditor = sharedPrefs?.edit()
            prefsEditor?.putInt("themeMode", R.style.DarkTheme)?.apply()
            if (themeValue != R.style.DarkTheme) recreate()
        } /*else if ((batteryValue!! && !batteryMode) || (timeValue!! && !timeMode) && themeValue == R.style.DarkTheme) {
            var prefsEditor = sharedPrefs?.edit()
            prefsEditor?.putInt("themeMode", R.style.LightTheme)?.commit()
            recreate()
        }*/
    }

    override fun onDestroy() {
        super.onDestroy()
        if (batteryRegistered) {
            this.unregisterReceiver(batteryListener)
            batteryRegistered = false
        }
        if (timeRegistered) {
            this.unregisterReceiver(timeListener)
            timeRegistered = false
        }
    }
}