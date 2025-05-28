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
            val plaka = plakaEditText.text.toString().trim().uppercase()

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


            // 🔍 Bu plaka kayıtlı mı kontrol et
            databaseRef.orderByChild("plaka").equalTo(plaka)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            // ❌ Plaka zaten kayıtlı → anonime izin yok
                            Toast.makeText(this@KayitsizGirisActivity, "Bu plaka kayıtlı. Misafir girişi yapılamaz!", Toast.LENGTH_LONG).show()
                        } else {
                            // ✅ Plaka boşta → anonim kullanıcı oluştur ve kaydet
                            auth.signInAnonymously()
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val uid = auth.currentUser?.uid
                                        val anonRef = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
                                            .getReference("anonimKullanicilar").child(uid!!)

                                        val data = mapOf(
                                            "plaka" to plaka,
                                            "girisSaati" to null,
                                            "parkAlani" to null,
                                            "odemeDurumu" to "bekliyor"
                                        )

                                        anonRef.setValue(data).addOnSuccessListener {
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

        buttonGeri.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}

