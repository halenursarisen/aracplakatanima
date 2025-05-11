package com.example.kullaniciapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kullaniciapp.ui.theme.Bildirim
import com.example.kullaniciapp.ui.theme.BildirimAdapter
import com.google.firebase.database.FirebaseDatabase

class BildirimFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BildirimAdapter
    private val bildirimList = mutableListOf<Bildirim>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bildirim, container, false)

        recyclerView = view.findViewById(R.id.bildirimRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = BildirimAdapter(bildirimList)
        recyclerView.adapter = adapter

        // Firebase verilerini çek
        FirebaseDatabase.getInstance().reference
            .child("bildirimler")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    bildirimList.clear()
                    for (child in snapshot.children) {
                        val mesaj = child.child("mesaj").value.toString()
                        val zaman = child.child("zaman").value.toString()
                        val tip = child.child("tip").value.toString()
                        bildirimList.add(Bildirim(mesaj, zaman, tip))
                    }
                    adapter.notifyDataSetChanged()
                }
            }

        return view
    }
}
