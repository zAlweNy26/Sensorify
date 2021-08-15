package it.alwe.sensorify

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : CommonActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow)

        if (findViewById<View>(R.id.settings_fragment) != null) {
            if (savedInstanceState != null) return
            supportFragmentManager.beginTransaction().replace(R.id.settings_fragment, SettingsFragment()).commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        var activity = intent.getStringExtra("previousActivity")!!
        if (activity != "MainActivity") activity = "sensors.$activity"
        val cls = Class.forName("it.alwe.sensorify.$activity")
        startActivity(Intent(this, cls))
        overridePendingTransition(R.anim.from_left, R.anim.to_right)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_credits -> {
                val intent = Intent(this, CreditsActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
            R.id.action_missing_sensors -> {
                val intent = Intent(this, MissingSensorsActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
            R.id.action_privacy_policy -> {
                val intentBuilder = CustomTabsIntent.Builder()
                val params = CustomTabColorSchemeParams.Builder()
                    .setNavigationBarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    .setSecondaryToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    .build()
                intentBuilder.setShowTitle(false)
                intentBuilder.setUrlBarHidingEnabled(true)
                intentBuilder.setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, params)
                intentBuilder.setStartAnimations(this, android.R.anim.fade_in, android.R.anim.fade_out)
                intentBuilder.setExitAnimations(this, android.R.anim.fade_in, android.R.anim.fade_out)
                val customTabsIntent = intentBuilder.build()
                customTabsIntent.launchUrl(this, Uri.parse("https://zalweny26.github.io/privacy_policy"))
            }
            else -> Log.d("Error", "Ah shit, here we go again...")
        }
        return super.onOptionsItemSelected(item)
    }
}
