package it.alwe.sensorify

import android.app.Application
import com.yariksoffice.lingver.Lingver
import com.yariksoffice.lingver.store.PreferenceLocaleStore
import java.util.*

// RIGHE DI CODICE : 3339

data class MissingSensor(var title: String, var description: String, var icon: Int){
    var isExpanded = false
}

class MainApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val store = PreferenceLocaleStore(this, Locale.getDefault())
        Lingver.init(this, store)

        /*MobileAds.initialize(this) {}

        val requestConfigurationBuilder = RequestConfiguration.Builder()
            .setTestDeviceIds(arrayListOf(
                AdRequest.DEVICE_ID_EMULATOR,
                "D88841A06FB3B7B7314D4A021C890B99"))
            .build()

        MobileAds.setRequestConfiguration(requestConfigurationBuilder)*/
    }
}