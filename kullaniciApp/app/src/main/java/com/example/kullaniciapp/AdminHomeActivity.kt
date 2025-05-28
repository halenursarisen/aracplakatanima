package com.example.kullaniciapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*


class AdminHomeActivity : AppCompatActivity() {

    private lateinit var buttonPlakaIslemleri: Button
    private lateinit var buttonLogout: Button
    private lateinit var plakaList: MutableList<String>
    private lateinit var adapter: PlateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_home)

        buttonPlakaIslemleri = findViewById(R.id.buttonPlakaIslemleri)
        buttonLogout = findViewById(R.id.buttonLogout)
        val buttonCikisListesi = findViewById<Button>(R.id.buttonCikisListesi)
        val buttonYeniKullanici = findViewById<Button>(R.id.buttonYeniKullanici)

        buttonPlakaIslemleri.setOnClickListener {
            showPlakaDialog()
        }

        buttonLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        buttonYeniKullanici.setOnClickListener {
            showYeniKullaniciDialog()
        }

        buttonCikisListesi.setOnClickListener {
            showCikisYapanKullanicilarDialog()
        }
    }

    private fun showPlakaDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_plates, null)
        val listView = dialogView.findViewById<ListView>(R.id.listViewPlates)
        val editTextSearchPlaka = dialogView.findViewById<EditText>(R.id.editTextSearchPlaka)
        val buttonSearchPlaka = dialogView.findViewById<Button>(R.id.buttonSearchPlaka)

        plakaList = mutableListOf()
        adapter = PlateAdapter(this, plakaList) { selectedPlaka ->
            showEditPlakaDialog(selectedPlaka)
        }
        listView.adapter = adapter

        val db = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")

        // Hem kullanıcı hem anonim çek
        db.getReference("kullanicilar").get().addOnSuccessListener { snapshot ->
            snapshot.children.forEach { userSnap ->
                val plakalarSnap = userSnap.child("plakalar")
                plakalarSnap.children.forEach { plakaSnap ->
                    val plaka = plakaSnap.key
                    plaka?.let { if (!plakaList.contains(it)) plakaList.add(it) }
                }
            }

            db.getReference("anonimKullanicilar").get().addOnSuccessListener { anonSnapshot ->
                anonSnapshot.children.forEach { anonSnap ->
                    val plaka = anonSnap.child("plaka").getValue(String::class.java)
                    plaka?.let { if (!plakaList.contains(it)) plakaList.add(it) }
                }

                adapter.notifyDataSetChanged()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "❌ Hata: ${it.message}", Toast.LENGTH_SHORT).show()
        }

        // Liste içinden seçim
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedPlaka = plakaList[position]
            showEditPlakaDialog(selectedPlaka)
        }

        // Elle arama kısmı
        buttonSearchPlaka.setOnClickListener {
            val girilenPlaka = editTextSearchPlaka.text.toString().trim()
            if (girilenPlaka.isEmpty()) {
                Toast.makeText(this, "❌ Lütfen bir plaka girin!", Toast.LENGTH_SHORT).show()
            } else if (plakaList.contains(girilenPlaka)) {
                showEditPlakaDialog(girilenPlaka)
            } else {
                Toast.makeText(this, "❌ Plaka bulunamadı!", Toast.LENGTH_SHORT).show()
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Tüm Plakalar")
            .setView(dialogView)
            .setNegativeButton("Kapat", null)
            .create()
            .show()
    }

    private fun showEditPlakaDialog(selectedPlaka: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_plate, null)
        val inputGirisTarihi = dialogView.findViewById<EditText>(R.id.inputGirisTarihi)
        val inputGiris = dialogView.findViewById<EditText>(R.id.inputGirisSaati)
        val inputCikisTarihi = dialogView.findViewById<EditText>(R.id.inputCikisTarihi)
        val inputCikis = dialogView.findViewById<EditText>(R.id.inputCikisSaati)
        val spinnerKat = dialogView.findViewById<Spinner>(R.id.spinnerKat)
        val spinnerAlan = dialogView.findViewById<Spinner>(R.id.spinnerAlan)

        val db = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        val kullanicilarRef = db.getReference("kullanicilar")
        val anonimRef = db.getReference("anonimKullanicilar")

        // Spinnerları doldur
        val katlarRef = db.getReference("otopark_duzeni")
        katlarRef.get().addOnSuccessListener { katSnapshot ->
            val katList = mutableListOf<String>()
            katSnapshot.children.forEach { katSnap ->
                katSnap.key?.let { katList.add(it) }
            }
            val katAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, katList)
            katAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerKat.adapter = katAdapter

            // Kat seçilince alanları çek ve spinnerAlan'a yükle
            spinnerKat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val secilenKat = katList[position]
                    val alanList = mutableListOf<String>()
                    katSnapshot.child(secilenKat).children.forEach { alanSnap ->
                        alanSnap.key?.let { alanList.add(it) }
                    }
                    val alanAdapter = ArrayAdapter(this@AdminHomeActivity, android.R.layout.simple_spinner_item, alanList)
                    alanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerAlan.adapter = alanAdapter
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            // Mevcut kaydın kat ve alan bilgilerini spinnerlarda seçili yap
            kullanicilarRef.get().addOnSuccessListener { snapshot ->
                var found = false
                for (userSnap in snapshot.children) {
                    val plakalarSnap = userSnap.child("plakalar")
                    if (plakalarSnap.hasChild(selectedPlaka)) {
                        val detaySnap = plakalarSnap.child(selectedPlaka)
                        val katValue = detaySnap.child("kat").getValue(String::class.java) ?: ""
                        val alanValue = detaySnap.child("alan").getValue(String::class.java) ?: ""

                        val katIndex = katList.indexOf(katValue)
                        if (katIndex >= 0) spinnerKat.setSelection(katIndex)

                        // Alan spinner güncellemesi
                        val alanList = mutableListOf<String>()
                        katSnapshot.child(katValue).children.forEach { alanSnap ->
                            alanSnap.key?.let { alanList.add(it) }
                        }
                        val alanAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, alanList)
                        alanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerAlan.adapter = alanAdapter

                        val alanIndex = alanList.indexOf(alanValue)
                        if (alanIndex >= 0) spinnerAlan.setSelection(alanIndex)

                        found = true
                        break
                    }
                }

                if (!found) {
                    anonimRef.get().addOnSuccessListener { anonSnapshot ->
                        for (anonSnap in anonSnapshot.children) {
                            if (anonSnap.child("plaka").getValue(String::class.java) == selectedPlaka) {
                                val katValue = anonSnap.child("kat").getValue(String::class.java) ?: ""
                                val alanValue = anonSnap.child("alan").getValue(String::class.java) ?: ""

                                val katIndex = katList.indexOf(katValue)
                                if (katIndex >= 0) spinnerKat.setSelection(katIndex)

                                val alanList = mutableListOf<String>()
                                katSnapshot.child(katValue).children.forEach { alanSnap ->
                                    alanSnap.key?.let { alanList.add(it) }
                                }
                                val alanAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, alanList)
                                alanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                spinnerAlan.adapter = alanAdapter

                                val alanIndex = alanList.indexOf(alanValue)
                                if (alanIndex >= 0) spinnerAlan.setSelection(alanIndex)
                                break
                            }
                        }
                    }
                }
            }
        }

        kullanicilarRef.get().addOnSuccessListener { snapshot ->
            var found = false
            for (userSnap in snapshot.children) {
                val plakalarSnap = userSnap.child("plakalar")
                if (plakalarSnap.hasChild(selectedPlaka)) {
                    val plakaRef = plakalarSnap.child(selectedPlaka).ref
                    val detaySnap = plakalarSnap.child(selectedPlaka)
                    fillAndShowDialog(
                        plakaRef, selectedPlaka, detaySnap, dialogView,
                        inputGirisTarihi, inputGiris, inputCikisTarihi, inputCikis, spinnerKat, spinnerAlan
                    )
                    found = true
                    break
                }
            }

            if (!found) {
                anonimRef.get().addOnSuccessListener { anonSnapshot ->
                    for (anonSnap in anonSnapshot.children) {
                        if (anonSnap.child("plaka").getValue(String::class.java) == selectedPlaka) {
                            val plakaRef = anonSnap.ref
                            fillAndShowDialog(
                                plakaRef, selectedPlaka, anonSnap, dialogView,
                                inputGirisTarihi, inputGiris, inputCikisTarihi, inputCikis, spinnerKat, spinnerAlan
                            )
                            break
                        }
                    }
                }
            }
        }
    }

    private fun fillAndShowDialog(
        ref: DatabaseReference,
        plaka: String,
        snapshot: DataSnapshot,
        dialogView: View,
        inputGirisTarihi: EditText,
        inputGiris: EditText,
        inputCikisTarihi: EditText,
        inputCikis: EditText,
        spinnerKat: Spinner,
        spinnerAlan: Spinner
    ) {
        inputGirisTarihi.setText(snapshot.child("giris_tarihi").getValue(String::class.java) ?: "")
        inputGiris.setText(snapshot.child("giris_saati").getValue(String::class.java) ?: "")
        inputCikisTarihi.setText(snapshot.child("cikis_tarihi").getValue(String::class.java) ?: "")
        inputCikis.setText(snapshot.child("cikis_saati").getValue(String::class.java) ?: "")

        // Kat ve alan bilgisi spinnerlarda gösteriliyor. Boşsa default seçili kalır
        val katValue = snapshot.child("kat").getValue(String::class.java) ?: ""
        val alanValue = snapshot.child("alan").getValue(String::class.java) ?: ""

        val katAdapter = spinnerKat.adapter as ArrayAdapter<String>
        val katIndex = katAdapter.getPosition(katValue)
        if (katIndex >= 0) spinnerKat.setSelection(katIndex)

        // Alan spinner güncellemesi
        val alanList = mutableListOf<String>()
        val db = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        val katRef = db.getReference("otopark_duzeni").child(katValue)
        katRef.get().addOnSuccessListener { alanSnapshot ->
            alanSnapshot.children.forEach { alanSnap ->
                alanSnap.key?.let { alanList.add(it) }
            }
            val alanAdapter = ArrayAdapter(dialogView.context, android.R.layout.simple_spinner_item, alanList)
            alanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerAlan.adapter = alanAdapter

            val alanIndex = alanAdapter.getPosition(alanValue)
            if (alanIndex >= 0) spinnerAlan.setSelection(alanIndex)
        }

        AlertDialog.Builder(this)
            .setTitle("Plaka: $plaka")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                val girisTarihiStr = inputGirisTarihi.text.toString()
                val girisSaatiStr = inputGiris.text.toString()
                val cikisTarihiStr = inputCikisTarihi.text.toString()
                val cikisSaatiStr = inputCikis.text.toString()
                val katStr = spinnerKat.selectedItem?.toString() ?: ""
                val alanStr = spinnerAlan.selectedItem?.toString() ?: ""

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                try {
                    val girisDate = dateFormat.parse(girisTarihiStr)
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                    if (girisDate != null && girisDate.before(today)) {
                        Toast.makeText(dialogView.context, "❌ Geçmiş tarih kaydedilemez!", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val yeniVeri = mapOf(
                        "giris_tarihi" to girisTarihiStr,
                        "giris_saati" to girisSaatiStr,
                        "cikis_tarihi" to cikisTarihiStr,
                        "cikis_saati" to cikisSaatiStr,
                        "kat" to katStr,
                        "alan" to alanStr
                    )

                    if (cikisSaatiStr.isNotBlank()) {
                        val db = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
                        val archiveRef = db.getReference("cikisYapanKullanicilar").child(plaka).push()

                        val archiveData = mapOf(
                            "giris_tarihi" to girisTarihiStr,
                            "giris_saati" to girisSaatiStr,
                            "cikis_tarihi" to cikisTarihiStr,
                            "cikis_saati" to cikisSaatiStr,
                            "kat" to katStr,
                            "alan" to alanStr,
                            "kaydedilme_zamani" to SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                        )

                        archiveRef.setValue(archiveData).addOnSuccessListener {
                            Toast.makeText(dialogView.context, "✅ Çıkış arşivine eklendi!", Toast.LENGTH_SHORT).show()

                            val resetData = mapOf(
                                "giris_tarihi" to "",
                                "giris_saati" to "",
                                "cikis_tarihi" to "",
                                "cikis_saati" to "",
                                "kat" to "",
                                "alan" to ""
                            )
                            ref.updateChildren(resetData).addOnSuccessListener {
                                Toast.makeText(dialogView.context, "✅ Mevcut kayıt sıfırlandı!", Toast.LENGTH_SHORT).show()
                                updateOtoparkDurumuBosalt(katStr, alanStr)
                            }.addOnFailureListener { e ->
                                Toast.makeText(dialogView.context, "❌ Mevcut kayıt sıfırlanamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }.addOnFailureListener { e ->
                            Toast.makeText(dialogView.context, "❌ Arşivleme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        ref.updateChildren(yeniVeri).addOnSuccessListener {
                            Toast.makeText(dialogView.context, "✅ Güncellendi!", Toast.LENGTH_SHORT).show()
                            updateOtoparkDurumuDolu(katStr, alanStr)
                        }.addOnFailureListener { e ->
                            Toast.makeText(dialogView.context, "❌ Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                } catch (e: Exception) {
                    Toast.makeText(dialogView.context, "❌ Geçersiz tarih formatı!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun updateOtoparkDurumuBosalt(kat: String, alan: String) {
        if (kat.isBlank() || alan.isBlank()) return
        val db = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        val otoparkRef = db.getReference("otopark_duzeni").child(kat).child(alan)
        otoparkRef.setValue(false).addOnSuccessListener {
            Toast.makeText(this, "✅ Otopark yeri boşaltıldı: $kat - $alan", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "❌ Otopark yeri boşaltılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateOtoparkDurumuDolu(kat: String, alan: String) {
        if (kat.isBlank() || alan.isBlank()) return
        val db = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        val otoparkRef = db.getReference("otopark_duzeni").child(kat).child(alan)
        otoparkRef.setValue(true).addOnSuccessListener {
            Toast.makeText(this, "✅ Otopark yeri dolu olarak işaretlendi: $kat - $alan", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "❌ Otopark yeri işaretlenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }






        private fun showCikisYapanKullanicilarDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_plates, null)
        val listView = dialogView.findViewById<ListView>(R.id.listViewPlates)
        val plakaList = mutableListOf<String>()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, plakaList)
        listView.adapter = adapter

        val db = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        db.getReference("cikisYapanKullanicilar").get().addOnSuccessListener { snapshot ->
            snapshot.children.forEach { plakaSnap ->
                val plaka = plakaSnap.key ?: ""
                plakaList.add(plaka)
            }
            adapter.notifyDataSetChanged()

            listView.setOnItemClickListener { _, _, position, _ ->
                val selectedPlaka = plakaList[position]
                showPlakaCikisDetayDialog(selectedPlaka)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "❌ Çıkış listesi yüklenemedi!", Toast.LENGTH_SHORT).show()
        }

        AlertDialog.Builder(this)
            .setTitle("Çıkış Yapan Kullanıcılar")
            .setView(dialogView)
            .setNegativeButton("Kapat", null)
            .create()
            .show()
    }

    private fun showPlakaCikisDetayDialog(plaka: String) {
        val db = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        db.getReference("cikisYapanKullanicilar").child(plaka).get().addOnSuccessListener { snapshot ->
            val detayList = StringBuilder()
            snapshot.children.forEach { kayitSnap ->
                if (kayitSnap.hasChild("giris_tarihi")) {
                    val girisTarihi = kayitSnap.child("giris_tarihi").getValue(String::class.java) ?: "-"
                    val girisSaati = kayitSnap.child("giris_saati").getValue(String::class.java) ?: "-"
                    val cikisTarihi = kayitSnap.child("cikis_tarihi").getValue(String::class.java) ?: "-"
                    val cikisSaati = kayitSnap.child("cikis_saati").getValue(String::class.java) ?: "-"

                    detayList.append("Giriş: $girisTarihi $girisSaati\nÇıkış: $cikisTarihi $cikisSaati\n\n")
                }
            }

            AlertDialog.Builder(this)
                .setTitle("Detay - $plaka")
                .setMessage(detayList.toString())
                .setNegativeButton("Kapat", null)
                .create()
                .show()
        }.addOnFailureListener {
            Toast.makeText(this, "❌ Detaylar yüklenemedi!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showYeniKullaniciDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_yeni_kullanici, null)
        val inputEmail = dialogView.findViewById<EditText>(R.id.inputYeniEmail)
        val inputPassword = dialogView.findViewById<EditText>(R.id.inputYeniPassword)
        val inputPlaka = dialogView.findViewById<EditText>(R.id.inputYeniPlaka)

        AlertDialog.Builder(this)
            .setTitle("Yeni Kullanıcı Kaydı")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                val email = inputEmail.text.toString().trim()
                val password = inputPassword.text.toString().trim()
                val plaka = inputPlaka.text.toString().trim()

                if (email.isEmpty() || password.isEmpty() || plaka.isEmpty()) {
                    Toast.makeText(this, "❌ Tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val auth = FirebaseAuth.getInstance()
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        val uid = it.user?.uid ?: return@addOnSuccessListener
                        val db = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
                        val userRef = db.getReference("kullanicilar").child(uid)

                        val userData = mapOf(
                            "email" to email,
                            "plakalar" to mapOf(
                                plaka to mapOf(
                                    "giris_tarihi" to "",
                                    "giris_saati" to "",
                                    "cikis_tarihi" to "",
                                    "cikis_saati" to "",
                                    "kat" to "",
                                    "alan" to ""
                                )
                            )
                        )

                        userRef.setValue(userData).addOnSuccessListener {
                            Toast.makeText(this, "✅ Kullanıcı kaydedildi!", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener { e ->
                            Toast.makeText(this, "❌ Veri kaydedilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "❌ Kullanıcı oluşturulamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("İptal", null)
            .create()
            .show()
    }
}
