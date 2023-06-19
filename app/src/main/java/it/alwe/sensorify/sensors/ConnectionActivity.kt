package it.alwe.sensorify.sensors

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.format.Formatter
import android.view.LayoutInflater
import androidx.core.app.ActivityCompat
import androidx.core.text.HtmlCompat
import com.devs.vectorchildfinder.VectorChildFinder
import com.devs.vectorchildfinder.VectorDrawableCompat
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import it.alwe.sensorify.databinding.ActivityConnectionBinding
import java.util.*

class ConnectionActivity : BaseBlockActivity<ActivityConnectionBinding>(),
    ActivityCompat.OnRequestPermissionsResultCallback {
    private var intentFilter: IntentFilter? = null
    private var connectionStatus = "offline"
    private var cSSID = ""
    private var cBSSID = ""
    private var cIP = ""
    private var cMAC = ""
    private var cRSSI = ""
    private var cSpeed = ""
    private var cFrequency = ""
    //private var cIMEI = ""
    private var cOperator = ""
    private var cType = ""

    override fun setupViewBinding(inflater: LayoutInflater): ActivityConnectionBinding {
        return ActivityConnectionBinding.inflate(inflater)
    }

    override fun onResume() {
        super.onResume()
        setBroadCastRegistered(true, connectionListener, intentFilter!!)
    }

    override fun onPause() {
        super.onPause()
        setBroadCastRegistered(false, connectionListener, intentFilter!!)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                setBroadCastRegistered(true, connectionListener, intentFilter!!)
                recreate()
            } else {
                colorIconSignal(0)
                content.blockMainInfoText.text = getString(R.string.state_text, getString(R.string.unavailableValue))
                content.connectionTypeText.text = getString(R.string.connectionType, getString(R.string.connection_type_none))
                content.ssid.text = getString(R.string.unavailableValue)
                content.bssid.text = getString(R.string.unavailableValue)
                content.ipAddress.text = getString(R.string.unavailableValue)
                content.macAddress.text = getString(R.string.unavailableValue)
                content.rssi.text = getString(R.string.unavailableValue)
                content.frequency.text = getString(R.string.unavailableValue)
                //imei.text = getString(R.string.unavailableValue)
                content.operator.text = getString(R.string.unavailableValue)
                content.speed.text = getString(R.string.unavailableValue)
            }
        }
    }

    private fun colorIconSignal(signalLevel: Int) {
        val vector = VectorChildFinder(this, R.drawable.ic_wifi, content.wifiIcon)
        val path0: VectorDrawableCompat.VFullPath = vector.findPathByName("cOff")
        path0.fillColor = if (signalLevel == 0) content.blockInformations.currentTextColor else Color.TRANSPARENT
        val path1: VectorDrawableCompat.VFullPath = vector.findPathByName("wifi1")
        path1.fillColor = if (signalLevel >= 1) content.blockInformations.currentTextColor else Color.TRANSPARENT
        val path2: VectorDrawableCompat.VFullPath = vector.findPathByName("wifi2")
        path2.fillColor = if (signalLevel >= 2) content.blockInformations.currentTextColor else Color.TRANSPARENT
        val path3: VectorDrawableCompat.VFullPath = vector.findPathByName("wifi3")
        path3.fillColor = if (signalLevel >= 3) content.blockInformations.currentTextColor else Color.TRANSPARENT
        val path4: VectorDrawableCompat.VFullPath = vector.findPathByName("wifi4")
        path4.fillColor = if (signalLevel >= 4) content.blockInformations.currentTextColor else Color.TRANSPARENT
        content.wifiIcon.invalidate()
    }

    private val connectionListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            var connectionState = 0
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				val networkCapabilities = connectivityManager.activeNetwork
				val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities)
                connectionState = when {
                    actNw != null && actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 1
                    actNw != null && actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 2
                    actNw != null && actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> 3
					else -> 0
				}
			} else {
				connectivityManager.run {
                    @Suppress("DEPRECATION")
					connectivityManager.activeNetworkInfo?.run {
                        connectionState = when (type) {
							ConnectivityManager.TYPE_WIFI -> 1
							ConnectivityManager.TYPE_MOBILE -> 2
							ConnectivityManager.TYPE_ETHERNET -> 3
							else -> 0
						}
                    }
				}
			}

            if (Settings.System.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0) cType = getString(
                R.string.connection_type_plane
            )
            else if (connectionState == 0) cType = getString(R.string.connection_type_none)

            when (connectionState) {
                1 -> {
                    cType = getString(R.string.connection_type_wifi)
                    val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val wifiInfo = wifiManager.connectionInfo
                    if (wifiInfo.supplicantState == SupplicantState.COMPLETED) {
                        colorIconSignal(WifiManager.calculateSignalLevel(wifiInfo.rssi, 4))
                        connectionStatus = getString(R.string.wifi_state_value_on)
                        cSSID = if (wifiInfo.ssid.replace("\"", "") == "<unknown ssid>") getString(R.string.unavailableValue) else wifiInfo.ssid.replace("\"", "")
                        cBSSID = wifiInfo?.bssid.toString()
                        cMAC = getString(R.string.unavailableValue)
                        @Suppress("DEPRECATION")
                        cIP = Formatter.formatIpAddress(wifiInfo.ipAddress)
                        cRSSI = "${wifiInfo?.rssi} dBm"
                        cFrequency = "${wifiInfo?.frequency} MHz"
                        //cIMEI = getString(R.string.unavailableValue)
                        cOperator = getString(R.string.unavailableValue)
                        cSpeed = "${wifiInfo?.linkSpeed} Mbps | ${(wifiInfo?.linkSpeed?.div(8))} MB/s"
                    }
                }
                2 -> {
                    cType = getString(R.string.connection_type_mobile)
                    val tManager = baseContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    /*val cellDBM = when (val info = tManager.allCellInfo?.firstOrNull()) {
                        is CellInfoLte -> info.cellSignalStrength.dbm
                        is CellInfoGsm -> info.cellSignalStrength.dbm
                        is CellInfoCdma -> info.cellSignalStrength.dbm
                        is CellInfoWcdma -> info.cellSignalStrength.dbm
                        else -> 0
                    }
                    val cellLevel = when (val info = tManager.allCellInfo?.firstOrNull()) {
                        is CellInfoLte -> info.cellSignalStrength.level
                        is CellInfoGsm -> info.cellSignalStrength.level
                        is CellInfoCdma -> info.cellSignalStrength.level
                        is CellInfoWcdma -> info.cellSignalStrength.level
                        else -> 0
                    }*/
                    colorIconSignal(4)
                    connectionStatus = getString(R.string.wifi_state_value_on)
                    cSSID = getString(R.string.unavailableValue)
                    cBSSID = getString(R.string.unavailableValue)
                    cMAC = getString(R.string.unavailableValue)
                    cIP = getString(R.string.unavailableValue)
                    cRSSI = getString(R.string.unavailableValue)
                    cFrequency = getString(R.string.unavailableValue)
                    /*cIMEI = when {
                        (Build.VERSION.SDK_INT < 26 || Build.VERSION.SDK_INT >= 29) &&
                                ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED -> tManager.deviceId
                        Build.VERSION.SDK_INT >= 26 &&
                                ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED -> tManager.imei
                        else -> getString(R.string.unavailableValue)
                    }*/
                    cOperator = "${tManager.networkOperatorName} | ${Locale("", tManager.networkCountryIso).displayCountry}"
                    cSpeed = if (Build.VERSION.SDK_INT >= 23) {
                        val nc: NetworkCapabilities = cm.getNetworkCapabilities(cm.activeNetwork)!!
                        "${(nc.linkDownstreamBandwidthKbps / 8000)} MB/s"
                    } else getString(R.string.unavailableValue)
                }
                else -> {
                    colorIconSignal(0)
                    connectionStatus = getString(R.string.wifi_state_value_off)
                    cSSID = getString(R.string.unavailableValue)
                    cBSSID = getString(R.string.unavailableValue)
                    cMAC = getString(R.string.unavailableValue)
                    cIP = getString(R.string.unavailableValue)
                    cRSSI = getString(R.string.unavailableValue)
                    cFrequency = getString(R.string.unavailableValue)
                    //cIMEI = getString(R.string.unavailableValue)
                    cOperator = getString(R.string.unavailableValue)
                    cSpeed = getString(R.string.unavailableValue)
                }
            }
            content.blockMainInfoText.text = getString(R.string.state_text, connectionStatus)
            content.connectionTypeText.text = getString(R.string.connectionType, cType)
            content.ssid.text = cSSID
            content.bssid.text = cBSSID
            content.ipAddress.text = cIP
            content.macAddress.text = cMAC
            content.rssi.text = cRSSI
            content.frequency.text = cFrequency
            //imei.text = cIMEI
            content.operator.text = cOperator
            content.speed.text = cSpeed
        }
    }

    override fun addCode() {
        content.blockMainInfoText.text = getString(R.string.state_text, getString(R.string.unavailableValue))
        content.connectionTypeText.text = getString(R.string.connectionType, getString(R.string.connection_type_none))
        content.simState.text = getSimState()

        intentFilter = IntentFilter()
        intentFilter?.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        intentFilter?.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        @Suppress("DEPRECATION") intentFilter?.addAction(ConnectivityManager.CONNECTIVITY_ACTION)

        content.connectionButton.setOnClickListener { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }

        content.mobileButton.setOnClickListener { startActivity(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)) }

        var connectionInfo = getString(R.string.connectionInfo)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) connectionInfo += "<br><br><b>${getText(
            R.string.sensorNote
        )}</b><br>${getText(R.string.android_mac_perms)}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) connectionInfo += "<br><br><b>${getText(
            R.string.sensorNote
        )}</b><br>${getText(R.string.wifi_android_perms)}"

        content.sensorInfoText.text = HtmlCompat.fromHtml(connectionInfo, HtmlCompat.FROM_HTML_MODE_LEGACY)

        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE), 1)
        else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
            && Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), 1)
    }

    private fun getSimState(): String {
        val telMgr = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        /*if (Build.VERSION.SDK_INT >= 30) Log.w("Alwe", "SIM slots : ${telMgr.activeModemCount}")
        else if (Build.VERSION.SDK_INT >= 23) Log.w("Alwe", "SIM slots : ${telMgr.phoneCount}")
        else Log.w("Alwe", "SIM slots : 1")

        if (Build.VERSION.SDK_INT >= 26)
            Log.w("Alwe", "SIM 1 : ${telMgr.getSimState(0)} | SIM 2 : ${telMgr.getSimState(1)}")
        else
            Log.w("Alwe", "SIM 1 : ${getSimStateBySlot(baseContext, 0)} | SIM 2 : ${getSimStateBySlot(baseContext, 1)}")*/

        return when (telMgr.simState) {
            TelephonyManager.SIM_STATE_ABSENT -> getString(R.string.simAbsent)
            TelephonyManager.SIM_STATE_CARD_IO_ERROR,
            TelephonyManager.SIM_STATE_CARD_RESTRICTED,
            TelephonyManager.SIM_STATE_NOT_READY,
            TelephonyManager.SIM_STATE_PERM_DISABLED -> getString(R.string.simDisabled)
            TelephonyManager.SIM_STATE_NETWORK_LOCKED,
            TelephonyManager.SIM_STATE_PIN_REQUIRED,
            TelephonyManager.SIM_STATE_PUK_REQUIRED -> getString(R.string.simBlocked)
            TelephonyManager.SIM_STATE_READY -> getString(R.string.simReady)
            TelephonyManager.SIM_STATE_UNKNOWN -> getString(R.string.simUnknown)
            else -> getString(R.string.simAbsent)
        }
    }

    /*private fun getSimStateBySlot(context: Context, slotID: Int): Int {
        var simState = 0
        val telephony = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        try {
            val telephonyClass = Class.forName(telephony.javaClass.name)
            val parameter = arrayOfNulls<Class<*>?>(1)
            parameter[0] = Int::class.javaPrimitiveType
            val getSimStateGemini = telephonyClass.getMethod("getSimState", *parameter)
            val obParameter = arrayOfNulls<Any>(1)
            obParameter[0] = slotID
            val obPhone = getSimStateGemini.invoke(telephony, obParameter)
            if (obPhone != null) simState = obPhone.toString().toInt()
        } catch (e: Exception) { e.printStackTrace() }
        return simState
    }*/

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = "${getString(R.string.connection_page)} :\n" +
            "${content.blockMainInfoText.text}\n" +
            "${content.connectionTypeText.text}\n" +
            "${getString(R.string.wifi_ssid)} ${content.ssid.text}\n" +
            "${getString(R.string.wifi_bssid)} ${content.bssid.text}\n" +
            "${getString(R.string.wifi_ip)} ${content.ipAddress.text}\n" +
            "${getString(R.string.wifi_mac)} ${content.macAddress.text}\n" +
            "${getString(R.string.wifi_rssi)} ${content.rssi.text}\n" +
            "${getString(R.string.wifi_frequency)} ${content.frequency.text}\n" +
            //"${getString(R.string.connectionIMEI)} ${imei.text}\n" +
            "${getString(R.string.mobileOperator)} ${content.operator.text}\n" +
            "${getString(R.string.simState)} ${content.simState.text}\n" +
            "${getString(R.string.connectionSpeed)} ${content.speed.text}"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.connection_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}