package com.example.kullaniciapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        val textWelcome = view.findViewById<TextView>(R.id.textWelcome)
        val buttonLogout = view.findViewById<Button>(R.id.buttonLogout)

        textWelcome.text = "Hoş geldin, $userEmail"

        buttonLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), MainActivity::class.java))
            activity?.finish()
        }

        return view
    }
}
