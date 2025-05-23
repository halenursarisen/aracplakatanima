package com.example.kullaniciapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class KayitsizGirisActivity : AppCompatActivity() {

    private lateinit var plakaEditText: EditText
    private lateinit var buttonDevam: Button
    private lateinit var buttonGeri: Button
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kayitsiz_giris)

        // XML’deki nesneleri bağla
        plakaEditText = findViewById(R.id.editTextPlaka)
        buttonDevam = findViewById(R.id.buttonDevam)
        buttonGeri = findViewById(R.id.buttonGeri)

        // Firestore bağlantısını al
        firestore = FirebaseFirestore.getInstance()

        // ✅ DEVAM ET butonuna tıklanınca
        buttonDevam.setOnClickListener {
            val plaka = plakaEditText.text.toString().trim().uppercase()

            if (plaka.isEmpty()) {
                Toast.makeText(this, "Lütfen plakanızı giriniz", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔍 Firestore'da bu plakaya sahip kullanıcı var mı?
            firestore.collection("users")
                .whereEqualTo("plaka", plaka)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.isEmpty) {
                        // ❌ Plaka kayıtlı → misafir girişi engellenir
                        Toast.makeText(this, "Bu plaka kayıtlı. Misafir girişi yapılamaz!", Toast.LENGTH_LONG).show()
                    } else {
                        // ✅ Plaka boşta → MisafirHomeActivity'ye yönlendir
                        val intent = Intent(this, MisafirHomeActivity::class.java)
                        intent.putExtra("plaka", plaka)
                        startActivity(intent)
                        finish()
                    }
                }
                .addOnFailureListener { error ->
                    // ❗ Veri çekme hatası
                    Toast.makeText(this, "Veritabanı hatası: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // 🔙 GERİ DÖN butonu → MainActivity
        buttonGeri.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
