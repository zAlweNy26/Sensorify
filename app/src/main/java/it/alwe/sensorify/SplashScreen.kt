package it.alwe.sensorify

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.splash_screen.*

class SplashScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.splash_screen)

        appVersion.text = "v${BuildConfig.VERSION_NAME}"
    }

    private fun openMain() {
        finish()
        startActivity(Intent(applicationContext, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onResume() {
        super.onResume()

        val handler =  Handler(Looper.getMainLooper())
        val runnableCode = Runnable { openMain() }
        handler.postDelayed(runnableCode, 3000)
    }
}
