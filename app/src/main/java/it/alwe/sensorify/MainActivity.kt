package it.alwe.sensorify

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.preference.PreferenceManager
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.snackbar.Snackbar
import it.alwe.sensorify.databinding.ActivityMainBinding
import it.alwe.sensorify.sensors.*

class MainActivity : CommonActivity(), ActivityCompat.OnRequestPermissionsResultCallback {
    private lateinit var binding: ActivityMainBinding
    private var sharedPrefs: SharedPreferences? = null
    private var blockIntent: Intent? = null
    private var intAd: InterstitialAd? = null
    private var adTimes: Int = 2

    private var activities = arrayOf(
        SystemActivity::class.java,
        ConnectionActivity::class.java,
        GPSActivity::class.java,
        BatteryActivity::class.java,
        SoundMeterActivity::class.java
    )

    private var icons = arrayOf(
        R.drawable.ic_phone_android,
        R.drawable.ic_wifi,
        R.drawable.ic_gps,
        R.drawable.ic_battery_60,
        R.drawable.ic_sound_meter
    )

    private var entries = arrayOf(
        R.string.system_block,
        R.string.connection_block,
        R.string.gps_block,
        R.string.battery_block,
        R.string.sound_meter_block
    )

    // TODO : https://stackoverflow.com/questions/43407960/center-align-items-in-gridview

    override fun onCreate(savedInstanceState: Bundle?) {
        checkForSensors()
        MobileAds.initialize(this) {}
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        installSplashScreen()
        super.onCreate(savedInstanceState)

        MobileAds.setRequestConfiguration(RequestConfiguration.Builder()
            .setTestDeviceIds(arrayListOf(
                AdRequest.DEVICE_ID_EMULATOR,
                "BB64A34AE46577580B9E2E8BC34EC579"))
            .build()
        )

        loadAd()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_star)

        binding.gridBlocks.setSelection(sharedPrefs?.getInt("scrollPosition", 0)!!)
        binding.gridBlocks.smoothScrollBy(sharedPrefs?.getInt("scrollOffset", 0)!!, 0)

