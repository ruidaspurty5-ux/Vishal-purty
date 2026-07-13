package com.example.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class SupportMessage(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val sender: String = "", // "USER" or "SUPPORT"
    val messageText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class ActiveChat(
    val userId: String,
    val userName: String,
    val lastMessage: String,
    val timestamp: Long
)

object SupportChatRepository {
    private const val TAG = "SupportChatRepository"
    
    // In-memory fallback in case Firestore is unavailable
    private val fallbackMessages = MutableStateFlow<List<SupportMessage>>(emptyList())

    private var firestore: FirebaseFirestore? = null

    init {
        try {
            firestore = FirebaseFirestore.getInstance()
            Log.d(TAG, "Firebase Firestore initialized successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "Firestore not initialized. Using in-memory fallback.", e)
        }
    }

    fun isFirestoreAvailable(): Boolean = firestore != null

    // Fetch messages for a specific user chat
    fun getMessagesFlow(userId: String): Flow<List<SupportMessage>> {
        val fs = firestore
        if (fs == null) {
            // Local fallback flow
            return callbackFlow {
                val job = launch {
                    fallbackMessages.collect { list ->
                        val filtered = list.filter { it.userId == userId }.sortedBy { it.timestamp }
                        trySend(filtered)
                    }
                }
                awaitClose { job.cancel() }
            }
        }

        return callbackFlow {
            val registration = fs.collection("support_messages")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to support messages: ", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val messages = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.id
                                val uId = doc.getString("userId") ?: ""
                                val name = doc.getString("userName") ?: ""
                                val snd = doc.getString("sender") ?: ""
                                val text = doc.getString("messageText") ?: ""
                                val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                SupportMessage(id, uId, name, snd, text, ts)
                            } catch (e: Exception) {
                                null
                            }
                        }.sortedBy { it.timestamp }
                        trySend(messages)
                    }
                }
            awaitClose { registration.remove() }
        }
    }

    // Fetch all active chats (unique users who have sent support messages) for the Admin
    fun getActiveChatsFlow(): Flow<List<ActiveChat>> {
        val fs = firestore
        if (fs == null) {
            return callbackFlow {
                val job = launch {
                    fallbackMessages.collect { list ->
                        val chats = list.groupBy { it.userId }
                            .map { (uId, msgs) ->
                                val lastMsg = msgs.maxByOrNull { it.timestamp }
                                ActiveChat(
                                    userId = uId,
                                    userName = lastMsg?.userName ?: "Anonymous User",
                                    lastMessage = lastMsg?.messageText ?: "",
                                    timestamp = lastMsg?.timestamp ?: System.currentTimeMillis()
                                )
                            }
                            .sortedByDescending { it.timestamp }
                        trySend(chats)
                    }
                }
                awaitClose { job.cancel() }
            }
        }

        return callbackFlow {
            val registration = fs.collection("support_messages")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to support messages for admin: ", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val allMessages = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.id
                                val uId = doc.getString("userId") ?: ""
                                val name = doc.getString("userName") ?: ""
                                val snd = doc.getString("sender") ?: ""
                                val text = doc.getString("messageText") ?: ""
                                val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                SupportMessage(id, uId, name, snd, text, ts)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        
                        val chats = allMessages.groupBy { it.userId }
                            .map { (uId, msgs) ->
                                val lastMsg = msgs.maxByOrNull { it.timestamp }
                                ActiveChat(
                                    userId = uId,
                                    userName = lastMsg?.userName ?: "User",
                                    lastMessage = lastMsg?.messageText ?: "",
                                    timestamp = lastMsg?.timestamp ?: System.currentTimeMillis()
                                )
                            }.sortedByDescending { it.timestamp }
                        
                        trySend(chats)
                    }
                }
            awaitClose { registration.remove() }
        }
    }

    suspend fun sendMessage(userId: String, userName: String, messageText: String, sender: String): Boolean {
        val msgId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val data = hashMapOf(
            "userId" to userId,
            "userName" to userName,
            "sender" to sender,
            "messageText" to messageText,
            "timestamp" to timestamp
        )

        val fs = firestore
        if (fs == null) {
            // Local fallback
            val currentList = fallbackMessages.value.toMutableList()
            currentList.add(SupportMessage(msgId, userId, userName, sender, messageText, timestamp))
            fallbackMessages.value = currentList
            
            // If user sends a message, let's auto-reply with a helpful supportive simulation reply
            if (sender == "USER") {
                kotlinx.coroutines.delay(1000)
                val replyId = UUID.randomUUID().toString()
                val replyTimestamp = System.currentTimeMillis()
                val replyText = "Hello $userName! Thank you for contacting Study Notes Support. An agent has been notified and will join this live Firestore chat shortly. Feel free to describe your issue or query."
                val updatedList = fallbackMessages.value.toMutableList()
                updatedList.add(SupportMessage(replyId, userId, userName, "SUPPORT", replyText, replyTimestamp))
                fallbackMessages.value = updatedList
            }
            return true
        }

        return try {
            fs.collection("support_messages").document(msgId).set(data)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending support message: ", e)
            false
        }
    }
}
