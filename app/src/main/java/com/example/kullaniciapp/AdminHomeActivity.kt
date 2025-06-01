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
import android.os.LocaleList
import android.text.InputType


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
        val buttonKullanicilar = findViewById<Button>(R.id.buttonKullanicilar)

        buttonKullanicilar.setOnClickListener {
            showKullanicilarDialog()
        }
        val buttonInfoKartDuzenle = findViewById<Button>(R.id.buttonInfoKartDuzenle)
        buttonInfoKartDuzenle.setOnClickListener {
            showInfoKartDuzenleDialog()
        }
        val buttonYeniBildirim = findViewById<Button>(R.id.buttonYeniBildirim)
        buttonYeniBildirim.setOnClickListener {
            showBildirimlerDialog()
        }
        val buttonChatbotMesajlar = findViewById<Button>(R.id.buttonChatbotMesajlar)
        buttonChatbotMesajlar.setOnClickListener {
            showChatbotMessagesDialog()
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
            val girilenPlakaRaw = editTextSearchPlaka.text.toString().trim()
            val girilenPlaka = girilenPlakaRaw.replace("\\s".toRegex(), "").uppercase()


            val turkPlakaRegex = Regex("^[0-9]{2}[A-Z]{1,3}[0-9]{2,4}$")
                val yabanciPlakaRegex = Regex("^[A-Z0-9]{5,10}$")


                if (!turkPlakaRegex.matches(girilenPlaka) && !yabanciPlakaRegex.matches(girilenPlaka)) {
                    Toast.makeText(this, "❗ Geçerli bir Türk veya yabancı plaka girin!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                else if (plakaList.contains(girilenPlaka)) {
                    showEditPlakaDialog(girilenPlaka)
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Plaka bulunamadı")
                    .setMessage("$girilenPlaka plakası anonim olarak eklensin mi?")
                    .setPositiveButton("Evet") { _, _ ->
                        val yeniAnonimRef = db.getReference("anonimKullanicilar").push()
                        val anonimVeri = mapOf(
                            "plaka" to girilenPlaka,
                            "giris_tarihi" to "",
                            "giris_saati" to "",
                            "cikis_tarihi" to "",
                            "cikis_saati" to "",
                            "kat" to "",
                            "alan" to ""
                        )
                        yeniAnonimRef.setValue(anonimVeri).addOnSuccessListener {
                            Toast.makeText(this, "✅ Anonim plaka eklendi!", Toast.LENGTH_SHORT).show()
                            showEditPlakaDialog(girilenPlaka)
                        }.addOnFailureListener { e ->
                            Toast.makeText(this, "❌ Anonim plaka eklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Hayır", null)
                    .show()
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
        otoparkRef.setValue("bos").addOnSuccessListener {
            Toast.makeText(this, "✅ Otopark yeri boşaltıldı: $kat - $alan", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "❌ Otopark yeri boşaltılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateOtoparkDurumuDolu(kat: String, alan: String) {
        if (kat.isBlank() || alan.isBlank()) return
        val db = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        val otoparkRef = db.getReference("otopark_duzeni").child(kat).child(alan)
        otoparkRef.setValue("dolu").addOnSuccessListener {
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
                val plakaRaw = inputPlaka.text.toString().trim()
                val plaka = plakaRaw.replace("\\s".toRegex(), "").uppercase()


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
    private fun showKullanicilarDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_kullanicilar, null)
        val listView = dialogView.findViewById<ListView>(R.id.listViewKullanicilar)
        val buttonKayitli = dialogView.findViewById<Button>(R.id.buttonKayitli)
        val buttonAnonim = dialogView.findViewById<Button>(R.id.buttonAnonim)

        val userList = mutableListOf<Pair<String, String>>() // (görünen metin, UID)

        // Artık silme yok → sadece listeleme adapter'ı
        val adapter = UserListAdapter(this, userList)

        listView.adapter = adapter

        val db = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        val kullanicilarRef = db.getReference("kullanicilar")
        val anonimRef = db.getReference("anonimKullanicilar")

        buttonKayitli.setOnClickListener {
            userList.clear()
            kullanicilarRef.get().addOnSuccessListener { snapshot ->
                snapshot.children.forEach { userSnap ->
                    val email = userSnap.child("email").getValue(String::class.java)
                        ?: userSnap.child("eposta").getValue(String::class.java)
                        ?: "?"

                    val plakalar = if (userSnap.hasChild("plakalar")) {
                        userSnap.child("plakalar").children.map { it.key ?: "" }
                            .joinToString(", ")
                    } else if (userSnap.hasChild("plaka")) {
                        userSnap.child("plaka").getValue(String::class.java) ?: "Plaka yok"
                    } else {
                        "Plaka yok"
                    }

                    userList.add(Pair("📧 $email\n🚗 $plakalar", userSnap.key ?: ""))
                }
                adapter.notifyDataSetChanged()
            }
        }

        buttonAnonim.setOnClickListener {
            userList.clear()
            anonimRef.get().addOnSuccessListener { anonSnapshot ->
                anonSnapshot.children.forEach { anonSnap ->
                    val plaka = anonSnap.child("plaka").getValue(String::class.java) ?: "?"
                    userList.add(Pair("🕶️ Anonim Kullanıcı\n🚗 $plaka", anonSnap.key ?: ""))
                }
                adapter.notifyDataSetChanged()
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Kullanıcılar")
            .setView(dialogView)
            .setNegativeButton("Kapat", null)
            .create()
            .show()
    }
    private fun showInfoKartDuzenleDialog() {
        val db =
            FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        val infoCardsRef = db.getReference("infoCards")
        val otoparkRef = db.getReference("otopark_duzeni")

        infoCardsRef.get().addOnSuccessListener { snapshot ->
            val builder = AlertDialog.Builder(this)
            builder.setTitle("İnfo Kartlar")

            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            val editableInputs =
                mutableListOf<Pair<DataSnapshot, Spinner>>() // artık spinner tutulacak

            // Önce doluluk oranını hesapla
            otoparkRef.get().addOnSuccessListener { otoparkSnapshot ->
                var toplamAlan = 0
                var doluAlan = 0

                otoparkSnapshot.children.forEach { katSnap ->
                    katSnap.children.forEach { alanSnap ->
                        toplamAlan++
                        val durumStr = alanSnap.getValue(String::class.java) ?: "bos"
                        if (durumStr == "dolu") doluAlan++
                    }
                }

                val dolulukYuzdesi = if (toplamAlan > 0) (doluAlan * 100) / toplamAlan else 0
                val otoparkDurumu = if (dolulukYuzdesi >= 95) "DOLU" else "BOŞ"

                snapshot.children.forEachIndexed { index, cardSnap ->
                    val title = cardSnap.child("title").getValue(String::class.java) ?: ""
                    var description =
                        cardSnap.child("description").getValue(String::class.java) ?: ""

                    if (index == 0) { // Otopark Durumu
                        description = otoparkDurumu
                        cardSnap.ref.child("description").setValue(description)
                    } else if (index == 1) { // Doluluk
                        description = "%$dolulukYuzdesi"
                        cardSnap.ref.child("description").setValue(description)
                    } else if (index == 2 || index == 3) { // Kamera Durumu, Giriş/Çıkış → admin elle seçsin
                        val options = listOf("Aktif", "Pasif")
                        val spinner = Spinner(this)
                        val spinnerAdapter =
                            ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
                        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinner.adapter = spinnerAdapter

                        val selectedIndex = options.indexOf(description)
                        if (selectedIndex >= 0) spinner.setSelection(selectedIndex)

                        layout.addView(TextView(this).apply { text = "$title Durumu:" })
                        layout.addView(spinner)

                        editableInputs.add(Pair(cardSnap, spinner))
                    }
                }

                builder.setView(layout)
                builder.setPositiveButton("Kaydet") { _, _ ->
                    editableInputs.forEach { (cardSnap, spinner) ->
                        val selectedValue = spinner.selectedItem.toString()
                        cardSnap.ref.child("description").setValue(selectedValue)
                    }
                    Toast.makeText(this, "✅ İnfo kartlar güncellendi!", Toast.LENGTH_SHORT).show()
                }
                builder.setNegativeButton("İptal", null)
                builder.show()
            }.addOnFailureListener {
                Toast.makeText(
                    this,
                    "❌ Otopark verisi yüklenemedi: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "❌ İnfo kartlar yüklenemedi: ${it.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun showBildirimlerDialog() {
        val database = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        val bildirimRef = database.getReference("adminMessages/bildirimler")

        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scrollView = ScrollView(this)
        val innerLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scrollView.addView(innerLayout)

        bildirimRef.get().addOnSuccessListener { snapshot ->
            innerLayout.removeAllViews()

            snapshot.children.forEach { child ->
                val key = child.key
                val bildirimObj = child.getValue(Bildirim::class.java)
                val mesaj = bildirimObj?.mesaj ?: ""
                val tip = bildirimObj?.tip ?: ""
                val zaman = bildirimObj?.zaman ?: ""

                val itemView = LayoutInflater.from(this).inflate(R.layout.item_bildirim, null)
                val textView = itemView.findViewById<TextView>(R.id.textViewMesaj)
                val editButton = itemView.findViewById<ImageButton>(R.id.buttonEdit)
                val deleteButton = itemView.findViewById<ImageButton>(R.id.buttonDelete)

                textView.text = "[$zaman] ($tip): $mesaj"

                editButton.setOnClickListener {
                    if (key != null && bildirimObj != null) {
                        showEditBildirimDialog(key, bildirimObj)
                    }
                }

                deleteButton.setOnClickListener {
                    if (key != null) {
                        bildirimRef.child(key).removeValue()
                            .addOnSuccessListener {
                                Toast.makeText(this@AdminHomeActivity, "Bildirim silindi", Toast.LENGTH_SHORT).show()
                                showBildirimlerDialog()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this@AdminHomeActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }

                innerLayout.addView(itemView)
            }

            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_bildirim_ekle, null)
            val inputEditText = dialogView.findViewById<EditText>(R.id.inputEditText)
            inputEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)

            val addButton = dialogView.findViewById<Button>(R.id.buttonEkle)

            addButton.setOnClickListener {
                val yeniMesaj = inputEditText.text.toString()
                if (yeniMesaj.isNotEmpty()) {
                    val yeniId = bildirimRef.push().key
                    if (yeniId != null) {
                        val yeniBildirim = Bildirim(
                            mesaj = yeniMesaj,
                            tip = "manual",
                            zaman = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        )
                        bildirimRef.child(yeniId).setValue(yeniBildirim)
                            .addOnSuccessListener {
                                Toast.makeText(this@AdminHomeActivity, "Bildirim eklendi", Toast.LENGTH_SHORT).show()
                                inputEditText.text.clear()
                                showBildirimlerDialog()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this@AdminHomeActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }

            layout.addView(scrollView)
            layout.addView(dialogView)

            AlertDialog.Builder(this)
                .setTitle("Uygulama Bildirimleri")
                .setView(layout)
                .setNegativeButton("Kapat", null)
                .show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditBildirimDialog(key: String, mevcutBildirim: Bildirim) {
        val inputEditText = EditText(this).apply { setText(mevcutBildirim.mesaj) }

        AlertDialog.Builder(this)
            .setTitle("Bildirim Düzenle")
            .setView(inputEditText)
            .setPositiveButton("Kaydet") { _, _ ->
                val guncelMesaj = inputEditText.text.toString()
                if (guncelMesaj.isNotEmpty()) {
                    val database = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
                    val bildirimRef = database.getReference("adminMessages/bildirimler")

                    val guncelBildirim = mevcutBildirim.copy(mesaj = guncelMesaj)

                    bildirimRef.child(key).setValue(guncelBildirim)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Bildirim güncellendi", Toast.LENGTH_SHORT).show()
                            showBildirimlerDialog()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    private fun showChatbotMessagesDialog() {
        val database = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        val messagesRef = database.getReference("adminMessages/messages")

        messagesRef.get().addOnSuccessListener { snapshot ->
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Chatbot Mesajlar")

            val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

            snapshot.children.forEach { child ->
                val messageObj = child.getValue(AdminChatMessage::class.java)
                val plaka = messageObj?.plaka ?: "Bilinmiyor"
                val mesaj = messageObj?.message ?: "Mesaj yok"
                val cevap = messageObj?.cevap ?: ""

                val itemLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                itemLayout.setPadding(0, 10, 0, 10)

                val plakaView = TextView(this).apply {
                    text = "🚗 $plaka"
                    textSize = 16f
                    setPadding(0, 0, 0, 5)
                }

                val mesajView = TextView(this).apply {
                    text = "💬 Mesaj: $mesaj"
                }

                val cevapView = TextView(this).apply {
                    text = if (cevap.isNotEmpty()) "✅ Cevap: $cevap" else "❌ Henüz cevaplanmadı"
                }

                val cevaplaButton = Button(this).apply {
                    text = "Cevapla"
                    setOnClickListener {
                        showCevaplaDialog(child.key ?: "", messageObj)
                    }
                }

                itemLayout.addView(plakaView)
                itemLayout.addView(mesajView)
                itemLayout.addView(cevapView)
                itemLayout.addView(cevaplaButton)

                layout.addView(itemLayout)
            }

            builder.setView(ScrollView(this).apply { addView(layout) })
            builder.setNegativeButton("Kapat", null)
            builder.show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "❌ Mesajlar yüklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun showCevaplaDialog(messageId: String, mevcutMesaj: AdminChatMessage?) {
        val inputEditText = EditText(this).apply {
            hint = "Cevabınızı girin"
            setText(mevcutMesaj?.cevap ?: "")
        }

        AlertDialog.Builder(this)
            .setTitle("Cevapla")
            .setView(inputEditText)
            .setPositiveButton("Kaydet") { _, _ ->
                val cevapText = inputEditText.text.toString()
                if (cevapText.isNotEmpty()) {
                    val database = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
                    val messagesRef = database.getReference("adminMessages/messages").child(messageId)
                    messagesRef.child("cevap").setValue(cevapText)
                        .addOnSuccessListener {
                            Toast.makeText(this, "✅ Cevap kaydedildi", Toast.LENGTH_SHORT).show()
                            showChatbotMessagesDialog()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "❌ Cevap kaydedilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }





}






