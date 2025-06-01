package com.example.kullaniciapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kullaniciapp.ui.theme.Bildirim
import com.example.kullaniciapp.ui.theme.BildirimAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.database.FirebaseDatabase

class BildirimFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BildirimAdapter
    private lateinit var toolbar: MaterialToolbar

    private val bildirimList = mutableListOf<Bildirim>()
    private val firebaseKeyList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bildirim, container, false)

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
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    bildirimList.clear()
                    firebaseKeyList.clear()

                    for (child in snapshot.children) {
                        val mesaj = child.child("mesaj").value?.toString() ?: continue
                        val zaman = child.child("zaman").value?.toString() ?: ""
                        val tip = child.child("tip").value?.toString() ?: "info"

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
    }
}
