package com.example.kullaniciapp

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ChatbotFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatAdapter
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: Button
    private val messages = mutableListOf<ChatMessage>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chatbot, container, false)

        recyclerView = view.findViewById(R.id.chatRecyclerView)
        editTextMessage = view.findViewById(R.id.editTextMessage)
        buttonSend = view.findViewById(R.id.buttonSend)

        adapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        adapter.clickListener = { selectedText ->
            if (selectedText == "Admin'e sor") {
                showAdminMessageDialog()
            } else {
                addMessage(selectedText, isUser = true)
                getBotResponse(selectedText)
            }
        }

        val starterMessages = listOf(
            ChatMessage("Merhaba! Ne öğrenmek istersiniz?", isUser = false),
            ChatMessage("Plakam ne?", isUser = false, isOption = true),
            ChatMessage("Giriş saatim?", isUser = false, isOption = true),
            ChatMessage("Park yerim?", isUser = false, isOption = true),
            ChatMessage("Ücret ne kadar?", isUser = false, isOption = true),
            ChatMessage("Geçmiş ödeme?", isUser = false, isOption = true),
            ChatMessage("Admin'e sor", isUser = false, isOption = true)
        )
        messages.addAll(starterMessages)
        adapter.notifyDataSetChanged()
        recyclerView.scrollToPosition(messages.size - 1)

        buttonSend.setOnClickListener {
            val userMessage = editTextMessage.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                addMessage(userMessage, isUser = true)
                editTextMessage.setText("")
                getBotResponse(userMessage)
            }
        }

        return view
    }

    private fun addMessage(text: String, isUser: Boolean) {
        messages.add(ChatMessage(text, isUser))
        adapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun getBotResponse(userText: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        ChatBot.getResponse(userText, uid) { botReply ->
            addMessage(botReply, isUser = false)
            addBotOptionsAgain()
        }
    }

    private fun addBotOptionsAgain() {
        val options = listOf(
            ChatMessage("Plakam ne?", isUser = false, isOption = true),
            ChatMessage("Giriş saatim?", isUser = false, isOption = true),
            ChatMessage("Park yerim?", isUser = false, isOption = true),
            ChatMessage("Ücret ne kadar?", isUser = false, isOption = true),
            ChatMessage("Geçmiş ödeme?", isUser = false, isOption = true),
            ChatMessage("Admin'e sor", isUser = false, isOption = true)
        )
        messages.addAll(options)
        adapter.notifyItemRangeInserted(messages.size - options.size, options.size)
        recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun showAdminMessageDialog() {
        val input = EditText(requireContext())
        input.hint = "Admin'e iletmek istediğiniz mesajı yazın"

        AlertDialog.Builder(requireContext())
            .setTitle("Admin'e Mesaj Gönder")
            .setView(input)
            .setPositiveButton("Gönder") { dialog, _ ->
                val messageToAdmin = input.text.toString().trim()
                if (messageToAdmin.isNotEmpty()) {
                    sendMessageToAdmin(messageToAdmin)
                    addMessage("Admin'e mesajınız iletildi: \"$messageToAdmin\"", isUser = false)
                }
                dialog.dismiss()
            }
            .setNegativeButton("İptal") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun sendMessageToAdmin(message: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "testUser"
        val timestamp = System.currentTimeMillis()

        val data = mapOf(
            "userId" to uid,
            "message" to message,
            "timestamp" to timestamp
        )

        val ref = FirebaseDatabase.getInstance()
            .getReference("adminMessages")
            .push()

        ref.setValue(data)
            .addOnSuccessListener {
                Log.d("AdminFirebase", "✅ Admin mesajı kaydedildi.")
            }
            .addOnFailureListener { e ->
                Log.e("AdminFirebase", "❌ Admin mesajı hatası: ${e.message}")
            }
    }
}
