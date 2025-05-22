package com.example.kullaniciapp

import com.google.firebase.firestore.FirebaseFirestore

object ChatBot {

    fun getResponse(userMessage: String, uid: String, callback: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)

        userRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val plaka = document.getString("plaka") ?: "-"
                val girisSaati = document.getString("giris_saati") ?: "-"
                val parkAlani = document.getString("park_alani") ?: "-"
                val ucret = document.getString("toplam_ucret") ?: "-"
                val gecmis = document.getString("gecmis_odeme") ?: "-"

                val response = when (userMessage.lowercase()) {
                    "plakam ne?", "plakam ne" -> "Plakan: $plaka"
                    "giriş saatim?", "giris saatim?", "giriş saatim" -> "Giriş saatin: $girisSaati"
                    "park yerim?", "park yerim" -> "Park yerin: $parkAlani"
                    "ücret ne kadar?", "ucret ne kadar?", "ücret ne kadar" -> "Toplam ücretin: $ucret"
                    "geçmiş ödeme?", "gecmis odeme?", "geçmiş ödeme" -> "Geçmiş ödeme: $gecmis"
                    else -> "Üzgünüm, bu konuda yardımcı olamıyorum."
                }

                callback(response)
            } else {
                callback("Kullanıcı bilgilerine ulaşılamadı.")
            }
        }.addOnFailureListener {
            callback("Bir hata oluştu, lütfen tekrar deneyin.")
        }
    }
}
