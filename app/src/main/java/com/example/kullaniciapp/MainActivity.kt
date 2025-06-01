package com.example.kullaniciapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
<<<<<<< HEAD
=======
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
>>>>>>> 44d3236e304bccba2f7b47a1a2eb8beafebd2045

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
<<<<<<< HEAD
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
=======

        // Kullanıcı girişi
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                showToast("Lütfen e-posta ve şifre girin")
            } else {
                loginUser(email, password)
            }
        }

        // Kullanıcı kaydı
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                showToast("Lütfen e-posta ve şifre girin")
            } else {
                registerUser(email, password)
            }
        }

        // Misafir girişi
        guestLoginButton.setOnClickListener {
            auth.signInAnonymously().addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    showToast("Misafir girişi yapıldı")
                    bildirimGonder("Misafir girişi yapıldı", "info")
                    startActivity(Intent(this, KayitsizGirisActivity::class.java))
                    finish()
                } else {
                    showToast("Anonim giriş başarısız: ${task.exception?.message}")
                }
            }
>>>>>>> 44d3236e304bccba2f7b47a1a2eb8beafebd2045
        }
    }

    private fun loginUser(email: String, password: String) {
<<<<<<< HEAD
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Giriş başarılı!", Toast.LENGTH_SHORT).show()

                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null) {
                        if (user.email == "admin@otoparkapp.com") {
                            // Admin paneline yönlendir
                            startActivity(Intent(this, AdminHomeActivity::class.java))
                        } else {
                            // Normal kullanıcı paneline yönlendir
                            startActivity(Intent(this, MainBottomActivity::class.java))
                        }
                        finish()
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
=======
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                showToast("Giriş başarılı!")
                bildirimGonder("$email giriş yaptı", "success")
                startActivity(Intent(this, MainBottomActivity::class.java))
                finish()
            } else {
                showToast("Giriş başarısız: ${task.exception?.message}")
            }
        }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                showToast("Kayıt başarılı!")
                bildirimGonder("$email kayıt oldu", "info")
                startActivity(Intent(this, MainBottomActivity::class.java))
                finish()
            } else {
                showToast("Kayıt başarısız: ${task.exception?.message}")
            }
        }
    }

    private fun bildirimGonder(mesaj: String, tip: String) {
        val database = FirebaseDatabase.getInstance().reference
        val zaman = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val bildirim = mapOf("mesaj" to mesaj, "zaman" to zaman, "tip" to tip)
        database.child("bildirimler").push().setValue(bildirim)
    }

    private fun showToast(mesaj: String) {
        Toast.makeText(this, mesaj, Toast.LENGTH_SHORT).show()
>>>>>>> 44d3236e304bccba2f7b47a1a2eb8beafebd2045
    }
}
