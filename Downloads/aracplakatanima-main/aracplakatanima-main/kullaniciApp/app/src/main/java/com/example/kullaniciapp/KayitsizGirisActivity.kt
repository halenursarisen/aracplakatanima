package com.example.kullaniciapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class KayitsizGirisActivity : AppCompatActivity() {

    private lateinit var plakaEditText: EditText
    private lateinit var buttonDevam: Button
    private lateinit var buttonGeri: Button
    private lateinit var databaseRef: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kayitsiz_giris)

        plakaEditText = findViewById(R.id.editTextPlaka)
        buttonDevam = findViewById(R.id.buttonDevam)
        buttonGeri = findViewById(R.id.buttonGeri)

        auth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("kullanicilar")

        buttonDevam.setOnClickListener {
            val plaka = plakaEditText.text.toString()
                .trim()
                .replace("\\s+".toRegex(), "") // tüm boşlukları sil
                .uppercase() // büyük harfe çevir


            if (plaka.isEmpty()) {
                Toast.makeText(this, "Lütfen plakanızı giriniz", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Türk plakası regex: 2 rakam + 1-3 harf + 2-4 rakam
            val turkPlakaRegex = Regex("^[0-9]{2}[A-Z]{1,3}[0-9]{2,4}$")
            // Yabancı plaka basit kontrol: en az 5 karakter, harf + rakam karışık
            val yabanciPlakaRegex = Regex("^[A-Z0-9]{5,10}$")

            if (turkPlakaRegex.matches(plaka)) {
                // Türk plakası → geçerli
            } else if (yabanciPlakaRegex.matches(plaka)) {
                // Yabancı plakası → geçerli
            } else {
                Toast.makeText(this, "❗ Geçerli bir Türk veya yabancı plaka girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
            val kullanicilarRef = db.getReference("kullanicilar")
            val anonimRef = db.getReference("anonimKullanicilar")

            buttonDevam.setOnClickListener {
                val plaka = plakaEditText.text.toString()
                    .trim()
                    .replace("\\s+".toRegex(), "")
                    .uppercase()

                if (plaka.isEmpty()) {
                    Toast.makeText(this, "Lütfen plakanızı giriniz", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val turkPlakaRegex = Regex("^[0-9]{2}[A-Z]{1,3}[0-9]{2,4}$")
                val yabanciPlakaRegex = Regex("^[A-Z0-9]{5,10}$")

                if (!turkPlakaRegex.matches(plaka) && !yabanciPlakaRegex.matches(plaka)) {
                    Toast.makeText(this, "❗ Geçerli bir Türk veya yabancı plaka girin", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // 🔍 Önce kullanicilar içinde tüm plakaları tarayalım
                kullanicilarRef.get().addOnSuccessListener { snapshot ->
                    var plakaBulundu = false

                    for (userSnap in snapshot.children) {
                        val plakalarSnap = userSnap.child("plakalar")
                        if (plakalarSnap.hasChild(plaka)) {
                            plakaBulundu = true
                            break
                        }
                    }

                    if (plakaBulundu) {
                        Toast.makeText(this, "Bu plaka kayıtlı. Misafir girişi yapılamaz!", Toast.LENGTH_LONG).show()
                    } else {
                        // 🔍 Sonra anonim kullanıcılar içinde kontrol et
                        anonimRef.orderByChild("plaka").equalTo(plaka)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(anonSnapshot: DataSnapshot) {
                                    if (anonSnapshot.exists()) {
                                        // ✅ Zaten anonim → Misafir ekranına yönlendir
                                        val anonSnap = anonSnapshot.children.first()
                                        val anonUid = anonSnap.key

                                        val plaka = anonSnap.child("plaka").getValue(String::class.java) ?: ""
                                        val girisTarihi = anonSnap.child("giris_tarihi").getValue(String::class.java) ?: ""
                                        val girisSaati = anonSnap.child("giris_saati").getValue(String::class.java) ?: ""
                                        val cikisTarihi = anonSnap.child("cikis_tarihi").getValue(String::class.java) ?: ""
                                        val cikisSaati = anonSnap.child("cikis_saati").getValue(String::class.java) ?: ""
                                        val kat = anonSnap.child("kat").getValue(String::class.java) ?: ""
                                        val alan = anonSnap.child("alan").getValue(String::class.java) ?: ""

                                        val intent = Intent(this@KayitsizGirisActivity, MisafirHomeActivity::class.java)
                                        intent.putExtra("uid", anonUid)
                                        intent.putExtra("plaka", plaka)
                                        intent.putExtra("giris_tarihi", girisTarihi)
                                        intent.putExtra("giris_saati", girisSaati)
                                        intent.putExtra("cikis_tarihi", cikisTarihi)
                                        intent.putExtra("cikis_saati", cikisSaati)
                                        intent.putExtra("kat", kat)
                                        intent.putExtra("alan", alan)
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        // ➕ Yeni anonim kullanıcı oluştur
                                        auth.signInAnonymously()
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    val uid = auth.currentUser?.uid
                                                    val newAnonRef = anonimRef.child(uid!!)

                                                    val data = mapOf(
                                                        "plaka" to plaka,
                                                        "giris_tarihi" to "",
                                                        "giris_saati" to "",
                                                        "cikis_tarihi" to "",
                                                        "cikis_saati" to "",
                                                        "kat" to "",
                                                        "alan" to "",
                                                        "odemeDurumu" to ""
                                                    )


                                                    newAnonRef.setValue(data).addOnSuccessListener {
                                                        Toast.makeText(this@KayitsizGirisActivity, "Anonim kullanıcı oluşturuldu", Toast.LENGTH_SHORT).show()
                                                        val intent = Intent(this@KayitsizGirisActivity, MisafirHomeActivity::class.java)
                                                        intent.putExtra("uid", uid)
                                                        startActivity(intent)
                                                        finish()
                                                    }
                                                } else {
                                                    Toast.makeText(this@KayitsizGirisActivity, "Anonim giriş başarısız: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(this@KayitsizGirisActivity, "Veritabanı hatası: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Veritabanı hatası: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }

        }

        buttonGeri.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}

