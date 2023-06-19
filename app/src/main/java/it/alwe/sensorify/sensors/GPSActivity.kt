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
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.devs.vectorchildfinder.VectorChildFinder
import com.devs.vectorchildfinder.VectorDrawableCompat
import com.google.android.material.snackbar.Snackbar
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import it.alwe.sensorify.databinding.ActivityGpsBinding

class GPSActivity : BaseBlockActivity<ActivityGpsBinding>(),
    ActivityCompat.OnRequestPermissionsResultCallback {
    private var locationManager: LocationManager? = null

    override fun setupViewBinding(inflater: LayoutInflater): ActivityGpsBinding {
        return ActivityGpsBinding.inflate(inflater)
    }

    @SuppressLint("MissingPermission")
    private val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true && permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                recreate()
                locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0f, locationListener)
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
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            content.mapsButton.isEnabled = true
            colorIconGPS(Color.GREEN)
        } else {
            content.mapsButton.isEnabled = false
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

        override fun onLocationChanged(location: Location) {
            content.longitudeValue.text = location.longitude.toString()
            content.latitudeValue.text = location.latitude.toString()
            content.altitudeValue.text = location.altitude.toString()
        }
    }

    private fun colorIconGPS(color: Int) {
        if (color == Color.GREEN) content.blockMainInfoText.text = getString(R.string.state_text, getString(R.string.gps_enabled))
        else if (color == Color.RED) content.blockMainInfoText.text = getString(R.string.state_text, getString(R.string.gps_disabled))
        val vector = VectorChildFinder(this, R.drawable.ic_gps, content.gpsIcon)
        val path1: VectorDrawableCompat.VFullPath = vector.findPathByName("gpsCircle")
        path1.fillColor = color
        val path2: VectorDrawableCompat.VFullPath = vector.findPathByName("gpsBG")
        path2.fillColor = content.blockInformations.currentTextColor
        content.gpsIcon.invalidate()
    }

    override fun addCode() {
        content.blockMainInfoText.text = getString(R.string.state_text, getString(R.string.gps_disabled))

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        colorIconGPS(Color.RED)

        content.mapsButton.setOnClickListener {
            val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${content.latitudeValue.text},${content.longitudeValue.text}"))
            mapsIntent.setPackage("com.google.android.apps.maps")
            if (mapsIntent.resolveActivity(packageManager) != null) startActivity(mapsIntent)
        }

        content.gpsActivate.setOnClickListener {
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
            "${content.blockMainInfoText.text}\n" +
            "${getString(R.string.latitude)} ${content.latitudeValue.text}\n" +
            "${getString(R.string.longitude)} ${content.longitudeValue.text}\n" +
            "${getString(R.string.altitude)} ${content.altitudeValue.text}"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.gps_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}