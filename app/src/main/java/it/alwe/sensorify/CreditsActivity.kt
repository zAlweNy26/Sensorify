package it.alwe.sensorify

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import it.alwe.sensorify.databinding.ActivityCreditsBinding

class CreditsActivity : CommonActivity() {
    private lateinit var binding: ActivityCreditsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreditsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolBar)

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow)

        binding.versionNumber.text = "v${BuildConfig.VERSION_NAME}"
        binding.translators.text = "${getString(R.string.translatorsText)}\n${getString(R.string.translatorsList)}"
        binding.betaTesters.text = "${getString(R.string.betaTestersText)}\n${getString(R.string.betaTestersList)}"

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

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                /*if (privacyLayout.visibility == View.VISIBLE) {
                    privacyLayout.visibility = View.GONE
                    privacyLayout.alpha = 0.0f
                    creditsLayout.visibility = View.VISIBLE
                    creditsLayout.alpha = 0.0f
                    creditsLayout.animate().alpha(1.0f).setListener(null)
                    return
                }*/
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
