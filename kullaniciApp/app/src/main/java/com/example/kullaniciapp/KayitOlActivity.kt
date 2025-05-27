package com.example.kullaniciapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class KayitOlActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var plakaEditText: EditText
    private lateinit var buttonKayitOl: Button
    private lateinit var buttonGeriDon: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kayit_ol)

        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        plakaEditText = findViewById(R.id.editTextPlaka)
        buttonKayitOl = findViewById(R.id.buttonRegister)
        buttonGeriDon = findViewById(R.id.buttonBack)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")

        buttonGeriDon.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        buttonKayitOl.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            var plaka = plakaEditText.text.toString().trim()
                .replace("\\s".toRegex(), "")
                .uppercase()

            if (email.isEmpty() || password.isEmpty() || plaka.isEmpty()) {
                Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
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

            val usersRef = database.getReference("kullanicilar")

            // Plaka var mı kontrolü
            usersRef.get().addOnSuccessListener { snapshot ->
                var plakaVar = false

                snapshot.children.forEach { userSnapshot ->
                    val existingPlaka = userSnapshot.child("plaka").getValue(String::class.java)
                    if (existingPlaka == plaka) {
                        plakaVar = true
                    }
                }

                if (plakaVar) {
                    Toast.makeText(this, "❌ Bu plaka zaten kayıtlı!", Toast.LENGTH_LONG).show()
                } else {
                    // Firebase Authentication kaydı
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { result ->
                            val uid = result.user?.uid
                            if (uid != null) {
                                val userMap = mapOf(
                                    "eposta" to email,
                                    "plaka" to plaka,
                                    "giris_saati" to "",
                                    "toplam_ucret" to "0₺"
                                )
                                usersRef.child(uid).setValue(userMap)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "✅ Kayıt başarılı!", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, MainActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "❌ Veritabanına yazılamadı: ${it.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "❌ Kayıt başarısız: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }.addOnFailureListener {
                Toast.makeText(this, "❌ Veritabanı hatası: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
