package com.example.firebase


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    // Insert new message
    @Insert
    suspend fun insertMessage(chatEntity: ChatEntity)

    // Get all messages (latest first)
    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<ChatEntity>>

    // Get messages by search query
    @Query("SELECT * FROM chat_messages WHERE message LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessages(query: String): Flow<List<ChatEntity>>

    // Delete specific message
    @Delete
    suspend fun deleteMessage(chatEntity: ChatEntity)

    // Delete all messages
    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()

    // Get total message count
    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun getMessageCount(): Int
}