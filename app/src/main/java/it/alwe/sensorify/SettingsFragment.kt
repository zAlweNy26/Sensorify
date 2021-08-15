package it.alwe.sensorify

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.text.HtmlCompat
import androidx.preference.*
import com.yariksoffice.lingver.Lingver
import java.io.File
import java.util.*

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var sharedPrefs: SharedPreferences? = null
    private var prefsEditor: SharedPreferences.Editor? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_prefs, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

        prefsEditor = sharedPrefs?.edit()

        super.onCreate(savedInstanceState)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        var numFiles = 0
        var dir: File? = null
        // TODO : PERMETTERE LA CANCELLAZIONE DEGLI SCREENSHOT FATTI
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            //dir = File(context?.getExternalFilesDir(null)?.absolutePath.toString() + "/Screenshots")
            dir = File(Environment.DIRECTORY_PICTURES + File.pathSeparator + "Sensorify")
            if (!dir.exists()) dir.mkdirs()
            for (file in dir.listFiles()!!) if (file.isFile) numFiles += 1
        }

        if (preference?.key == "delete_screenshots" && numFiles > 0) {
            val messageText = getString(R.string.settings_delete_screenshots_question)
            val alertBuilder = AlertDialog.Builder(context,
                if (sharedPrefs?.getInt("themeMode", R.style.LightTheme) == R.style.LightTheme) R.style.lightDialogAlertTheme else R.style.darkDialogAlertTheme)
                .setTitle(getString(R.string.settings_delete_screenshots))
                .setMessage(getString(R.string.settings_delete_screenshots_question))
                .setIcon(R.drawable.ic_delete)
                .setPositiveButton(android.R.string.ok) { dialog, whichButton -> for (file in dir?.listFiles()!!) file.delete() }
                .setNegativeButton(android.R.string.cancel, null)
            val alertDialog = alertBuilder.show()
            val messageView = alertDialog.findViewById(android.R.id.message) as TextView
            if (Build.VERSION.SDK_INT >= 24) messageView.text = HtmlCompat.fromHtml(messageText.replace("0", "<b>${numFiles}</b>"), HtmlCompat.FROM_HTML_MODE_LEGACY)
            else messageView.text = HtmlCompat.fromHtml(messageText.replace("0", "<b>${numFiles}</b>"), HtmlCompat.FROM_HTML_MODE_LEGACY)
            messageView.gravity = Gravity.CENTER
        } else if (preference?.key == "delete_screenshots" && numFiles == 0) {
            val alertBuilder = AlertDialog.Builder(context,
                if (sharedPrefs?.getInt("themeMode", R.style.LightTheme) == R.style.LightTheme) R.style.lightDialogAlertTheme else R.style.darkDialogAlertTheme)
                .setTitle(getString(R.string.settings_delete_screenshots_title))
                .setMessage(getString(R.string.settings_screenshots_no_files))
                .setIcon(R.drawable.ic_delete)
                .setPositiveButton(android.R.string.ok, null)
            val alertDialog = alertBuilder.show()
            val messageView = alertDialog.findViewById(android.R.id.message) as TextView
            messageView.gravity = Gravity.CENTER
        }

        return super.onPreferenceTreeClick(preference)
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        if (parentFragmentManager.findFragmentByTag("NumberPickerDialog") != null) return
        if (preference is NumberPickerPreference) {
            val dialog = NumberPickerPreferenceDialog.newInstance(preference.key)
            dialog.setTargetFragment(this, 0)
            dialog.show(parentFragmentManager, "NumberPickerDialog")
        } else super.onDisplayPreferenceDialog(preference)
    }

    override fun onResume() {
        super.onResume()

        findPreference<SwitchPreferenceCompat>("pref_theme")?.isChecked = sharedPrefs?.getInt("themeMode", R.style.LightTheme) == R.style.DarkTheme

        findPreference<ListPreference>("pref_temp")?.summary = findPreference<ListPreference>("pref_temp")?.entry

        findPreference<ListPreference>("pref_distance")?.summary = findPreference<ListPreference>("pref_distance")?.entry

        findPreference<ListPreference>("pref_pressure")?.summary = findPreference<ListPreference>("pref_pressure")?.entry

        findPreference<ListPreference>("pref_angle")?.summary = findPreference<ListPreference>("pref_angle")?.entry

        findPreference<ListPreference>("pref_language")?.value = sharedPrefs?.getString("language", Locale.getDefault().language)

        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val batteryManager = context?.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val currentTime = (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 100) + Calendar.getInstance().get(Calendar.MINUTE)

        if (key.equals("pref_theme")) {
            val themeValue = sharedPreferences?.getBoolean(key, false)!!
            prefsEditor?.putInt("themeMode", if (themeValue) R.style.DarkTheme else R.style.LightTheme)?.commit()
            findPreference<SwitchPreferenceCompat>("pref_theme_time")?.isChecked = false
            prefsEditor?.putBoolean("timeMode", false)?.commit()
            findPreference<SwitchPreferenceCompat>("pref_theme_battery")?.isChecked = false
            prefsEditor?.putBoolean("batteryMode", false)?.commit()
            /*if (!themeValue && (currentTime >= 2000) || (currentTime < 800)) {
                findPreference<SwitchPreferenceCompat>("pref_theme_time")?.isChecked = false
                prefsEditor?.putBoolean("timeMode", false)?.commit()
            }
            if (!themeValue && batteryLevel <= 30) {
                findPreference<SwitchPreferenceCompat>("pref_theme_battery")?.isChecked = false
                prefsEditor?.putBoolean("batteryMode", false)?.commit()
            }*/
            activity?.finish()
            activity?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            activity?.startActivity(activity?.intent)
        } else if (key.equals("pref_theme_battery")) {
            val batteryValue = sharedPreferences?.getBoolean(key, false)
            prefsEditor?.putBoolean("batteryMode", batteryValue!!)?.commit()
            if (batteryValue!! && batteryLevel <= 30 && sharedPrefs?.getInt("themeMode", R.style.LightTheme) == R.style.LightTheme) {
                prefsEditor?.putInt("themeMode", R.style.DarkTheme)?.commit()
                findPreference<SwitchPreferenceCompat>("pref_theme")?.isChecked = true
                activity?.finish()
                activity?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                activity?.startActivity(activity?.intent)
            }
        } else if (key.equals("pref_theme_time")) {
            val timeValue = sharedPreferences?.getBoolean(key, false)
            prefsEditor?.putBoolean("timeMode", timeValue!!)?.commit()
            if (timeValue!! && (currentTime >= 2000) || (currentTime < 800) && sharedPrefs?.getInt("themeMode", R.style.LightTheme) == R.style.LightTheme) {
                prefsEditor?.putInt("themeMode", R.style.DarkTheme)?.commit()
                findPreference<SwitchPreferenceCompat>("pref_theme")?.isChecked = true
                activity?.finish()
                activity?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                activity?.startActivity(activity?.intent)
            }
        } else if (key.equals("pref_language")) {
            val pref: ListPreference = findPreference("pref_language")!!
            val value: String = pref.value
            if (value != sharedPrefs?.getString("language", Locale.getDefault().language)) {
                prefsEditor?.putString("language", value)?.commit()
                Lingver.getInstance().setLocale(requireContext(), value)
                /*Locale.setDefault(Locale(value))
                val config = context?.resources?.configuration
                config?.setLocale(Locale(value))
                config?.setLayoutDirection(Locale(value))
                if (Build.VERSION.SDK_INT >= 25)  context?.createConfigurationContext(config!!)
                else context?.resources?.updateConfiguration(config!!, context?.resources?.displayMetrics)*/
                activity?.finish()
                activity?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                activity?.startActivity(activity?.intent)
            }
        } else if (key.equals("pref_screen")) {
            val screenValue = sharedPreferences?.getBoolean(key, false)
            prefsEditor?.putBoolean("screenMode", screenValue!!)?.commit()
            view?.keepScreenOn = true
        } else if (key.equals("pref_rotation")) {
            val rotationValue = sharedPreferences?.getBoolean(key, false)
            prefsEditor?.putInt("rotationMode", if (rotationValue!!) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)?.commit()
            activity?.requestedOrientation = if (rotationValue!!) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else if (key.equals("pref_temp")) {
            val pref: ListPreference = findPreference("pref_temp")!!
            val value: String = pref.value
            pref.summary = pref.entry
            prefsEditor?.putString("tempUnit", value)?.commit()
        } else if (key.equals("pref_distance")) {
            val pref: ListPreference = findPreference("pref_distance")!!
            val value: String = pref.value
            pref.summary = pref.entry
            prefsEditor?.putString("distanceUnit", value)?.commit()
        } else if (key.equals("pref_pressure")) {
            val pref: ListPreference = findPreference("pref_pressure")!!
            val value: String = pref.value
            pref.summary = pref.entry
            prefsEditor?.putString("pressureUnit", value)?.commit()
        } else if (key.equals("pref_angle")) {
            val pref: ListPreference = findPreference("pref_angle")!!
            val value: String = pref.value
            pref.summary = pref.entry
            prefsEditor?.putString("angleUnit", value)?.commit()
        } else if (key.equals("pref_precision")) {
            val pref: NumberPickerPreference = findPreference("pref_precision")!!
            prefsEditor?.putInt("precisionValue", pref.getPersistedInt())?.commit()
        }
    }
}