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

            if (email == "admin@otoparkapp.com") {
                Toast.makeText(this, "❗ Bu e-posta ile kayıt olunamaz", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val turkPlakaRegex = Regex("^[0-9]{2}[A-Z]{1,3}[0-9]{2,4}$")
            val yabanciPlakaRegex = Regex("^[A-Z0-9]{5,10}$")

            if (!turkPlakaRegex.matches(plaka) && !yabanciPlakaRegex.matches(plaka)) {
                Toast.makeText(
                    this,
                    "❗ Geçerli bir Türk veya yabancı plaka girin",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val usersRef = database.getReference("kullanicilar")

            usersRef.get().addOnSuccessListener { snapshot ->
                var plakaVar = false

                snapshot.children.forEach { userSnapshot ->
                    val existingPlaka = userSnapshot.child("plaka").getValue(String::class.java)
                    if (existingPlaka == plaka) {
                        plakaVar = true
                    }

                    val plakalarSnapshot = userSnapshot.child("plakalar")
                    plakalarSnapshot.children.forEach { plakaSnap ->
                        if (plakaSnap.key == plaka) {
                            plakaVar = true
                        }
                    }
                }

                if (plakaVar) {
                    Toast.makeText(this, "❌ Bu plaka zaten kayıtlı!", Toast.LENGTH_LONG).show()
                } else {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { result ->
                            val uid = result.user?.uid
                            if (uid != null) {
                                val userMap = mapOf(
                                    "adSoyad" to "Yeni Kullanıcı",
                                    "eposta" to email,
                                    "plaka" to plaka,
                                    "plakalar" to mapOf(
                                        plaka to mapOf(
                                            "marka" to "",
                                            "model" to "",
                                            "sigorta" to "",
                                            "ruhsat" to "",
                                            "kasko" to "",
                                            "giris_tarihi" to "",
                                            "giris_saati" to "",
                                            "cikis_tarihi" to "",
                                            "cikis_saati" to "",
                                            "kat" to "",
                                            "alan" to "",
                                            "toplam_ucret" to ""
                                        )
                                    )
                                )

                                usersRef.child(uid).setValue(userMap)
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            this,
                                            "✅ Kayıt başarılı!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        startActivity(Intent(this, MainActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            this,
                                            "❌ Veritabanına yazılamadı: ${it.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                this,
                                "❌ Kayıt başarısız: ${it.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }.addOnFailureListener {
                Toast.makeText(this, "❌ Veritabanı hatası: ${it.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}