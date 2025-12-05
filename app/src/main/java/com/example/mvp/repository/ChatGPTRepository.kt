package com.example.mvp.repository

import com.example.mvp.api.ChatGPTService
import com.example.mvp.api.ChatMessage
import com.example.mvp.api.ChatRequest
import com.example.mvp.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatGPTRepository {
    private val apiService = RetrofitClient.chatGPTService
    
    // TODO: Replace with your actual OpenAI API key
    // IMPORTANT: Never commit this key to Git!
    // For production, use secure storage or environment variables
    private val apiKey = "YOUR_OPENAI_API_KEY_HERE"
    
    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<ChatMessage> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val messages = mutableListOf<ChatMessage>()
            
            // Add system message to set AI behavior
            messages.add(
                ChatMessage(
                    role = "system",
                    content = "You are a helpful HOME (Housing Operations & Maintenance Engine) AI Assistant. " +
                            "You help users with maintenance questions, ticket tracking, contractor recommendations, " +
                            "scheduling, and property management. Be concise and helpful."
                )
            )
            
            // Add conversation history
            messages.addAll(conversationHistory)
            
            // Add current user message
            messages.add(ChatMessage(role = "user", content = userMessage))
            
            val request = ChatRequest(
                model = "gpt-3.5-turbo",
                messages = messages,
                max_tokens = 500,
                temperature = 0.7
            )
            
            val response = apiService.sendMessage(
                authorization = "Bearer $apiKey",
                request = request
            )
            
            if (response.isSuccessful && response.body() != null) {
                val aiResponse = response.body()!!.choices.firstOrNull()?.message?.content
                    ?: "Sorry, I couldn't generate a response."
                Result.success(aiResponse)
            } else {
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

