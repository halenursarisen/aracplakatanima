package com.example.kullaniciapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import android.os.Build

class HesabimFragment : Fragment() {

    private lateinit var rootLayout: LinearLayout
    private lateinit var textName: TextView
    private lateinit var textEmail: TextView
    private lateinit var textPhone: TextView
    private lateinit var editNameButton: ImageButton
    private lateinit var editPhoneButton: ImageButton
    private lateinit var addPlateButton: ImageButton
    private lateinit var logoutButton: Button
    private lateinit var editPasswordButton: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var plakaList: MutableList<String>
    private lateinit var adapter: PlakaAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_hesabim, container, false)

        rootLayout = view.findViewById(R.id.rootLayout)
        textName = view.findViewById(R.id.textName)
        textEmail = view.findViewById(R.id.textEmail)
        textPhone = view.findViewById(R.id.textPhone)
        editNameButton = view.findViewById(R.id.buttonEditName)
        editPhoneButton = view.findViewById(R.id.buttonEditPhone)
        addPlateButton = view.findViewById(R.id.buttonAddPlate)
        logoutButton = view.findViewById(R.id.buttonLogout)
        editPasswordButton = view.findViewById(R.id.buttonEditPassword)
        recyclerView = view.findViewById(R.id.recyclerViewPlates)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid

        if (uid.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "❗ Kullanıcı oturumu bulunamadı", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), MainActivity::class.java))
            activity?.finish()
            return view
        }

        val db = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        val ref = db.getReference("kullanicilar").child(uid)

        progressBar = ProgressBar(requireContext())
        progressBar.visibility = View.VISIBLE

        plakaList = mutableListOf()
        adapter = PlakaAdapter(plakaList,
            { clickedPlaka ->
                val detay = ref.child("plakalar").child(clickedPlaka)
                detay.get().addOnSuccessListener { snapshot ->
                    val marka = snapshot.child("marka").getValue(String::class.java) ?: "-"
                    val model = snapshot.child("model").getValue(String::class.java) ?: "-"
                    val sigorta = snapshot.child("sigorta").getValue(String::class.java) ?: "-"
                    val ruhsat = snapshot.child("ruhsat").getValue(String::class.java) ?: "-"
                    val kasko = snapshot.child("kasko").getValue(String::class.java) ?: "-"

                    AlertDialog.Builder(requireContext())
                        .setTitle("Plaka: $clickedPlaka")
                        .setMessage("Marka: $marka\nModel: $model\nSigorta: $sigorta\nKasko: $kasko\nRuhsat: $ruhsat")
                        .setPositiveButton("Tamam", null)
                        .show()
                }
            },
            { plakaToDelete, position ->
                ref.child("plakalar").child(plakaToDelete).removeValue()
                    .addOnSuccessListener {
                        plakaList.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        Toast.makeText(requireContext(), "✅ Plaka silindi!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "❌ Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            })
        recyclerView.adapter = adapter

        loadUserData(ref)

        editNameButton.setOnClickListener {
            showEditDialog("İsim - Soyisim", textName.text.toString()) { newName ->
                ref.child("adSoyad").setValue(newName)
                textName.text = newName
            }
        }

        editPhoneButton.setOnClickListener {
            showEditDialog("Telefon", textPhone.text.toString()) { newPhone ->
                ref.child("telefon").setValue(newPhone)
                textPhone.text = newPhone
            }
        }

        addPlateButton.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_plate, null)
            val plakaInput = dialogView.findViewById<EditText>(R.id.inputPlaka)
            val markaInput = dialogView.findViewById<EditText>(R.id.inputMarka)
            val modelInput = dialogView.findViewById<EditText>(R.id.inputModel)
            val sigortaInput = dialogView.findViewById<EditText>(R.id.inputSigorta)
            val ruhsatInput = dialogView.findViewById<EditText>(R.id.inputRuhsat)
            val kaskoInput = dialogView.findViewById<EditText>(R.id.inputKasko)

            AlertDialog.Builder(requireContext())
                .setTitle("Yeni Plaka Ekle")
                .setView(dialogView)
                .setPositiveButton("Ekle") { _, _ ->
                    var plaka = plakaInput.text.toString()
                        .replace("\\s".toRegex(), "")
                        .uppercase()

                    val turkPlakaRegex = Regex("^[0-9]{2}[A-Z]{1,3}[0-9]{2,4}$")
                    val yabanciPlakaRegex = Regex("^[A-Z0-9]{5,10}$")

                    if (!turkPlakaRegex.matches(plaka) && !yabanciPlakaRegex.matches(plaka)) {
                        Toast.makeText(requireContext(), "❗ Geçerli bir Türk veya yabancı plaka girin", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val marka = markaInput.text.toString()
                    val model = modelInput.text.toString()
                    val sigorta = sigortaInput.text.toString()
                    val ruhsat = ruhsatInput.text.toString()
                    val kasko = kaskoInput.text.toString()

                    val plateMap = mapOf(
                        "marka" to marka,
                        "model" to model,
                        "sigorta" to sigorta,
                        "ruhsat" to ruhsat,
                        "kasko" to kasko
                    )

                    ref.child("plakalar").child(plaka).setValue(plateMap)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "✅ Plaka eklendi!", Toast.LENGTH_SHORT).show()
                            plakaList.add(plaka)
                            adapter.notifyItemInserted(plakaList.size - 1)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "❌ Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("İptal", null)
                .show()
        }

        editPasswordButton.setOnClickListener {
            showPasswordChangeDialog(auth)
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), MainActivity::class.java))
            activity?.finish()
        }

        return view
    }

    private fun showPasswordChangeDialog(auth: FirebaseAuth) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val currentPasswordInput = dialogView.findViewById<EditText>(R.id.inputCurrentPassword)
        val newPasswordInput = dialogView.findViewById<EditText>(R.id.inputNewPassword)

        AlertDialog.Builder(requireContext())
            .setTitle("Şifre Değiştir")
            .setView(dialogView)
            .setPositiveButton("Güncelle") { _, _ ->
                val currentPassword = currentPasswordInput.text.toString()
                val newPassword = newPasswordInput.text.toString()

                if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                    Toast.makeText(requireContext(), "❗ Tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val user = auth.currentUser
                val credential = EmailAuthProvider.getCredential(user?.email!!, currentPassword)

                user.reauthenticate(credential).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Toast.makeText(requireContext(), "✅ Şifre güncellendi!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "❌ Güncelleme hatası: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "❗ Mevcut şifre yanlış!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun loadUserData(ref: DatabaseReference) {
        ref.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                textName.text = snapshot.child("adSoyad").getValue(String::class.java) ?: "-"
                textEmail.text = snapshot.child("eposta").getValue(String::class.java) ?: "-"
                textPhone.text = snapshot.child("telefon").getValue(String::class.java) ?: "-"

                plakaList.clear()
                val plakalarSnapshot = snapshot.child("plakalar")
                for (plakaSnap in plakalarSnapshot.children) {
                    plakaList.add(plakaSnap.key ?: "")
                }
                adapter.notifyDataSetChanged()

                checkInsuranceAndKasko(plakalarSnapshot)

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
    }

    private fun checkInsuranceAndKasko(plakalarSnapshot: DataSnapshot) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()

        plakalarSnapshot.children.forEach { plaka ->
            val plakaNo = plaka.key ?: return@forEach
            val sigortaDateStr = plaka.child("sigorta").getValue(String::class.java)
            val kaskoDateStr = plaka.child("kasko").getValue(String::class.java)

            sigortaDateStr?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        val sigortaDate = LocalDate.parse(it, formatter)
                        val daysLeft = ChronoUnit.DAYS.between(today, sigortaDate)
                        if (daysLeft == 30L || daysLeft == 15L || daysLeft == 7L) {
                            saveMessageToFirebase("Sigorta Hatırlatma", "$plakaNo sigortası $daysLeft gün içinde bitiyor!")
                        }
                    } catch (e: Exception) {
                        Log.e("DATE_PARSE", "Sigorta tarihi okunamadı: ${e.message}")
                    }
                }
            }

            kaskoDateStr?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        val kaskoDate = LocalDate.parse(it, formatter)
                        val daysLeft = ChronoUnit.DAYS.between(today, kaskoDate)
                        if (daysLeft == 30L || daysLeft == 15L || daysLeft == 7L) {
                            saveMessageToFirebase("Kasko Hatırlatma", "$plakaNo kaskosu $daysLeft gün içinde bitiyor!")
                        }
                    } catch (e: Exception) {
                        Log.e("DATE_PARSE", "Kasko tarihi okunamadı: ${e.message}")
                    }
                }
            }
        }
    }

    private fun saveMessageToFirebase(title: String, message: String) {
        val adminRef = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("adminMessages").child("bildirimler")
        val newKey = adminRef.push().key ?: return
        val bildirimData = mapOf(
            "baslik" to title,
            "mesaj" to message,
            "zaman" to System.currentTimeMillis().toString(),
            "tip" to "warning"
        )
        adminRef.child(newKey).setValue(bildirimData)
    }

    private fun showEditDialog(title: String, currentValue: String, onSave: (String) -> Unit) {
        val input = EditText(requireContext())
        input.setText(currentValue)

        AlertDialog.Builder(requireContext())
            .setTitle("$title Düzenle")
            .setView(input)
            .setPositiveButton("Kaydet") { _, _ ->
                val newValue = input.text.toString()
                if (newValue.isNotEmpty()) {
                    onSave(newValue)
                    Toast.makeText(requireContext(), "✅ $title güncellendi", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}
