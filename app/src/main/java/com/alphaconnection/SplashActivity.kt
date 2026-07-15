package com.alphaconnection

import android.content.Intent
import android.os.Bundle
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.ivLogo)
        val appName = findViewById<TextView>(R.id.tvAppName)
        val subtitle = findViewById<TextView>(R.id.tvSubtitle)

        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 800
            fillAfter = true
        }

        logo.startAnimation(fadeIn)
        appName.startAnimation(fadeIn.apply { startOffset = 200 })
        subtitle.startAnimation(AlphaAnimation(0f, 1f).apply {
            duration = 800
            startOffset = 400
            fillAfter = true
        })

        logo.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 1800)
    }
}
