package com.example.kullaniciapp

import android.content.Intent
import android.os.Bundle
<<<<<<< HEAD
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
=======
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
>>>>>>> 44d3236e304bccba2f7b47a1a2eb8beafebd2045
import com.google.firebase.database.*

class KayitsizGirisActivity : AppCompatActivity() {

    private lateinit var plakaEditText: EditText
    private lateinit var buttonDevam: Button
    private lateinit var buttonGeri: Button
<<<<<<< HEAD
    private lateinit var databaseRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
=======
    private lateinit var database: DatabaseReference
>>>>>>> 44d3236e304bccba2f7b47a1a2eb8beafebd2045

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kayitsiz_giris)

        plakaEditText = findViewById(R.id.editTextPlaka)
        buttonDevam = findViewById(R.id.buttonDevam)
        buttonGeri = findViewById(R.id.buttonGeri)

<<<<<<< HEAD
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
=======
        database = FirebaseDatabase.getInstance().reference

        // 🔙 Geri Dön Butonu
        buttonGeri.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        // 🚗 Devam Et Butonu
        buttonDevam.setOnClickListener {
            val girilenPlaka = plakaEditText.text.toString().uppercase().trim()

            if (girilenPlaka.isEmpty()) {
                Toast.makeText(this, "Lütfen plakanızı girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // plakaSahipligi → plaka → UID eşleşmesi
            database.child("plakaSahipligi").child(girilenPlaka)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val uid = snapshot.getValue(String::class.java)
                            Toast.makeText(applicationContext, "Plaka eşleşti!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@KayitsizGirisActivity, MainBottomActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(applicationContext, "Bu plakaya ait kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
>>>>>>> 44d3236e304bccba2f7b47a1a2eb8beafebd2045
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
<<<<<<< HEAD
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

=======
                        Toast.makeText(applicationContext, "Veri okunamadı", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }
}
>>>>>>> 44d3236e304bccba2f7b47a1a2eb8beafebd2045
