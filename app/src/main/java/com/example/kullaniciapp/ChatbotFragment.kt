package com.example.kullaniciapp

<<<<<<< HEAD
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Priority

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

        fetchWeatherAndStore()

        adapter.clickListener = { selectedText ->
            when (selectedText) {
                "Admin'e sor" -> {
                    adminMessageMode = true
                    showAdminMessageDialog()
                }
                "Yeni bir sorunuz var mı?" -> {}
                "Evet" -> {
                    val adminOption = ChatMessage("Admin'e sor", isUser = false, isOption = true)
                    messages.add(adminOption)
                    adapter.notifyItemInserted(messages.size - 1)
                    recyclerView.scrollToPosition(messages.size - 1)
                }
                "Hayır" -> {
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
                    Toast.makeText(requireContext(), "Admin cevabı gelmeden sohbet temizlenemez.", Toast.LENGTH_LONG).show()
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
            ChatMessage("Bugünkü hava durumu", isUser = false, isOption = true),
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
        val options = listOf(
            ChatMessage("Plakam ne?", isUser = false, isOption = true),
            ChatMessage("Giriş saatim?", isUser = false, isOption = true),
            ChatMessage("Park yerim?", isUser = false, isOption = true),
            ChatMessage("Ücret ne kadar?", isUser = false, isOption = true),
            ChatMessage("Geçmiş ödeme?", isUser = false, isOption = true),
            ChatMessage("Bugünkü hava durumu", isUser = false, isOption = true),
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
                    addMessage("Admin'e mesajınız iletildi: \"$messageToAdmin\"", isUser = false)
                }
                dialog.dismiss()
            }
            .setNegativeButton("İptal") { dialog, _ -> dialog.cancel() }
            .show()
    }

    private fun checkPendingAdminReply(uid: String, callback: (Boolean) -> Unit) {
        callback(false) // Şimdilik kontrolsüz geçiyoruz
    }

    private fun clearChatHistory(uid: String) {
        messages.clear()
        addStarterMessages()
        Toast.makeText(requireContext(), "Sohbet geçmişi temizlendi.", Toast.LENGTH_SHORT).show()
    }

    private fun loadMessagesFromAdminMessages() {
        // Firebase'den admin mesajları yüklenecekse buraya kod eklenebilir
    }

    private fun listenForAdminRepliesRealtime() {
        // Firebase'den admin cevabı dinlenecekse buraya kod eklenebilir
    }
}
=======
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ChatbotFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ChatbotFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chatbot, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ChatbotFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ChatbotFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
>>>>>>> 44d3236e304bccba2f7b47a1a2eb8beafebd2045
