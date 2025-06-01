package com.example.kullaniciapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kullaniciapp.ui.theme.Bildirim
import com.example.kullaniciapp.ui.theme.BildirimAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.database.FirebaseDatabase
import androidx.core.content.ContextCompat

class BildirimFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BildirimAdapter
    private lateinit var toolbar: MaterialToolbar
    private val bildirimList = mutableListOf<Bildirim>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bildirim, container, false)

        toolbar = view.findViewById(R.id.bildirimToolbar)
        recyclerView = view.findViewById(R.id.bildirimRecyclerView)

        // RecyclerView ayarları
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = BildirimAdapter(bildirimList)
        recyclerView.adapter = adapter

        // Toolbar başlık ve sol ikon (sadece görsel)
        toolbar.title = "Bildirimler"
        val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_notifications)
        icon?.setBounds(0, 0, 64, 64)  // PNG büyükse burada küçültebilirsin
        toolbar.navigationIcon = icon

        // Sağ üst menü (çöp kutusu: Tümünü Sil)
        toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_clear_all) {
                bildirimList.clear()
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Tüm bildirimler temizlendi.", Toast.LENGTH_SHORT).show()
                true
            } else {
                false
            }
        }


        // Firebase'den bildirimleri çek
        FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
            .reference
            .child("adminMessages")
            .child("bildirimler")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    bildirimList.clear()
                    val tempList = mutableListOf<Bildirim>()
                    for (child in snapshot.children) {
                        val mesaj = child.child("mesaj").value?.toString() ?: continue
                        val zaman = child.child("zaman").value?.toString() ?: ""
                        val tip = child.child("tip").value?.toString() ?: "info"
                        tempList.add(Bildirim(mesaj, zaman, tip))
                    }
                    bildirimList.clear()
                    bildirimList.addAll(tempList.asReversed())  // Listeyi ters çevirerek ekliyoruz

                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
            }

        return view
    }
}
