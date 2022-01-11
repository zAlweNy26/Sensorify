package it.alwe.sensorify.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.devs.vectorchildfinder.VectorChildFinder
import com.devs.vectorchildfinder.VectorDrawableCompat
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import kotlinx.android.synthetic.main.activity_gps.*

class GPSActivity : BaseBlockActivity(), ActivityCompat.OnRequestPermissionsResultCallback {
    private var locationManager: LocationManager? = null

    override val contentView: Int
        get() = R.layout.activity_gps

    @SuppressLint("MissingPermission")
    private val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false && permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false -> {
                recreate()
                locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0f, locationListener)
            } else -> onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()

        if (locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mapsButton.isEnabled = true
            colorIconGPS(Color.GREEN)
        } else {
            mapsButton.isEnabled = false
            colorIconGPS(Color.RED)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        else locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0f, locationListener)
    }

    override fun onPause() {
        super.onPause()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        else locationManager?.removeUpdates(locationListener)
    }

    private val locationListener: LocationListener = object : LocationListener {

        override fun onProviderDisabled(provider: String) { colorIconGPS(Color.RED) }

        override fun onProviderEnabled(provider: String) { colorIconGPS(Color.GREEN) }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onLocationChanged(location: Location) {
            longitudeValue.text = location.longitude.toString()
            latitudeValue.text = location.latitude.toString()
            altitudeValue.text = location.altitude.toString()
        }
    }

    private fun colorIconGPS(color: Int) {
        if (color == Color.GREEN) blockMainInfoText.text = getString(R.string.state_text, getString(R.string.gps_enabled))
        else if (color == Color.RED) blockMainInfoText.text = getString(R.string.state_text, getString(R.string.gps_disabled))
        val vector = VectorChildFinder(this, R.drawable.ic_gps, gpsIcon)
        val path1: VectorDrawableCompat.VFullPath = vector.findPathByName("gpsCircle")
        path1.fillColor = color
        val path2: VectorDrawableCompat.VFullPath = vector.findPathByName("gpsBG")
        path2.fillColor = blockInformations.currentTextColor
        gpsIcon.invalidate()
    }

    override fun addCode() {
        blockMainInfoText.text = getString(R.string.state_text, getString(R.string.gps_disabled))

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        colorIconGPS(Color.RED)

        mapsButton.setOnClickListener {
            val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${latitudeValue.text},${longitudeValue.text}"))
            mapsIntent.setPackage("com.google.android.apps.maps")
            if (mapsIntent.resolveActivity(packageManager) != null) startActivity(mapsIntent)
        }

        gpsActivate.setOnClickListener {
            val gpsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(gpsIntent)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        else locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0f, locationListener)
    }

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = "${getString(R.string.gps_page)} :\n" +
            "${blockMainInfoText.text}\n" +
            "${getString(R.string.latitude)} ${latitudeValue.text}\n" +
            "${getString(R.string.longitude)} ${longitudeValue.text}\n" +
            "${getString(R.string.altitude)} ${altitudeValue.text}"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.gps_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}