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
import com.google.firebase.firestore.FirebaseFirestore
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator

class HomeFragment : Fragment() {

    private lateinit var textGecenSure: TextView
    private lateinit var textKullaniciAdSoyad: TextView
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // 👤 Kullanıcı adı
        textKullaniciAdSoyad = view.findViewById(R.id.textKullaniciAdSoyad)
        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        val displayName = userEmail ?: "Kullanıcı"
        textKullaniciAdSoyad.text = "Hoş geldin, $displayName 👋"

        // 🔔 Animasyon
        val textAnim = view.findViewById<TextView>(R.id.textAnimUyari)
        val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.scroll_text)
        textAnim.startAnimation(anim)

        // ⏱ Geçen süre
        textGecenSure = view.findViewById(R.id.textGecenSure)

        // 🔄 ViewPager2 ve Dots
        val viewPager = view.findViewById<ViewPager2>(R.id.infoViewPager)
        val dotsIndicator = view.findViewById<WormDotsIndicator>(R.id.dotsIndicator)

        val infoCards = listOf(
            InfoCard("Otopark Durumu", "Açık – Giriş serbest", android.R.drawable.ic_dialog_info),
            InfoCard("Doluluk", "18/30 dolu (A1-A10 dolu)", android.R.drawable.ic_menu_agenda),
            InfoCard("Kamera Durumu", "Kamera sistemi aktif", android.R.drawable.ic_menu_camera),
            InfoCard("Giriş/Çıkış", "Giriş açık – Çıkış kapalı", android.R.drawable.ic_menu_directions)
        )

        viewPager.adapter = InfoCardAdapter(infoCards)
        dotsIndicator.attachTo(viewPager)

        // ✅ Otomatik scroll başlat
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

        // 📦 Firestore'dan kullanıcı verisini çek
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(uid)

            userRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    view.findViewById<TextView>(R.id.textPlaka).text =
                        "Plaka: ${document.getString("plaka") ?: "-"}"

                    view.findViewById<TextView>(R.id.textGirisSaati).text =
                        "Giriş Saati: ${document.getString("giris_saati") ?: "-"}"

                    view.findViewById<TextView>(R.id.textParkAlani).text =
                        "Park Alanı: ${document.getString("park_alani") ?: "-"}"

                    view.findViewById<TextView>(R.id.textUcret).text =
                        "Toplam Ücret: ${document.getString("toplam_ucret") ?: "-"}"

                    view.findViewById<TextView>(R.id.textTarife).text =
                        "Tarife: İlk 30 dk ücretsiz, her ek saat 10₺"

                    view.findViewById<TextView>(R.id.textGecmisOdeme).text =
                        "Geçmiş Ödeme: ${document.getString("gecmis_odeme") ?: "-"}"
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Veri alınamadı!", Toast.LENGTH_SHORT).show()
            }
        }

        // 💸 Ödeme
        val buttonOde = view.findViewById<Button>(R.id.buttonOde)
        buttonOde.setOnClickListener {
            Toast.makeText(requireContext(), "Ödeme işlemi başlatıldı.", Toast.LENGTH_SHORT).show()
        }

        // 📆 Rezervasyon
        val buttonRezervasyon = view.findViewById<Button>(R.id.buttonRezervasyon)
        buttonRezervasyon.setOnClickListener {
            Toast.makeText(requireContext(), "Rezervasyon ekranı yakında!", Toast.LENGTH_SHORT).show()
        }

        // Sayaç başlat
        startTimeMillis = System.currentTimeMillis() - (12 * 60 * 1000)
        handler.post(timerRunnable)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timerRunnable)
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
    }
}
