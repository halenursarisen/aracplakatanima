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
    private lateinit var logoutButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_hesabim, container, false)

        // View bağlantıları
        textName = view.findViewById(R.id.textName)
        textPlate = view.findViewById(R.id.textPlate)
        logoutButton = view.findViewById(R.id.buttonLogout)
        progressBar = view.findViewById(R.id.progressBar)

        // Firebase'den UID'yi al
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("GERCEK_UID", "Aktif UID: $uid")

        if (uid != null) {
            progressBar.visibility = View.VISIBLE

            // Realtime DB bağlantısı (custom URL ile)
            val database = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
            val userRef = database.getReference("kullanicilar").child(uid)

            userRef.get()
                .addOnSuccessListener { snapshot ->
                    Log.d("FIREBASE_DATA", "Snapshot exists: ${snapshot.exists()}, value: ${snapshot.value}")

                    if (snapshot.exists()) {
                        val adSoyad = snapshot.child("adSoyad").getValue(String::class.java) ?: "Ad boş"
                        val plaka = snapshot.child("plaka").getValue(String::class.java) ?: "Plaka boş"

                        textName.text = adSoyad
                        textPlate.text = plaka

                        Toast.makeText(requireContext(), "✅ Veri başarıyla alındı", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "❗ Veri bulunamadı", Toast.LENGTH_SHORT).show()
                    }

                    progressBar.visibility = View.GONE
                }
                .addOnFailureListener { e ->
                    Log.e("FIREBASE_HATA", "Veri alınamadı: ${e.message}")
                    Toast.makeText(requireContext(), "Veri alınırken hata oluştu ❌", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
        } else {
            Toast.makeText(requireContext(), "Giriş yapılmamış", Toast.LENGTH_SHORT).show()
        }

        // Çıkış butonu
        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), KayitsizGirisActivity::class.java))
            activity?.finish()
        }

        return view
    }
}
