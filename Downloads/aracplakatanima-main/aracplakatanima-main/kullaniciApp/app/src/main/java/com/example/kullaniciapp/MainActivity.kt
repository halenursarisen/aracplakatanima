package com.example.kullaniciapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val registerButton = findViewById<Button>(R.id.buttonRegister)
        val guestLoginButton = findViewById<Button>(R.id.btnGuestLogin)
        val resetPasswordButton = findViewById<Button>(R.id.btnResetPassword) // 💡 yeni eklenen buton

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Lütfen e-posta ve şifre alanlarını doldurun", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        registerButton.setOnClickListener {
            startActivity(Intent(this, KayitOlActivity::class.java))
        }

        // 👇 Anonim giriş işlemi
        guestLoginButton.setOnClickListener {
            auth.signInAnonymously()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Kayıt olmadan giriş yapılacak", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, KayitsizGirisActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Anonim giriş başarısız: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // 👇 Şifremi Unuttum sayfasına yönlendirme
        resetPasswordButton.setOnClickListener {
            startActivity(Intent(this, SifremiUnuttumActivity::class.java))
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Giriş başarılı!", Toast.LENGTH_SHORT).show()

                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null) {
                        val db = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
                        val ref = db.getReference("kullanicilar").child(user.uid)

                        ref.get().addOnSuccessListener { snapshot ->
                            val abonelikBitisStr = snapshot.child("abonelikBitis").getValue(String::class.java)
                            val abonelikDurumu = snapshot.child("abonelikDurumu").getValue(String::class.java) ?: "normal"

                            if (abonelikDurumu == "abonelikli" && !abonelikBitisStr.isNullOrEmpty()) {
                                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                try {
                                    val abonelikBitisDate = sdf.parse(abonelikBitisStr)
                                    val today = Date() // Bugünü temsil eden Date objesi

                                    if (today.after(abonelikBitisDate)) {
                                        // Abonelik süresi dolmuş, arşivle ve sıfırla
                                        val eskiBaslangic = snapshot.child("abonelikBaslangic").getValue(String::class.java) ?: ""
                                        val eskiBitis = abonelikBitisStr
                                        val eskiUcret = snapshot.child("abonelikUcreti").getValue(Int::class.java) ?: 0
                                        val email = snapshot.child("eposta").getValue(String::class.java) ?: "-"
                                        val plakalarSnapshot = snapshot.child("plakalar")
                                        val plakalarList = plakalarSnapshot.children.mapNotNull { it.key }

                                        val arsivKaydi = mapOf(
                                            "baslangic" to eskiBaslangic,
                                            "bitis" to eskiBitis,
                                            "ucret" to eskiUcret,
                                            "email" to email,
                                            "plakalar" to plakalarList, // Plakaları da arşivle
                                            "bitisTarihi" to SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date()) // Arşivleme tarihi
                                        )

                                        // Kullanıcının kendi altında geçmişe kaydet
                                        ref.child("gecmisAbonelikler").push().setValue(arsivKaydi)

                                        // Admin genel arşivine kaydet
                                        val adminRef = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
                                            .getReference("gecmisAbonelikler")
                                        adminRef.push().setValue(arsivKaydi)

                                        // Aktif abonelik bilgilerini temizle
                                        ref.child("abonelikDurumu").setValue("normal")
                                        ref.child("abonelikBaslangic").removeValue()
                                        ref.child("abonelikBitis").removeValue()
                                        ref.child("abonelikUcreti").removeValue()

                                        Toast.makeText(this, "❗ Abonelik süreniz doldu ve iptal edildi.", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    // Tarih parse hatası durumunda
                                    Toast.makeText(this, "Abonelik tarihi okunurken hata oluştu.", Toast.LENGTH_SHORT).show()
                                }
                            }

                            // 🔽 Abonelik kontrolü bittikten sonra yönlendirme
                            if (user.email == "admin@otoparkapp.com") {
                                startActivity(Intent(this, AdminHomeActivity::class.java))
                            } else {
                                startActivity(Intent(this, MainBottomActivity::class.java))
                            }
                            finish()
                        }.addOnFailureListener { e ->
                            Toast.makeText(this, "Kullanıcı verisi okunamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                            // Yönlendirme hatası durumunda da yapılmalı
                            if (user.email == "admin@otoparkapp.com") {
                                startActivity(Intent(this, AdminHomeActivity::class.java))
                            } else {
                                startActivity(Intent(this, MainBottomActivity::class.java))
                            }
                            finish()
                        }
                    }
                } else {
                    Toast.makeText(this, "Giriş başarısız: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Kayıt başarılı!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Kayıt başarısız: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
