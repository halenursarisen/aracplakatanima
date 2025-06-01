package com.example.kullaniciapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
<<<<<<< HEAD
import android.widget.Toast
import androidx.core.content.ContextCompat
=======
>>>>>>> 44d3236e304bccba2f7b47a1a2eb8beafebd2045
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kullaniciapp.ui.theme.Bildirim
import com.example.kullaniciapp.ui.theme.BildirimAdapter
<<<<<<< HEAD
import com.google.android.material.appbar.MaterialToolbar
=======
>>>>>>> 44d3236e304bccba2f7b47a1a2eb8beafebd2045
import com.google.firebase.database.FirebaseDatabase

class BildirimFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BildirimAdapter
<<<<<<< HEAD
    private lateinit var toolbar: MaterialToolbar

    private val bildirimList = mutableListOf<Bildirim>()
    private val firebaseKeyList = mutableListOf<String>()
=======
    private val bildirimList = mutableListOf<Bildirim>()
>>>>>>> 44d3236e304bccba2f7b47a1a2eb8beafebd2045

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bildirim, container, false)

<<<<<<< HEAD
        toolbar = view.findViewById(R.id.bildirimToolbar)
        recyclerView = view.findViewById(R.id.bildirimRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = BildirimAdapter(bildirimList) { position ->
            val key = firebaseKeyList[position]
            val database = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
            database.getReference("adminMessages").child("bildirimler").child(key)
                .removeValue()
                .addOnSuccessListener {
                    firebaseKeyList.removeAt(position)
                    Toast.makeText(requireContext(), "Bildirim silindi", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        recyclerView.adapter = adapter

        // Toolbar ayarları
        toolbar.title = "Bildirimler"
        val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_notifications)
        icon?.setBounds(0, 0, 64, 64)
        toolbar.navigationIcon = icon

        toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_clear_all) {
                clearAllNotifications()
                true
            } else {
                false
            }
        }

        fetchNotifications()

        return view
    }

    private fun fetchNotifications() {
        val database = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        database.getReference("adminMessages").child("bildirimler")
=======
        recyclerView = view.findViewById(R.id.bildirimRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = BildirimAdapter(bildirimList)
        recyclerView.adapter = adapter

        // ✅ TEST VERİSİ — Firebase çalışmasa bile bu görünmeli!
        bildirimList.add(Bildirim("TEST mesajı", "Şimdi", "success"))
        adapter.notifyDataSetChanged()

        // 🔧 Firebase'den bildirimleri çek
        FirebaseDatabase.getInstance().reference
            .child("adminMessages")
            .child("bildirimler")
>>>>>>> 44d3236e304bccba2f7b47a1a2eb8beafebd2045
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    bildirimList.clear()
<<<<<<< HEAD
                    firebaseKeyList.clear()

=======
>>>>>>> 44d3236e304bccba2f7b47a1a2eb8beafebd2045
                    for (child in snapshot.children) {
                        val mesaj = child.child("mesaj").value?.toString() ?: continue
                        val zaman = child.child("zaman").value?.toString() ?: ""
                        val tip = child.child("tip").value?.toString() ?: "info"
<<<<<<< HEAD

                        bildirimList.add(Bildirim(mesaj, zaman, tip))
                        firebaseKeyList.add(child.key!!)
                    }

                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Bildirimler alınamadı", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearAllNotifications() {
        val database = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        database.getReference("adminMessages").child("bildirimler")
            .removeValue()
            .addOnSuccessListener {
                bildirimList.clear()
                firebaseKeyList.clear()
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Tüm bildirimler silindi.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Silinemedi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
=======
                        bildirimList.add(Bildirim(mesaj, zaman, tip))
                    }
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
            }

        return view
>>>>>>> 44d3236e304bccba2f7b47a1a2eb8beafebd2045
    }
}
