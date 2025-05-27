package com.example.kullaniciapp

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.*
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
    private lateinit var menuButton: ImageButton
    private val messages = mutableListOf<ChatMessage>()

    private var adminMessageMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chatbot, container, false)

        recyclerView = view.findViewById(R.id.chatRecyclerView)
        editTextMessage = view.findViewById(R.id.editTextMessage)
        buttonSend = view.findViewById(R.id.buttonSend)
        menuButton = view.findViewById(R.id.menuButton)

        adapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        if (messages.isEmpty()) {
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
        }

        adapter.clickListener = { selectedText ->
            if (selectedText == "Admin'e sor") {
                adminMessageMode = true
                showAdminMessageDialog()
            } else {
                addMessage(selectedText, isUser = true)
                saveMessageToFirebase(selectedText, isUser = true)
                getBotResponse(selectedText)
            }
        }

        buttonSend.setOnClickListener {
            val userMessage = editTextMessage.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                addMessage(userMessage, isUser = true)
                saveMessageToFirebase(userMessage, isUser = true)
                editTextMessage.setText("")
                getBotResponse(userMessage)
            }
        }

        menuButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Sohbeti Temizle")
                .setMessage("Tüm sohbet geçmişinizi silmek istiyor musunuz?")
                .setPositiveButton("Evet") { _, _ -> clearChatHistory() }
                .setNegativeButton("Hayır", null)
                .show()
        }

        loadMessagesFromFirebase()

        // Test düğümü (geliştirme kontrolü için)
        val testRef = FirebaseDatabase.getInstance().getReference("testNode")
        val testData = mapOf("check" to "hello test")
        testRef.push().setValue(testData)

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
            saveMessageToFirebase(botReply, isUser = false)
            if (!adminMessageMode) addBotOptionsAgain()
        }
    }

    private fun addBotOptionsAgain() {
        val lastOptions = messages.takeLast(6)
        val alreadyAdded = lastOptions.all { !it.isUser && it.isOption }
        if (alreadyAdded) return

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
                    saveMessageToFirebase("Admin'e mesajınız iletildi: \"$messageToAdmin\"", isUser = false)
                }
                dialog.dismiss()
            }
            .setNegativeButton("İptal") { dialog, _ -> dialog.cancel() }
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

        val database = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        val adminRef = database.getReference("adminMessages/messages")
        val userRef = database.getReference("userChats").child(uid)

        // Admin verisi kaydedilsin
        adminRef.push().setValue(data)
        // Kullanıcı geçmişine de aynı mesaj isUser: false ile yazılsın
        val userMessage = mapOf(
            "message" to message,
            "isUser" to false,
            "timestamp" to timestamp
        )
        userRef.push().setValue(userMessage)
    }

    private fun saveMessageToFirebase(message: String, isUser: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val timestamp = System.currentTimeMillis()

        val messageData = mapOf(
            "message" to message,
            "isUser" to isUser,
            "timestamp" to timestamp
        )

        val database = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        val ref = database.getReference("userChats").child(uid)

        ref.push()
            .setValue(messageData)
    }


    private fun loadMessagesFromFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        val ref = database.getReference("userChats").child(uid)

        ref.get().addOnSuccessListener { snapshot ->
            messages.clear()
            snapshot.children.forEach { child ->
                val message = child.child("message").getValue(String::class.java) ?: return@forEach
                val isUser = child.child("isUser").getValue(Boolean::class.java) ?: false
                messages.add(ChatMessage(message, isUser))
            }
            adapter.notifyDataSetChanged()
            recyclerView.scrollToPosition(messages.size - 1)
        }
    }


    private fun clearChatHistory() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "testUser"
        val database = FirebaseDatabase.getInstance("https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/")
        val userRef = database.getReference("userChats").child(uid)

        userRef.removeValue()
            .addOnSuccessListener {
                messages.clear()
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Sohbet geçmişi temizlendi.", Toast.LENGTH_SHORT).show()

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
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Temizleme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
