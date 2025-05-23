package com.example.kullaniciapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.firestore.FirebaseFirestore
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator

class MisafirHomeActivity : AppCompatActivity() {

    private lateinit var textGecenSure: TextView
    private var startTimeMillis: Long = 0L
    private val handler = Handler(Looper.getMainLooper())

    private var currentPage = 0
    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private lateinit var autoScrollRunnable: Runnable

    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsedMillis = System.currentTimeMillis() - startTimeMillis
            val hours = (elapsedMillis / (1000 * 60 * 60)) % 24
            val minutes = (elapsedMillis / (1000 * 60)) % 60
            val seconds = (elapsedMillis / 1000) % 60
            val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            textGecenSure.text = "Geçen park süresi: $timeFormatted"
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_misafir_home)

        val plaka = intent.getStringExtra("plaka") ?: "Bilinmiyor"
        findViewById<TextView>(R.id.textPlaka).text = "Plaka: $plaka"

        // 🔔 Animasyon
        val animText = findViewById<TextView>(R.id.textAnimUyari)
        val anim = AnimationUtils.loadAnimation(this, R.anim.scroll_text)
        animText.startAnimation(anim)

        // ⏱ Sayaç
        textGecenSure = findViewById(R.id.textGecenSure)
        startTimeMillis = System.currentTimeMillis() - (10 * 60 * 1000)
        handler.post(timerRunnable)

        // 🔄 ViewPager
        val viewPager = findViewById<ViewPager2>(R.id.infoViewPager)
        val dots = findViewById<WormDotsIndicator>(R.id.dotsIndicator)

        val infoCards = listOf(
            InfoCard("Otopark Durumu", "Açık – Giriş serbest", android.R.drawable.ic_dialog_info),
            InfoCard("Doluluk", "18/30 dolu", android.R.drawable.ic_menu_agenda),
            InfoCard("Güvenlik", "Kamera sistemi aktif", android.R.drawable.ic_menu_camera)
        )

        viewPager.adapter = InfoCardAdapter(infoCards)
        dots.attachTo(viewPager)

        autoScrollRunnable = object : Runnable {
            override fun run() {
                val itemCount = viewPager.adapter?.itemCount ?: 0
                if (itemCount > 0) {
                    currentPage = (currentPage + 1) % itemCount
                    viewPager.setCurrentItem(currentPage, true)
                    autoScrollHandler.postDelayed(this, 2000)
                }
            }
        }
        autoScrollHandler.postDelayed(autoScrollRunnable, 2000)

        // 📝 Bilgiler
        findViewById<TextView>(R.id.textGirisSaati).text = "Giriş Saati: -"
        findViewById<TextView>(R.id.textParkAlani).text = "Park Alanı: -"
        findViewById<TextView>(R.id.textUcret).text = "Toplam Ücret: -"
        findViewById<TextView>(R.id.textTarife).text = "Tarife: İlk 30 dk ücretsiz, sonra 10₺/saat"

        // 💸 Ödeme
        findViewById<Button>(R.id.buttonOde).setOnClickListener {
            Toast.makeText(this, "Ödeme işlemi misafir girişiyle yapılamaz.", Toast.LENGTH_SHORT).show()
        }

        // ⛔ Rezervasyon
        findViewById<Button>(R.id.buttonRezervasyon).visibility = Button.GONE

        // 🔑 Kayıt Ol
        findViewById<Button>(R.id.buttonKayitOl).setOnClickListener {
            startActivity(Intent(this, KayitOlActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
    }
}
