package com.example.kullaniciapp

<<<<<<< HEAD
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
    private val handler = Handler(Looper.getMainLooper())
    private var startTimeMillis: Long = 0L

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

=======
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

>>>>>>> 44d3236e304bccba2f7b47a1a2eb8beafebd2045
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

<<<<<<< HEAD
        textKullaniciAdSoyad = view.findViewById(R.id.textKullaniciAdSoyad)
        textGecenSure = view.findViewById(R.id.textGecenSure)
        val viewPager = view.findViewById<ViewPager2>(R.id.infoViewPager)
        val dotsIndicator = view.findViewById<WormDotsIndicator>(R.id.dotsIndicator)

        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid

        if (uid != null) {
            val db = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")

            // Kullanıcı adı
            db.getReference("kullanicilar").child(uid).child("adSoyad")
                .get().addOnSuccessListener { snapshot ->
                    val userName = snapshot.getValue(String::class.java)
                    val displayName = userName ?: auth.currentUser?.email ?: "Kullanıcı"
                    textKullaniciAdSoyad.text = "Hoş geldin, $displayName 👋"
                }

            // Info cards
            db.getReference("infoCards").get().addOnSuccessListener { snapshot ->
                val infoCards = snapshot.children.map {
                    val title = it.child("title").getValue(String::class.java) ?: "-"
                    val description = it.child("description").getValue(String::class.java) ?: "-"
                    val icon = when (title) {
                        "Otopark Durumu" -> android.R.drawable.ic_dialog_info
                        "Doluluk" -> android.R.drawable.ic_menu_agenda
                        "Kamera Durumu" -> android.R.drawable.ic_menu_camera
                        "Giriş/Çıkış" -> android.R.drawable.ic_menu_directions
                        else -> android.R.drawable.ic_menu_help
                    }
                    InfoCard(title, description, icon)
                }
                viewPager.adapter = InfoCardAdapter(infoCards)
                dotsIndicator.attachTo(viewPager)
            }

            // Kullanıcı bilgileri
            db.getReference("kullanicilar").child(uid).get().addOnSuccessListener { userSnapshot ->
                val abonelikDurumu = userSnapshot.child("abonelikDurumu").getValue(String::class.java) ?: "normal"
                val maksimumGunlukUcret = userSnapshot.child("maksimumGunlukUcret").getValue(Int::class.java) ?: 100
                val gecmisOdeme = userSnapshot.child("gecmisOdeme").getValue(String::class.java) ?: "-"

                val plakalarNode = userSnapshot.child("plakalar")
                val textPlaka = view.findViewById<TextView>(R.id.textPlaka)
                val textParkAlani = view.findViewById<TextView>(R.id.textParkAlani)
                val textGirisSaati = view.findViewById<TextView>(R.id.textGirisSaati)
                val textUcret = view.findViewById<TextView>(R.id.textUcret)

                val plakaText = StringBuilder()
                val parkAlaniText = StringBuilder()
                val girisSaatiText = StringBuilder()
                val ucretText = StringBuilder()

                var sayaçBaşlatıldı = false

                plakalarNode.children.forEach { plakaSnap ->
                    val plakaKey = plakaSnap.key ?: "-"
                    val alan = plakaSnap.child("alan").getValue(String::class.java) ?: "-"
                    val girisSaatiStr = plakaSnap.child("giris_saati").getValue(String::class.java) ?: "-"

                    plakaText.append("Plaka: $plakaKey\n")
                    parkAlaniText.append("Park Alanı: $alan\n")
                    girisSaatiText.append("Giriş Saati: $girisSaatiStr\n")

                    if (!sayaçBaşlatıldı && girisSaatiStr != "-") {
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        try {
                            val girisDate = sdf.parse(girisSaatiStr)
                            startTimeMillis = girisDate.time
                            handler.post(timerRunnable)
                            sayaçBaşlatıldı = true
                        } catch (_: Exception) {
                            textGecenSure.text = "Geçen park süresi: Hesaplanamadı"
                        }
                    }

                    if (girisSaatiStr != "-") {
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        try {
                            val girisDate = sdf.parse(girisSaatiStr)
                            val now = Date()
                            val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(now.time - girisDate.time)

                            var toplamUcret = 0
                            val hesaplananUcret = if (abonelikDurumu == "abonelikli") {
                                "Abonelikli (Aylık ödeme dahil)"
                            } else {
                                if (diffMinutes > 30) {
                                    val ekSaatler = Math.ceil((diffMinutes - 30).toDouble() / 60).toInt()
                                    toplamUcret = ekSaatler * 10
                                }
                                if (toplamUcret > maksimumGunlukUcret) {
                                    toplamUcret = maksimumGunlukUcret
                                }
                                "$toplamUcret₺ (Maksimum günlük)"
                            }
                            ucretText.append("$plakaKey → $hesaplananUcret\n")
                        } catch (_: Exception) {
                            ucretText.append("$plakaKey → Hesaplanamadı\n")
                        }
                    } else {
                        ucretText.append("$plakaKey → Giriş saati yok\n")
                    }
                }

                textPlaka.text = plakaText.toString().trim()
                textParkAlani.text = parkAlaniText.toString().trim()
                textGirisSaati.text = girisSaatiText.toString().trim()
                textUcret.text = ucretText.toString().trim()

                view.findViewById<TextView>(R.id.textTarife).text =
                    "Tarife: İlk 30 dk ücretsiz, her ek saat 10₺, maksimum günlük $maksimumGunlukUcret₺"

                view.findViewById<TextView>(R.id.textGecmisOdeme).text =
                    "Geçmiş Ödeme: $gecmisOdeme"
            }
        }

        view.findViewById<Button>(R.id.buttonOde).setOnClickListener {
            Toast.makeText(requireContext(), "Ödeme işlemi başlatıldı.", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.buttonRezervasyon).setOnClickListener {
            Toast.makeText(requireContext(), "Rezervasyon ekranı yakında!", Toast.LENGTH_SHORT).show()
=======
        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        val textWelcome = view.findViewById<TextView>(R.id.textWelcome)
        val buttonLogout = view.findViewById<Button>(R.id.buttonLogout)

        textWelcome.text = "Hoş geldin, $userEmail"

        buttonLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), MainActivity::class.java))
            activity?.finish()
>>>>>>> 44d3236e304bccba2f7b47a1a2eb8beafebd2045
        }

        return view
    }
<<<<<<< HEAD

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timerRunnable)
    }
=======
>>>>>>> 44d3236e304bccba2f7b47a1a2eb8beafebd2045
}
