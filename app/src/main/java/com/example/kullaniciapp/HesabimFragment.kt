package com.example.kullaniciapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HesabimFragment : Fragment() {

    private lateinit var textName: TextView
    private lateinit var textPlate: TextView
    private lateinit var textEmail: TextView
    private lateinit var textPhone: TextView
    private lateinit var logoutButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_hesabim, container, false)

        textName = view.findViewById(R.id.textName)
        textPlate = view.findViewById(R.id.textPlate)
        textEmail = view.findViewById(R.id.textEmail)
        textPhone = view.findViewById(R.id.textPhone)
        logoutButton = view.findViewById(R.id.buttonLogout)
        progressBar = view.findViewById(R.id.progressBar)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("FIREBASE_UID", "Giriş yapan UID: $uid")

        val db = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        val ref = db.getReference("kullanicilar").child(uid ?: "")

        if (uid != null) {
            progressBar.visibility = View.VISIBLE

            ref.get().addOnSuccessListener { snapshot ->
                Log.d("FIREBASE_SNAPSHOT", "Snapshot value: ${snapshot.value}")

                // Alan adlarını tek tek logla
                for (child in snapshot.children) {
                    Log.d("FIREBASE_FIELD", "Key: ${child.key}, Value: ${child.value}")
                }

                if (snapshot.exists()) {
                    val adSoyad = snapshot.child("adSoyad").getValue(String::class.java) ?: "-"
                    val plaka = snapshot.child("plaka").getValue(String::class.java) ?: "-"
                    val email = snapshot.child("eposta").getValue(String::class.java) ?: "-"
                    val phone = snapshot.child("telefon").getValue(String::class.java) ?: "-"

                    textName.text = adSoyad
                    textPlate.text = plaka
                    textEmail.text = email
                    textPhone.text = phone

                    Toast.makeText(requireContext(), "✅ Bilgiler yüklendi", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "⚠️ Kullanıcı verisi bulunamadı", Toast.LENGTH_SHORT).show()
                }

                progressBar.visibility = View.GONE
            }.addOnFailureListener { e ->
                Log.e("FIREBASE_HATA", "Veri çekilemedi: ${e.message}")
                Toast.makeText(requireContext(), "❌ Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
        } else {
            Toast.makeText(requireContext(), "❗ Giriş yapılmamış", Toast.LENGTH_SHORT).show()
        }

        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), KayitsizGirisActivity::class.java))
            activity?.finish()
        }

        return view
    }
}
