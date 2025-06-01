package com.example.kullaniciapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MisafirHomeActivity : AppCompatActivity() {

    private lateinit var textGecenSure: TextView
    private lateinit var databaseRef: DatabaseReference
    private val auth = FirebaseAuth.getInstance()
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

        val uid = auth.currentUser?.uid ?: return

        // UI öğeleri
        val textPlaka = findViewById<TextView>(R.id.textPlaka)
        val textGirisSaati = findViewById<TextView>(R.id.textGirisSaati)
        val textParkAlani = findViewById<TextView>(R.id.textParkAlani)
        val textUcret = findViewById<TextView>(R.id.textUcret)
        val textTarife = findViewById<TextView>(R.id.textTarife)
        textGecenSure = findViewById(R.id.textGecenSure)

        // Animasyon
        val animText = findViewById<TextView>(R.id.textAnimUyari)
        val anim = AnimationUtils.loadAnimation(this, R.anim.scroll_text)
        animText.startAnimation(anim)

        // ViewPager setup
        val viewPager = findViewById<ViewPager2>(R.id.infoViewPager)
        val dots = findViewById<WormDotsIndicator>(R.id.dotsIndicator)

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

        // Firebase bağlantısı
        val db = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        databaseRef = db.getReference("anonimKullanicilar").child(uid)

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val plaka = snapshot.child("plaka").getValue(String::class.java) ?: "-"
                val girisSaati = snapshot.child("girisSaati").getValue(String::class.java) ?: "-"
                val parkAlani = snapshot.child("parkAlani").getValue(String::class.java) ?: "-"

                val textHosgeldin = findViewById<TextView>(R.id.textHosgeldin)
                textHosgeldin.text = "Hoş geldin, $plaka 👋"


                textPlaka.text = "Plaka: $plaka"
                textGirisSaati.text = "Giriş Saati: $girisSaati"
                textParkAlani.text = "Park Alanı: $parkAlani"
                textTarife.text = "Tarife: İlk 30 dk ücretsiz, sonra 10₺/saat"

                // Dinamik infoCards listesi
                val infoCardList = mutableListOf<InfoCard>()
                infoCardList.addAll(
                    listOf(
                        InfoCard("Otopark Durumu", snapshot.child("otoparkDurumu").getValue(String::class.java) ?: "-", android.R.drawable.ic_dialog_info),
                        InfoCard("Doluluk", snapshot.child("doluluk").getValue(String::class.java) ?: "-", android.R.drawable.ic_menu_agenda),
                        InfoCard("Kamera Durumu", snapshot.child("kameraDurumu").getValue(String::class.java) ?: "-", android.R.drawable.ic_menu_camera),
                        InfoCard("Giriş/Çıkış", snapshot.child("girisCikis").getValue(String::class.java) ?: "-", android.R.drawable.ic_menu_directions)
                    )
                )

                val cardsSnapshot = snapshot.child("infoCards")
                if (cardsSnapshot.exists()) {
                    for (cardSnap in cardsSnapshot.children) {
                        val title = cardSnap.child("title").getValue(String::class.java) ?: "-"
                        val description = cardSnap.child("description").getValue(String::class.java) ?: "-"
                        val iconId = android.R.drawable.ic_dialog_info
                        infoCardList.add(InfoCard(title, description, iconId))
                    }
                }

                viewPager.adapter = InfoCardAdapter(infoCardList)
                dots.attachTo(viewPager)

                if (girisSaati != "-" && girisSaati != "null") {
                    try {
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val girisDate = sdf.parse(girisSaati)
                        startTimeMillis = girisDate.time
                        handler.post(timerRunnable)

                        val now = Date()
                        val diffMillis = now.time - girisDate.time
                        val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
                        var toplamUcret = 0

                        if (diffMinutes > 30) {
                            val ekSaat = Math.ceil((diffMinutes - 30).toDouble() / 60).toInt()
                            toplamUcret = ekSaat * 10
                        }

                        if (toplamUcret > 100) toplamUcret = 100
                        textUcret.text = "Toplam Ücret: $toplamUcret₺"
                    } catch (e: Exception) {
                        textGecenSure.text = "Geçen park süresi: Hesaplanamadı"
                        textUcret.text = "Toplam Ücret: -"
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MisafirHomeActivity, "Veri çekilemedi!", Toast.LENGTH_SHORT).show()
            }
        })

        findViewById<Button>(R.id.buttonOde).setOnClickListener {
            Toast.makeText(this, "Ödeme işlemi misafir girişiyle yapılamaz.", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.buttonKayitOl).setOnClickListener {
            startActivity(Intent(this, KayitOlActivity::class.java))
        }

        // 🚪 Çıkış Yap butonu
        findViewById<Button>(R.id.buttonCikisYap).setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Çıkış yapıldı.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
    }
}
