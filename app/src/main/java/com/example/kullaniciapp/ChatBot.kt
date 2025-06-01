package com.example.kullaniciapp

import com.google.firebase.database.FirebaseDatabase

object ChatBot {

    private val databaseUrl = "https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/"
    private val database = FirebaseDatabase.getInstance(databaseUrl)

    fun getResponse(userMessage: String, uid: String, havaDurumu: String?, callback: (String) -> Unit) {
        val userRef = database.getReference("kullanicilar").child(uid)

        userRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                callback("Kullanıcı bilgilerine ulaşılamadı.")
                return@addOnSuccessListener
            }

            val plakalarNode = snapshot.child("plakalar")
            if (!plakalarNode.exists()) {
                callback("Henüz kayıtlı bir plaka bilginiz bulunmamaktadır.")
                return@addOnSuccessListener
            }

            val plakaBilgileriList = mutableListOf<String>()

            for (plakaSnap in plakalarNode.children) {
                val plakaKey = plakaSnap.key ?: continue
                val girisSaati = plakaSnap.child("giris_saati").getValue(String::class.java) ?: "-"
                val girisTarihi = plakaSnap.child("giris_tarihi").getValue(String::class.java) ?: "-"
                val cikisSaati = plakaSnap.child("cikis_saati").getValue(String::class.java) ?: "-"
                val cikisTarihi = plakaSnap.child("cikis_tarihi").getValue(String::class.java) ?: "-"
                val alan = plakaSnap.child("alan").getValue(String::class.java) ?: "-"
                val kat = plakaSnap.child("kat").getValue(String::class.java) ?: "-"
                val toplamUcret = plakaSnap.child("toplam_ucret").getValue(String::class.java) ?: "-"

                val bilgi = """
                    🚗 Plaka: $plakaKey
                    📅 Giriş: $girisTarihi $girisSaati
                    📅 Çıkış: $cikisTarihi $cikisSaati
                    📍 Alan: $alan, Kat: $kat
                    💰 Ücret: $toplamUcret
                """.trimIndent()

                plakaBilgileriList.add(bilgi)
            }

            val response = when (userMessage.lowercase()) {
                "plakam ne?", "plakam ne" -> plakaBilgileriList.joinToString("\n\n")
                "giriş saatim?", "giris saatim?", "giriş saatim" -> plakaBilgileriList.joinToString("\n\n") {
                    it.lines().find { line -> line.contains("Giriş:") } ?: "-"
                }
                "park yerim?", "park yerim" -> plakaBilgileriList.joinToString("\n\n") {
                    it.lines().find { line -> line.contains("Alan:") } ?: "-"
                }
                "ücret ne kadar?", "ucret ne kadar?", "ücret ne kadar" -> plakaBilgileriList.joinToString("\n\n") {
                    it.lines().find { line -> line.contains("Ücret:") } ?: "-"
                }
                "bugünkü hava durumu", "hava durumu", "hava nasıl" -> havaDurumu ?: "Hava durumu bilgisi alınamadı."
                else -> "Üzgünüm, bu konuda yardımcı olamıyorum."
            }

            callback(response)
        }.addOnFailureListener {
            callback("Bir hata oluştu, lütfen tekrar deneyin.")
        }
    }
}
