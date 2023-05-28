package it.alwe.sensorify

import android.app.Application
import com.yariksoffice.lingver.Lingver
import com.yariksoffice.lingver.store.PreferenceLocaleStore
import java.util.*

// RIGHE DI CODICE : 3573

data class MissingSensor(var title: String, var description: String, var icon: Int){
    var isExpanded = false
}

class MainApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val store = PreferenceLocaleStore(this, Locale.getDefault())
        Lingver.init(this, store)
    }
}