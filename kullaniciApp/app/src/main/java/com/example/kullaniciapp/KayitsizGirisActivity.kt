package com.example.kullaniciapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class KayitsizGirisActivity : AppCompatActivity() {

    private lateinit var plakaEditText: EditText
    private lateinit var buttonDevam: Button
    private lateinit var buttonGeri: Button
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kayitsiz_giris)

        plakaEditText = findViewById(R.id.editTextPlaka)
        buttonDevam = findViewById(R.id.buttonDevam)
        buttonGeri = findViewById(R.id.buttonGeri)

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
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(applicationContext, "Veri okunamadı", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }
}
