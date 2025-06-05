package com.example.kullaniciapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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
    private val handler = Handler(Looper.getMainLooper())

    private var currentPage = 0
    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private lateinit var autoScrollRunnable: Runnable
    private lateinit var updateRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_misafir_home)

        val uid = intent.getStringExtra("uid") ?: return

        val textPlaka = findViewById<TextView>(R.id.textPlaka)
        val textGirisTarihi = findViewById<TextView>(R.id.textGirisTarihi)
        val textGirisSaati = findViewById<TextView>(R.id.textGirisSaati)
        val textUcret = findViewById<TextView>(R.id.textUcret)
        textGecenSure = findViewById(R.id.textGecenSure)

        val animText = findViewById<TextView>(R.id.textAnimUyari)
        val anim = AnimationUtils.loadAnimation(this, R.anim.scroll_text)
        animText.startAnimation(anim)

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

        val db = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        databaseRef = db.getReference("anonimKullanicilar").child(uid)

        val infoCardsRef = db.getReference("infoCards")
        infoCardsRef.get().addOnSuccessListener { snapshot ->
            val infoCardList = mutableListOf<InfoCard>()
            snapshot.children.forEach { cardSnap ->
                val title = cardSnap.child("title").getValue(String::class.java) ?: "-"
                val description = cardSnap.child("description").getValue(String::class.java) ?: "-"
                val icon = when (title) {
                    "Otopark Durumu" -> android.R.drawable.ic_dialog_info
                    "Doluluk" -> android.R.drawable.ic_menu_agenda
                    "Kamera Durumu" -> android.R.drawable.ic_menu_camera
                    "Giriş/Çıkış" -> android.R.drawable.ic_menu_directions
                    else -> android.R.drawable.ic_menu_help
                }
                infoCardList.add(InfoCard(title, description, icon))
            }
            viewPager.adapter = InfoCardAdapter(infoCardList)
            dots.attachTo(viewPager)
        }

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val plaka = snapshot.child("plaka").getValue(String::class.java) ?: "-"
                val girisTarihi = snapshot.child("giris_tarihi").getValue(String::class.java) ?: "-"
                val girisSaati = snapshot.child("giris_saati").getValue(String::class.java) ?: "-"

                findViewById<TextView>(R.id.textHosgeldin).text = "Hoş geldin, $plaka 👋"
                textPlaka.text = "Plaka: $plaka"
                textGirisTarihi.text = "Giriş Tarihi: $girisTarihi"
                textGirisSaati.text = "Giriş Saati: $girisSaati"

                if (girisSaati != "-" && girisTarihi != "-") {
                    try {
                        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        val girisDate = sdf.parse("$girisTarihi $girisSaati")!!
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

                        updateRunnable = object : Runnable {
                            override fun run() {
                                val currentTime = Date()
                                val elapsedMillis = currentTime.time - girisDate.time
                                val elapsedHours = TimeUnit.MILLISECONDS.toHours(elapsedMillis)
                                val elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis) % 60
                                val elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis) % 60

                                textGecenSure.text = String.format(
                                    "Geçen Süre: %02d saat %02d dakika %02d saniye",
                                    elapsedHours, elapsedMinutes, elapsedSeconds
                                )
                                handler.postDelayed(this, 1000)
                            }
                        }
                        handler.post(updateRunnable)
                    } catch (e: Exception) {
                        textUcret.text = "Toplam Ücret: Hesaplanamadı"
                        textGecenSure.text = "Geçen Süre: Hesaplanamadı"
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MisafirHomeActivity, "Veri çekilemedi: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        findViewById<Button>(R.id.buttonOde).setOnClickListener {
            val anonimRef = db.getReference("anonimKullanicilar").child(uid)
            val cikisYapanRef = db.getReference("cikisYapanKullanicilar").child(uid)
            val toplamUcret = textUcret.text.toString()

            anonimRef.get().addOnSuccessListener { snapshot ->
                val userData = snapshot.value
                if (userData != null) {
                    cikisYapanRef.setValue(userData).addOnSuccessListener {
                        cikisYapanRef.child("gecmisOdeme").setValue(toplamUcret).addOnSuccessListener {
                            anonimRef.removeValue().addOnSuccessListener {
                                Toast.makeText(this, "✅ Ödeme kaydedildi ve çıkış yapanlara taşındı.", Toast.LENGTH_LONG).show()

                                textUcret.text = "0₺"

                                // Çıkış saatine göre geçen süreyi sabitle
                                val girisTarihi = snapshot.child("giris_tarihi").getValue(String::class.java) ?: "-"
                                val girisSaati = snapshot.child("giris_saati").getValue(String::class.java) ?: "-"
                                val cikisTarihi = snapshot.child("cikis_tarihi").getValue(String::class.java) ?: "-"
                                val cikisSaati = snapshot.child("cikis_saati").getValue(String::class.java) ?: "-"

                                if (girisTarihi != "-" && girisSaati != "-" && cikisTarihi != "-" && cikisSaati != "-") {
                                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                    val girisDate = sdf.parse("$girisTarihi $girisSaati")
                                    val cikisDate = sdf.parse("$cikisTarihi $cikisSaati")
                                    val elapsedMillis = cikisDate.time - girisDate.time
                                    val elapsedHours = TimeUnit.MILLISECONDS.toHours(elapsedMillis)
                                    val elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis) % 60
                                    val elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis) % 60

                                    handler.removeCallbacks(updateRunnable)
                                    textGecenSure.text = String.format(
                                        "Geçen Süre: %02d saat %02d dakika %02d saniye",
                                        elapsedHours, elapsedMinutes, elapsedSeconds
                                    )
                                    textGecenSure.text = "Geçen Süre: 0 saat 0 dakika 0 saniye"
                                    findViewById<TextView>(R.id.textGirisTarihi).text = "Giriş Tarihi: -"
                                    findViewById<TextView>(R.id.textGirisSaati).text = "Giriş Saati: -"
                                }
                            }
                        }
                    }
                }
            }
        }

        findViewById<Button>(R.id.buttonKayitOl).setOnClickListener {
            startActivity(Intent(this, KayitOlActivity::class.java))
        }

        findViewById<Button>(R.id.buttonCikisYap).setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Çıkış yapıldı.", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)

            finish()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        autoScrollHandler.removeCallbacks(autoScrollRunnable)


        if (this::updateRunnable.isInitialized) {
            handler.removeCallbacks(updateRunnable)
        }
    }
}
