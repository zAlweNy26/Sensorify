package it.alwe.sensorify

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_credits.*

class CreditsActivity : CommonActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_credits)

        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow)

        versionNumber.text = "v${BuildConfig.VERSION_NAME}"
        translators.text = "${getString(R.string.translatorsText)}\n${getString(R.string.translatorsList)}"
        betaTesters.text = "${getString(R.string.betaTestersText)}\n${getString(R.string.betaTestersList)}"

        /*privacyButton.setOnClickListener {
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

        closeButton.setOnClickListener {
            privacyLayout.visibility = View.GONE
            privacyLayout.alpha = 0.0f
            creditsLayout.visibility = View.VISIBLE
            creditsLayout.alpha = 0.0f
            creditsLayout.animate().alpha(1.0f).setListener(null)
        }*/
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        /*if (privacyLayout.visibility == View.VISIBLE) {
            privacyLayout.visibility = View.GONE
            privacyLayout.alpha = 0.0f
            creditsLayout.visibility = View.VISIBLE
            creditsLayout.alpha = 0.0f
            creditsLayout.animate().alpha(1.0f).setListener(null)
            return
        }*/
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