        binding.gridBlocks.adapter = BlocksAdapter(this, entries, icons)
        binding.gridBlocks.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position).toString()
            blockIntent = Intent(applicationContext, activities[selectedItem.toInt()])
            binding.gridBlocks.onSaveInstanceState()
            Log.w("AdView", "adTimes : $adTimes")
            if (intAd != null && adTimes % 5 == 0) {
                intAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.w("AdView", "onAdDismissedFullScreenContent")
                        loadAd()
                        finish()
                        startActivity(blockIntent)
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.w("AdView", "onAdFailedToShowFullScreenContent : $adError")
                        intAd = null
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.w("AdView", "onAdShowedFullScreenContent")
                        intAd = null
                    }
                }
                intAd?.show(this)
            } else {
                Log.w("AdView", "Not loaded !")
                finish()
                startActivity(blockIntent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
            adTimes += 1
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            && Build.VERSION.SDK_INT <= 29)
            neededPermissionRequest.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
    }

    private fun loadAd() {
        val adRequest = AdRequest.Builder().build()
        val intAdString = if (adRequest.isTestDevice(this)) getString(R.string.testInterstitialID) else getString(R.string.mainInterstitialID)
        InterstitialAd.load(this, intAdString, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.w("AdView", "onAdFailedToLoad : $adError")
                intAd = null
            }
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.w("AdView", "onAdLoaded")
                intAd = interstitialAd
            }
        })
    }

    override fun onStop() {
        super.onStop()
        var offset = 15 * resources.displayMetrics.density
        val firstChild = binding.gridBlocks.getChildAt(0)
        offset -= firstChild?.top ?: 0
        sharedPrefs?.edit()?.putInt("scrollPosition", binding.gridBlocks.firstVisiblePosition)?.apply()
        sharedPrefs?.edit()?.putInt("scrollOffset", offset.toInt())?.apply()
    }

    @SuppressLint("MissingPermission")
    private val neededPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        Log.w("NeededPerms", permissions.toString())
        when {
            permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true -> {
                recreate()
            } else -> {
                Snackbar.make(
                    findViewById(R.id.mainLayout), getString(R.string.permissionsLack),
                    Snackbar.LENGTH_LONG
                ).apply {
                    view.findViewById<TextView>(R.id.snackbar_text).maxLines = 3
                    setAction(getString(android.R.string.ok)) { }
                    setActionTextColor(ContextCompat.getColor(context, R.color.monoAxisColor))
                    show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent = Intent(this, SettingsActivity::class.java)
        intent.putExtra("previousActivity", this::class.java.simpleName)
        when (item.itemId) {
            android.R.id.home -> {
                try {
                    val goToMarket = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + baseContext.packageName))
                    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    startActivity(goToMarket)
                } catch (e: ActivityNotFoundException) {
                    val goToMarket = Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + baseContext.packageName))
                    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    startActivity(goToMarket)
                }
            }
            R.id.action_settings -> {
                startActivity(intent)
                overridePendingTransition(R.anim.from_right, R.anim.to_left)
                finish()
            }
            else -> Log.d("Error", "Ah shit, here we go again...")
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkForSensors() {
        val oSM = baseContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensorsList: List<Sensor> = oSM.getSensorList(Sensor.TYPE_ALL)

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            activities += HeartbeatActivity::class.java
            icons += R.drawable.ic_heart
            entries += R.string.heartbeat_block
        }

        if (sensorsList.contains(oSM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)) &&
                sensorsList.contains(oSM.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD))) {
            activities += OrientationActivity::class.java
            icons += R.drawable.ic_compass
            entries += R.string.orientation_block
        }

        for (sensor in sensorsList) {
            when (sensor.type) {
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    if (!entries.contains(R.string.linear_acceleration_block)) {
                        activities += LinearAccActivity::class.java
                        icons += R.drawable.ic_line
                        entries += R.string.linear_acceleration_block
                    }
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    if (!entries.contains(R.string.accelerometer_block)) {
                        activities += AccelerometerActivity::class.java
                        icons += R.drawable.ic_accelerometer
                        entries += R.string.accelerometer_block
                    }
                }
                Sensor.TYPE_MAGNETIC_FIELD, Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> {
                    if (!entries.contains(R.string.magnetometer_block)) {
                        activities += MagnetometerActivity::class.java
                        icons += R.drawable.ic_magnet
                        entries += R.string.magnetometer_block
                    }
                }
                Sensor.TYPE_GYROSCOPE, Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> {
                    if (!entries.contains(R.string.gyroscope_block)) {
                        activities += GyroscopeActivity::class.java
                        icons += R.drawable.ic_gyroscope
                        entries += R.string.gyroscope_block
                    }
                }
                Sensor.TYPE_LIGHT -> {
                    if (!entries.contains(R.string.luminosity_block)) {
                        activities += LuminosityActivity::class.java
                        icons += R.drawable.ic_lightbulb_on
                        entries += R.string.luminosity_block
                    }
                }
                Sensor.TYPE_PRESSURE -> {
                    if (!entries.contains(R.string.barometer_block)) {
                        activities += BarometerActivity::class.java
                        icons += R.drawable.ic_gauge
                        entries += R.string.barometer_block
                    }
                }
                Sensor.TYPE_PROXIMITY -> {
                    if (!entries.contains(R.string.proximity_block)) {
                        activities += ProximityActivity::class.java
                        icons += R.drawable.ic_ruler
                        entries += R.string.proximity_block
                    }
                }
                Sensor.TYPE_STEP_DETECTOR -> {
                    if (!entries.contains(R.string.pedometer_block)) {
                        activities += PedometerActivity::class.java
                        icons += R.drawable.ic_pedometer
                        entries += R.string.pedometer_block
                    }
                }
                Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                    if (!entries.contains(R.string.temperature_block)) {
                        activities += TempActivity::class.java
                        icons += R.drawable.ic_thermometer
                        entries += R.string.temperature_block
                    }
                }
                Sensor.TYPE_RELATIVE_HUMIDITY -> {
                    if (!entries.contains(R.string.humidity_block)) {
                        activities += HumidityActivity::class.java
                        icons += R.drawable.ic_humidity
                        entries += R.string.humidity_block
                    }
                }
                Sensor.TYPE_ROTATION_VECTOR/*, Sensor.TYPE_GAME_ROTATION_VECTOR, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR*/ -> {
                    if (!entries.contains(R.string.rotation_vectors_block)) {
                        activities += RotationVectorsActivity::class.java
                        icons += R.drawable.ic_rotation
                        entries += R.string.rotation_vectors_block
                    }
                }
                Sensor.TYPE_GRAVITY -> {
                    if (!entries.contains(R.string.gravity_block)) {
                        activities += GravityActivity::class.java
                        icons += R.drawable.ic_globe
                        entries += R.string.gravity_block
                    }
                }
            }
        }
    }
}
