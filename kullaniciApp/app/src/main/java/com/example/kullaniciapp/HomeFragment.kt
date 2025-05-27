package com.example.kullaniciapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private lateinit var textGecenSure: TextView
    private lateinit var textKullaniciAdSoyad: TextView
    private lateinit var databaseRef: DatabaseReference
    private var startTimeMillis: Long = 0L
    private val handler = Handler(Looper.getMainLooper())

    private var currentPage = 0
    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private var autoScrollRunnable: Runnable? = null  // ✅ nullable yaptık

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Kullanıcı adı
        textKullaniciAdSoyad = view.findViewById(R.id.textKullaniciAdSoyad)
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid

        if (uid != null) {
            val userRef = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("kullanicilar").child(uid)

            userRef.child("adSoyad").get().addOnSuccessListener { snapshot ->
                val userName = snapshot.getValue(String::class.java)
                val displayName = userName ?: auth.currentUser?.email ?: "Kullanıcı"
                textKullaniciAdSoyad.text = "Hoş geldin, $displayName 👋"
            }.addOnFailureListener {
                val fallbackName = auth.currentUser?.email ?: "Kullanıcı"
                textKullaniciAdSoyad.text = "Hoş geldin, $fallbackName 👋"
            }
        }

        // Animasyon
        val textAnim = view.findViewById<TextView>(R.id.textAnimUyari)
        val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.scroll_text)
        textAnim.startAnimation(anim)

        // Geçen süre text
        textGecenSure = view.findViewById(R.id.textGecenSure)

        // ViewPager2 ve Dots
        val viewPager = view.findViewById<ViewPager2>(R.id.infoViewPager)
        val dotsIndicator = view.findViewById<WormDotsIndicator>(R.id.dotsIndicator)

        // Firebase Realtime Database bağlantısı
        databaseRef = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("systemInfo")

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val infoCards = listOf(
                    InfoCard("Otopark Durumu", snapshot.child("otoparkDurumu").getValue(String::class.java) ?: "-", android.R.drawable.ic_dialog_info),
                    InfoCard("Doluluk", snapshot.child("doluluk").getValue(String::class.java) ?: "-", android.R.drawable.ic_menu_agenda),
                    InfoCard("Kamera Durumu", snapshot.child("kameraDurumu").getValue(String::class.java) ?: "-", android.R.drawable.ic_menu_camera),
                    InfoCard("Giriş/Çıkış", snapshot.child("girisCikis").getValue(String::class.java) ?: "-", android.R.drawable.ic_menu_directions)
                )
                viewPager.adapter = InfoCardAdapter(infoCards)
                dotsIndicator.attachTo(viewPager)

                // Kullanıcı ve park bilgileri
                view.findViewById<TextView>(R.id.textPlaka).text =
                    "Plaka: ${snapshot.child("plaka").getValue(String::class.java) ?: "-"}"

                view.findViewById<TextView>(R.id.textParkAlani).text =
                    "Park Alanı: ${snapshot.child("parkAlani").getValue(String::class.java) ?: "-"}"

                val girisSaatiStr = snapshot.child("girisSaati").getValue(String::class.java) ?: "-"
                view.findViewById<TextView>(R.id.textGirisSaati).text =
                    "Giriş Saati: $girisSaatiStr"

                // Abonelik ve maksimum ücret bilgileri
                val abonelikDurumu = snapshot.child("abonelikDurumu").getValue(String::class.java) ?: "normal"
                val maksimumGunlukUcret = snapshot.child("maksimumGunlukUcret").getValue(Int::class.java) ?: 100

                // Geçen süreyi hesapla ve sayaç başlat
                if (girisSaatiStr != "-") {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    try {
                        val girisDate = sdf.parse(girisSaatiStr)
                        startTimeMillis = girisDate.time
                        handler.post(timerRunnable) // canlı sayaç başlat

                        // Ücret hesaplama
                        val now = Date()
                        val diffMillis = now.time - girisDate.time
                        val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)

                        val toplamUcretText = view.findViewById<TextView>(R.id.textUcret)
                        var toplamUcret = 0
                        if (abonelikDurumu == "abonelikli") {
                            toplamUcretText.text = "Toplam Ücret: Abonelikli (Aylık ödeme dahil)"
                        } else {
                            if (diffMinutes > 30) {
                                val ekSaatler = Math.ceil((diffMinutes - 30).toDouble() / 60).toInt()
                                toplamUcret = ekSaatler * 10
                            }
                            if (toplamUcret > maksimumGunlukUcret) {
                                toplamUcret = maksimumGunlukUcret
                            }
                            toplamUcretText.text = "Toplam Ücret: $toplamUcret₺ (Maksimum günlük)"
                        }

                    } catch (e: Exception) {
                        textGecenSure.text = "Geçen park süresi: Hesaplanamadı"
                    }
                } else {
                    textGecenSure.text = "Geçen park süresi: -"
                }

                view.findViewById<TextView>(R.id.textTarife).text =
                    "Tarife: İlk 30 dk ücretsiz, her ek saat 10₺, maksimum günlük $maksimumGunlukUcret₺"

                view.findViewById<TextView>(R.id.textGecmisOdeme).text =
                    "Geçmiş Ödeme: ${snapshot.child("gecmisOdeme").getValue(String::class.java) ?: "-"}"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Veri alınamadı!", Toast.LENGTH_SHORT).show()
            }
        })

        // Ödeme
        val buttonOde = view.findViewById<Button>(R.id.buttonOde)
        buttonOde.setOnClickListener {
            Toast.makeText(requireContext(), "Ödeme işlemi başlatıldı.", Toast.LENGTH_SHORT).show()
        }

        // Rezervasyon
        val buttonRezervasyon = view.findViewById<Button>(R.id.buttonRezervasyon)
        buttonRezervasyon.setOnClickListener {
            Toast.makeText(requireContext(), "Rezervasyon ekranı yakında!", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timerRunnable)
        autoScrollRunnable?.let {
            autoScrollHandler.removeCallbacks(it)
        }
    }
}
