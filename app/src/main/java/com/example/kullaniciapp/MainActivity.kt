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

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Lütfen e-posta ve şifre girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Lütfen e-posta ve şifre girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(email, password)
        }

        guestLoginButton.setOnClickListener {
            auth.signInAnonymously()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Misafir girişi yapıldı", Toast.LENGTH_SHORT).show()
                        bildirimGonder("Misafir girişi yapıldı", "info")
                        startActivity(Intent(this, KayitsizGirisActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Anonim giriş başarısız: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Giriş başarılı!", Toast.LENGTH_SHORT).show()
                    bildirimGonder("$email giriş yaptı", "success")
                    startActivity(Intent(this, MainBottomActivity::class.java))
                    finish()
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
                    bildirimGonder("$email kayıt oldu", "info")
                    startActivity(Intent(this, MainBottomActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Kayıt başarısız: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun bildirimGonder(mesaj: String, tip: String) {
        val database = FirebaseDatabase.getInstance().reference
        val zaman = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val bildirim = mapOf(
            "mesaj" to mesaj,
            "zaman" to zaman,
            "tip" to tip
        )

        database.child("bildirimler").push().setValue(bildirim)
    }
}
