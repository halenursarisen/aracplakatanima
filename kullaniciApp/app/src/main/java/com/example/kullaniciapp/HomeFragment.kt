package com.example.kullaniciapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.view.animation.AnimationUtils

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // ✅ Kayan animasyonu başlat
        val textAnim = view.findViewById<TextView>(R.id.textAnimUyari)
        val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.scroll_text)
        textAnim.startAnimation(anim)

        val db = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid

        textKullaniciAdSoyad = view.findViewById(R.id.textKullaniciAdSoyad)
        textGecenSure = view.findViewById(R.id.textGecenSure)
        val viewPager = view.findViewById<ViewPager2>(R.id.infoViewPager)
        val dotsIndicator = view.findViewById<WormDotsIndicator>(R.id.dotsIndicator)

        if (uid != null) {
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
                val abonelikDurumu =
                    userSnapshot.child("abonelikDurumu").getValue(String::class.java) ?: "normal"
                val maksimumGunlukUcret =
                    userSnapshot.child("maksimumGunlukUcret").getValue(Int::class.java) ?: 100
                val gecmisOdeme =
                    userSnapshot.child("gecmisOdeme").getValue(String::class.java) ?: "-"

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
                    val girisSaatiStr =
                        plakaSnap.child("giris_saati").getValue(String::class.java) ?: "-"
                    val girisTarihiStr =
                        plakaSnap.child("giris_tarihi").getValue(String::class.java) ?: "-"

                    plakaText.append("Plaka: $plakaKey\n")
                    parkAlaniText.append("Park Alanı: $alan\n")
                    girisSaatiText.append("Giriş Tarihi: $girisTarihiStr, Saat: $girisSaatiStr\n")

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
                            val diffMinutes =
                                TimeUnit.MILLISECONDS.toMinutes(now.time - girisDate.time)

                            var toplamUcret = 0
                            val hesaplananUcret = if (abonelikDurumu == "abonelikli") {
                                "Abonelikli (Aylık ödeme dahil)"
                            } else {
                                if (diffMinutes > 30) {
                                    val ekSaatler =
                                        Math.ceil((diffMinutes - 30).toDouble() / 60).toInt()
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

                view.findViewById<Button>(R.id.buttonOde).setOnClickListener {
                    val toplamUcret = view.findViewById<TextView>(R.id.textUcret).text.toString()
                    val kullaniciRef = db.getReference("kullanicilar").child(uid!!)
                    val cikisYapanRef = db.getReference("cikisYapanKullanicilar").child(uid)

                    kullaniciRef.get().addOnSuccessListener { userSnapshot ->
                        val userData = userSnapshot.value

                        if (userData != null) {
                            // ✅ Çıkış yapanlara TAM kullanıcı verisini kaydet
                            cikisYapanRef.setValue(userData).addOnSuccessListener {
                                cikisYapanRef.child("gecmisOdeme").setValue(toplamUcret)

                                // ✅ plakalar altındaki her plaka için alt alanları sıfırla
                                val plakalarNode = userSnapshot.child("plakalar")
                                for (plakaSnap in plakalarNode.children) {
                                    val plakaKey = plakaSnap.key ?: continue
                                    val plakaRef = kullaniciRef.child("plakalar").child(plakaKey)

                                    plakaRef.child("giris_tarihi").setValue("")
                                    plakaRef.child("giris_saati").setValue("")
                                    plakaRef.child("cikis_tarihi").setValue("")
                                    plakaRef.child("cikis_saati").setValue("")
                                    plakaRef.child("alan").setValue("")
                                    plakaRef.child("kat").setValue("")
                                }

                                // ✅ Ekrandaki değerleri sıfırla
                                view.findViewById<TextView>(R.id.textUcret)?.text = "0₺"
                                view.findViewById<TextView>(R.id.textGecenSure)?.text = "Geçen Süre: 0 saat 0 dakika 0 saniye"
                                view.findViewById<TextView>(R.id.textGirisTarihi)?.text = "Giriş Tarihi: "
                                view.findViewById<TextView>(R.id.textGirisSaati)?.text = "Giriş Saati: "
                                view.findViewById<TextView>(R.id.textParkAlani)?.text = "Park Alanı: "


                                Toast.makeText(requireContext(), "✅ Ödeme kaydedildi, plakaların alt verileri sıfırlandı.", Toast.LENGTH_LONG).show()
                            }.addOnFailureListener {
                                Toast.makeText(requireContext(), "❌ Çıkış yapanlara kayıt başarısız: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }



            }
                setupRezervasyonInfoIcon(view, db, uid)
        }


        view.findViewById<Button>(R.id.buttonOde).setOnClickListener {
            Toast.makeText(requireContext(), "Ödeme işlemi başlatıldı.", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<Button>(R.id.buttonRezervasyon).setOnClickListener {

            val rezervasyonRef = db.getReference("rezervasyonlar")
            val kullaniciPlakalarRef = db.getReference("kullanicilar").child(uid!!).child("plakalar")

            // Önce plakaları çekelim
            kullaniciPlakalarRef.get().addOnSuccessListener { snapshot ->
                val plakaListesi = snapshot.children.mapNotNull { it.key }

                if (plakaListesi.isEmpty()) {
                    Toast.makeText(requireContext(), "❗ Kayıtlı plakanız yok.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // Spinner dialogu hazırla
                val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_plaka_spinner, null)
                val spinner = dialogView.findViewById<Spinner>(R.id.spinnerPlaka)

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, plakaListesi)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter

                AlertDialog.Builder(requireContext())
                    .setTitle("Plaka Seçimi")
                    .setView(dialogView)
                    .setPositiveButton("Devam") { _, _ ->
                        val secilenPlaka = spinner.selectedItem as String

                        // Aktif rezervasyon var mı kontrol et
                        rezervasyonRef.get().addOnSuccessListener { rezSnapshot ->
                            var aktifVar = false

                            for (rezSnap in rezSnapshot.children) {
                                val kullaniciId = rezSnap.child("kullanici_id").getValue(String::class.java)
                                val durum = rezSnap.child("durum").getValue(String::class.java)

                                if (kullaniciId == uid && durum == "aktif") {
                                    aktifVar = true
                                    break
                                }
                            }

                            if (aktifVar) {
                                Toast.makeText(requireContext(), "❗ Zaten aktif bir rezervasyon var, yeni oluşturulamaz.", Toast.LENGTH_LONG).show()
                            } else {
                                // Başlangıç zamanı seç
                                showDateTimePicker { baslangicSaat ->
                                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    val start = sdf.parse(baslangicSaat)
                                    val now = Date()

                                    if (start.before(now)) {
                                        Toast.makeText(requireContext(), "❗ Başlangıç saati geçmişte olamaz.", Toast.LENGTH_LONG).show()
                                        return@showDateTimePicker
                                    }

                                    // Bitiş zamanı seç
                                    showDateTimePicker { bitisSaat ->
                                        val end = sdf.parse(bitisSaat)

                                        if (end.before(start)) {
                                            Toast.makeText(requireContext(), "❗ Bitiş saati, başlangıçtan önce olamaz.", Toast.LENGTH_LONG).show()
                                            return@showDateTimePicker
                                        }

                                        // Boş yer bul ve ata
                                        findAndAssignSpot(db.getReference("otopark_duzeni")) { kat, alan ->
                                            if (kat != null && alan != null) {
                                                val yeniRezId = rezervasyonRef.push().key ?: return@findAndAssignSpot
                                                val rezervasyonVeri = mapOf(
                                                    "kullanici_id" to uid,
                                                    "plaka" to secilenPlaka,
                                                    "kat" to kat,
                                                    "alan" to alan,
                                                    "baslangic" to baslangicSaat,
                                                    "bitis" to bitisSaat,
                                                    "durum" to "aktif"
                                                )

                                                rezervasyonRef.child(yeniRezId).setValue(rezervasyonVeri)
                                                    .addOnSuccessListener {
                                                        db.getReference("otopark_duzeni").child(kat).child(alan).child("durum").setValue("rezerve")
                                                        Toast.makeText(requireContext(), "✅ Rezervasyon yapıldı: $kat - $alan", Toast.LENGTH_LONG).show()
                                                    }
                                                    .addOnFailureListener {
                                                        Toast.makeText(requireContext(), "❌ Rezervasyon kaydedilemedi: ${it.message}", Toast.LENGTH_LONG).show()
                                                    }
                                            } else {
                                                Toast.makeText(requireContext(), "❗ Rezerve edilebilir boş alan kalmadı.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                            }
                        }.addOnFailureListener {
                            Toast.makeText(requireContext(), "Rezervasyon durumu kontrol edilemedi: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    .setNegativeButton("İptal", null)
                    .show()
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Plakalar çekilemedi: ${it.message}", Toast.LENGTH_LONG).show()
            }

        }



        return view
    }

    private fun setupRezervasyonInfoIcon(view: View, db: FirebaseDatabase, uid: String) {
        val iconRezervasyonBilgi = view.findViewById<ImageView>(R.id.iconRezervasyonInfo)
        val rezervasyonRef = db.getReference("rezervasyonlar")

        iconRezervasyonBilgi.setOnClickListener {
            rezervasyonRef.get()
                .addOnSuccessListener { snapshot ->
                    var aktifRezBilgi: String? = null

                    for (rezSnap in snapshot.children) {
                        val kullaniciId = rezSnap.child("kullanici_id").getValue(String::class.java)
                        val durum = rezSnap.child("durum").getValue(String::class.java)

                        if (kullaniciId == uid && durum == "aktif") {
                            val plaka = rezSnap.child("plaka").getValue(String::class.java) ?: "-"
                            val kat = rezSnap.child("kat").getValue(String::class.java) ?: "-"
                            val alan = rezSnap.child("alan").getValue(String::class.java) ?: "-"
                            val baslangic = rezSnap.child("baslangic").getValue(String::class.java) ?: "-"
                            val bitis = rezSnap.child("bitis").getValue(String::class.java) ?: "-"

                            aktifRezBilgi = """
                                Plaka: $plaka
                                Kat: $kat
                                Alan: $alan
                                Başlangıç: $baslangic
                                Bitiş: $bitis
                            """.trimIndent()
                            break
                        }
                    }

                    if (aktifRezBilgi != null) {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Rezervasyon Bilgisi")
                            .setMessage(aktifRezBilgi)
                            .setPositiveButton("Tamam", null)
                            .setNegativeButton("❌ İptal Et") { _, _ ->
                                // Aktif rezervasyon ID’sini bul ve durumu iptal yap
                                rezervasyonRef.get().addOnSuccessListener { snapshot ->
                                    for (rezSnap in snapshot.children) {
                                        val kullaniciId = rezSnap.child("kullanici_id").getValue(String::class.java)
                                        val durum = rezSnap.child("durum").getValue(String::class.java)

                                        if (kullaniciId == uid && durum == "aktif") {
                                            val rezId = rezSnap.key
                                            val kat = rezSnap.child("kat").getValue(String::class.java)
                                            val alan = rezSnap.child("alan").getValue(String::class.java)

                                            if (rezId != null && kat != null && alan != null) {
                                                rezervasyonRef.child(rezId).child("durum").setValue("iptal")
                                                db.getReference("otopark_duzeni").child(kat).child(alan).child("durum").setValue("bos")
                                                Toast.makeText(requireContext(), "✅ Rezervasyon iptal edildi.", Toast.LENGTH_LONG).show()
                                            }
                                            break
                                        }
                                    }
                                }
                            }
                            .show()
                    } else {
                        Toast.makeText(requireContext(), "Aktif rezervasyon bulunamadı.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Rezervasyon bilgisi çekilemedi: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timerRunnable)
    }

    fun findAndAssignSpot(otoparkRef: DatabaseReference, onResult: (String?, String?) -> Unit) {
        otoparkRef.child("Kat2").get().addOnSuccessListener { snapshot ->
            for (spotSnap in snapshot.children) {
                val tip = spotSnap.child("tip").getValue(String::class.java)
                val durum = spotSnap.child("durum").getValue(String::class.java)
                if (tip == "rezerve" && durum == "bos") {
                    onResult("Kat2", spotSnap.key!!)
                    return@addOnSuccessListener
                }
            }

            otoparkRef.child("Kat3").get().addOnSuccessListener { snap ->
                for (spotSnap in snap.children) {
                    val tip = spotSnap.child("tip").getValue(String::class.java)
                    val durum = spotSnap.child("durum").getValue(String::class.java)
                    if (tip == "rezerve" && durum == "bos") {
                        onResult("Kat3", spotSnap.key!!)
                        return@addOnSuccessListener
                    }
                }
                onResult(null, null)
            }
        }
    }

    fun showDateTimePicker(onDateTimeSelected: (String) -> Unit) {
        val now = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, month, day)

            TimePickerDialog(requireContext(), { _, hour, minute ->
                selectedDate.set(Calendar.HOUR_OF_DAY, hour)
                selectedDate.set(Calendar.MINUTE, minute)

                if (selectedDate.before(Calendar.getInstance())) {
                    Toast.makeText(requireContext(), "❗ Geçmiş tarih/saat seçemezsiniz.", Toast.LENGTH_LONG).show()
                    return@TimePickerDialog
                }

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val formatted = dateFormat.format(selectedDate.time)
                onDateTimeSelected(formatted)
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()

        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).apply {
            datePicker.minDate = now.timeInMillis
        }.show()
    }
}
