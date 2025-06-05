package com.example.kullaniciapp

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationServices

class ChatbotFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatAdapter
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: Button
    private lateinit var menuButton: ImageButton
    private val messages = mutableListOf<ChatMessage>()

    private var adminMessageMode = false
    private var havaDurumu: String? = null

    private val databaseUrl = "https://aracplakatanima-default-rtdb.europe-west1.firebasedatabase.app/"
    private val database = FirebaseDatabase.getInstance(databaseUrl)

    companion object {
        private const val API_KEY = "e8ed0a257bbb22d0dbaa89c3457c8ff2"
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater, container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        val view = inflater.inflate(R.layout.fragment_chatbot, container, false)

        recyclerView = view.findViewById(R.id.chatRecyclerView)
        editTextMessage = view.findViewById(R.id.editTextMessage)
        buttonSend = view.findViewById(R.id.buttonSend)
        menuButton = view.findViewById(R.id.menuButton)

        adapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        if (messages.isEmpty()) {
            addStarterMessages()
        }

        // Hava durumu bilgisini çekmek için bu fonksiyonu çağırın
        fetchWeatherAndStore()

        adapter.clickListener = { selectedText ->
            when (selectedText) {
                "Admin'e sor" -> {
                    adminMessageMode = true
                    showAdminMessageDialog()
                }
                "Yeni bir sorunuz var mı?" -> {
                    // Bu sadece soru mesajı, kullanıcı cevaplayamaz, boş bırakabiliriz
                }
                "Evet" -> {
                    // Admin'e sor seçeneğini göster
                    val adminOption = ChatMessage("Admin'e sor", isUser = false, isOption = true)
                    messages.add(adminOption)
                    adapter.notifyItemInserted(messages.size - 1)
                    recyclerView.scrollToPosition(messages.size - 1)
                }
                "Hayır" -> {
                    // Chatbotu starter mesajlara döndür
                    messages.clear()
                    addStarterMessages()
                }
                else -> {
                    addMessage(selectedText, isUser = true)
                    getBotResponse(selectedText)
                }
            }
        }

        buttonSend.setOnClickListener {
            val userMessage = editTextMessage.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                addMessage(userMessage, isUser = true)
                editTextMessage.setText("")
                getBotResponse(userMessage)
            }
        }

        menuButton.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(requireContext(), "Kullanıcı oturumu bulunamadı.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkPendingAdminReply(uid) { hasPending ->
                if (hasPending) {
                    Toast.makeText(requireContext(),
                        "Admin cevabı gelmeden sohbet temizlenemez.", Toast.LENGTH_LONG).show()
                } else {
                    clearChatHistory(uid)
                }
            }
        }

        loadMessagesFromAdminMessages()
        listenForAdminRepliesRealtime()

        return view
    }

    private fun fetchWeatherAndStore() {
        val context = requireContext()

        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude

                    Log.d("Konum", "Lat: $lat, Lon: $lon")

                    WeatherService.api.getWeatherByCoordinates(lat, lon, API_KEY)
                        .enqueue(object : Callback<WeatherResponse> {
                            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                                if (response.isSuccessful) {
                                    val weather = response.body()
                                    val desc = weather?.weather?.firstOrNull()?.description ?: "bilgi yok"
                                    val temp = weather?.main?.temp?.toInt() ?: 0
                                    havaDurumu = "Hava: $desc, $temp°C"
                                    Log.d("Hava", "Alındı: $havaDurumu")
                                } else {
                                    havaDurumu = "Hava bilgisi alınamadı."
                                    Log.e("Hava", "Yanıt başarısız: ${response.code()}")
                                }
                            }

                            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                                havaDurumu = "Hava bilgisi alınamadı."
                                Log.e("Hava", "Hata: ${t.message}")
                            }
                        })
                } else {
                    havaDurumu = "Konum alınamadı"
                    Log.e("Konum", "getCurrentLocation null döndü")
                }
            }.addOnFailureListener {
                havaDurumu = "Konum alınamadı"
                Log.e("Konum", "getCurrentLocation başarısız: ${it.message}")
            }
    }

    private fun getBotResponse(userText: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        ChatBot.getResponse(userText, uid, havaDurumu) { botReply ->
            addMessage(botReply, isUser = false)
            if (!adminMessageMode) addBotOptionsAgain()
        }
    }

    private fun addStarterMessages() {
        val starterMessages = listOf(
            ChatMessage("Merhaba! Ne öğrenmek istersiniz?", isUser = false),
            ChatMessage("Plakam ne?", isUser = false, isOption = true),
            ChatMessage("Giriş saatim?", isUser = false, isOption = true),
            ChatMessage("Park yerim?", isUser = false, isOption = true),
            ChatMessage("Ücret ne kadar?", isUser = false, isOption = true),
            ChatMessage("Geçmiş ödeme?", isUser = false, isOption = true),
            ChatMessage("Bugünkü hava durumu", isUser = false, isOption = true), // Bu satır eklendi
            ChatMessage("Admin'e sor", isUser = false, isOption = true)
        )
        messages.addAll(starterMessages)
        adapter.notifyDataSetChanged()
    }

    private fun addMessage(text: String, isUser: Boolean) {
        messages.add(ChatMessage(text, isUser))
        adapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
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
            ChatMessage("Bugünkü hava durumu", isUser = false, isOption = true), // Bu satır eklendi
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
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setPositiveButton
                    archiveAdminMessages(uid, {
                        sendMessageToAdmin(messageToAdmin)
                        addMessage("Admin'e mesajınız iletildi: \"$messageToAdmin\"", isUser = false)
                    }, { error ->
                        Toast.makeText(requireContext(), "Mesaj arşivleme hatası: ${error.message}", Toast.LENGTH_LONG).show()
                    })
                }
                dialog.dismiss()
            }
            .setNegativeButton("İptal") { dialog, _ -> dialog.cancel() }
            .show()
    }

    private fun saveUserMessageToAdminMessages(message: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val timestamp = System.currentTimeMillis()

        val data = mapOf(
            "userId" to uid,
            "message" to message,
            "isUser" to true,
            "timestamp" to timestamp
        )
        val adminMessagesRef = database.getReference("adminMessages/messages")
        adminMessagesRef.push().setValue(data)
    }

    private fun sendMessageToAdmin(message: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "testUser"
        val timestamp = System.currentTimeMillis()

        val data = mapOf(
            "userId" to uid,
            "message" to message,
            "timestamp" to timestamp,
            "isUser" to true
        )

        val adminMessagesRef = database.getReference("adminMessages/messages")
        adminMessagesRef.push().setValue(data)
    }

    private fun loadMessagesFromAdminMessages() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val adminMessagesRef = database.getReference("adminMessages/messages")

        adminMessagesRef.orderByChild("userId").equalTo(uid).get().addOnSuccessListener { snapshot ->
            messages.clear()
            snapshot.children.sortedBy { it.child("timestamp").getValue(Long::class.java) ?: 0L }
                .forEach { child ->
                    val message = child.child("message").getValue(String::class.java) ?: return@forEach
                    val isUser = child.child("isUser").getValue(Boolean::class.java) ?: false
                    messages.add(ChatMessage(message, isUser))
                }
            adapter.notifyDataSetChanged()
            recyclerView.scrollToPosition(messages.size - 1)
        }
    }

    private fun clearChatHistory(uid: String) {
        val adminMessagesRef = database.getReference("adminMessages/messages")
        adminMessagesRef.orderByChild("userId").equalTo(uid).get().addOnSuccessListener { snapshot ->
            archiveAdminMessages(uid, {
                snapshot.children.forEach { it.ref.removeValue() }
                messages.clear()
                addStarterMessages()
                Toast.makeText(requireContext(), "Sohbet geçmişi temizlendi.", Toast.LENGTH_SHORT).show()
            }, { error ->
                Toast.makeText(requireContext(), "Arşivleme hatası: ${error.message}", Toast.LENGTH_LONG).show()
            })
        }
    }

    private fun checkPendingAdminReply(uid: String, callback: (Boolean) -> Unit) {
        val adminMessagesRef = database.getReference("adminMessages/messages")
        adminMessagesRef.orderByChild("userId").equalTo(uid).get()
            .addOnSuccessListener { snapshot ->
                val pending = snapshot.children.any {
                    it.child("isUser").getValue(Boolean::class.java) == true && it.child("cevap").getValue(String::class.java).isNullOrBlank()
                }
                callback(pending)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun archiveAdminMessages(uid: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val adminMessagesRef = database.getReference("adminMessages/messages")
        val archiveRef = database.getReference("adminMessages/gecmisMesajlar").child(uid).push()

        adminMessagesRef.orderByChild("userId").equalTo(uid).get()
            .addOnSuccessListener { snapshot ->
                val messagesMap = mutableMapOf<String, Any>()
                snapshot.children.forEach { child ->
                    child.key?.let { key ->
                        messagesMap[key] = child.value ?: ""
                    }
                }
                val archiveData = mapOf(
                    "timestamp" to System.currentTimeMillis(),
                    "messages" to messagesMap
                )
                archiveRef.setValue(archiveData)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { onError(it) }
            }
            .addOnFailureListener { onError(it) }
    }

    private fun listenForAdminRepliesRealtime() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val adminMessagesRef = database.getReference("adminMessages/messages")
        adminMessagesRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val userId = snapshot.child("userId").getValue(String::class.java)
                val adminReply = snapshot.child("cevap").getValue(String::class.java)
                if (userId == uid && !adminReply.isNullOrBlank()) {
                    val replyMessage = "Admin cevabı: $adminReply"
                    if (!messages.any { it.message == replyMessage }) {
                        addMessage(replyMessage, isUser = false)
                        showNewQuestionPrompt()
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val userId = snapshot.child("userId").getValue(String::class.java)
                val adminReply = snapshot.child("cevap").getValue(String::class.java)
                if (userId == uid && !adminReply.isNullOrBlank()) {
                    val replyMessage = "Admin cevabı: $adminReply"
                    val index = messages.indexOfFirst { it.message.startsWith("Admin cevabı:") }
                    if (index != -1) {
                        messages[index] = ChatMessage(replyMessage, isUser = false)
                        adapter.notifyItemChanged(index)
                    } else {
                        addMessage(replyMessage, isUser = false)
                    }
                    showNewQuestionPrompt()
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val userId = snapshot.child("userId").getValue(String::class.java)
                val adminReply = snapshot.child("cevap").getValue(String::class.java)
                if (userId == uid && !adminReply.isNullOrBlank()) {
                    val replyMessage = "Admin cevabı: $adminReply"
                    val index = messages.indexOfFirst { it.message == replyMessage }
                    if (index != -1) {
                        messages.removeAt(index)
                        adapter.notifyItemRemoved(index)
                    }
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showNewQuestionPrompt() {
        // Önce "Yeni bir sorunuz var mı?" sorusunu ekle
        val prompt = ChatMessage("Yeni bir sorunuz var mı?", isUser = false, isOption = false)
        messages.add(prompt)
        adapter.notifyItemInserted(messages.size - 1)

        // Sonra "Evet" ve "Hayır" seçeneklerini ekle
        val yesOption = ChatMessage("Evet", isUser = false, isOption = true)
        val noOption = ChatMessage("Hayır", isUser = false, isOption = true)
        messages.add(yesOption)
        messages.add(noOption)
        adapter.notifyItemRangeInserted(messages.size - 2, 2)

        recyclerView.scrollToPosition(messages.size - 1)
    }
}