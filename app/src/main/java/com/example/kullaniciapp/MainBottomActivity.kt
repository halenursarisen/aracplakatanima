package com.example.kullaniciapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainBottomActivity : AppCompatActivity() {
<<<<<<< HEAD
=======

>>>>>>> 44d3236e304bccba2f7b47a1a2eb8beafebd2045
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_bottom)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

<<<<<<< HEAD
        // İlk açılışta HomeFragment göster
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()

        // Sekmelere tıklanınca fragment değiştir
=======
        // Uygulama ilk açıldığında HomeFragment gösterilir
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        // Navigation item seçildiğinde ilgili fragment gösterilir
>>>>>>> 44d3236e304bccba2f7b47a1a2eb8beafebd2045
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
