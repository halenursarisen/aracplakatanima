package com.example.kullaniciapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class KayitOlActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var plakaEditText: EditText
    private lateinit var buttonKayitOl: Button
    private lateinit var buttonGeriDon: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kayit_ol)

        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        plakaEditText = findViewById(R.id.editTextPlaka)
        buttonKayitOl = findViewById(R.id.buttonRegister)
        buttonGeriDon = findViewById(R.id.buttonBack)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        buttonGeriDon.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        buttonKayitOl.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val plaka = plakaEditText.text.toString().trim().uppercase()

            if (email.isEmpty() || password.isEmpty() || plaka.isEmpty()) {
                Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. Aynı plaka var mı kontrol et
            firestore.collection("users")
                .whereEqualTo("plaka", plaka)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        Toast.makeText(this, "Bu plaka zaten kayıtlı!", Toast.LENGTH_LONG).show()
                    } else {
                        // 2. Firebase Authentication kayıt
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener { result ->
                                val uid = result.user?.uid
                                if (uid != null) {
                                    val userMap = hashMapOf(
                                        "email" to email,
                                        "plaka" to plaka,
                                        "giris_saati" to "",
                                        "toplam_ucret" to "0₺"
                                    )
                                    firestore.collection("users").document(uid).set(userMap)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Kayıt başarılı!", Toast.LENGTH_SHORT).show()
                                            startActivity(Intent(this, MainActivity::class.java))
                                            finish()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this, "Firestore'a yazılamadı: ${it.message}", Toast.LENGTH_LONG).show()
                                        }
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Kayıt başarısız: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Veritabanı hatası: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
