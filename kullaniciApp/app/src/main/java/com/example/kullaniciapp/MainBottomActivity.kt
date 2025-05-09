package com.example.kullaniciapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainBottomActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_bottom)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        // İlk açılışta HomeFragment göster
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()

        // Sekmelere tıklanınca fragment değiştir
        bottomNav.setOnItemSelectedListener { item ->
            val selectedFragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_notifications -> BildirimFragment()
                R.id.nav_chatbot -> ChatbotFragment()
                R.id.nav_profile -> HesabimFragment()
                else -> HomeFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, selectedFragment)
                .commit()

            true
        }
    }
}
