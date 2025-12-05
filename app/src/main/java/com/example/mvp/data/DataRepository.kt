package com.example.mvp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_data")

class DataRepository(private val context: Context) {
    private val gson = Gson()
    
    // Keys for storing data per user
    private fun ticketsKey(userEmail: String) = stringPreferencesKey("tickets_$userEmail")
    private fun jobsKey(userEmail: String) = stringPreferencesKey("jobs_$userEmail")
    private val currentUserKey = stringPreferencesKey("current_user")
    private val lastViewedTimestampsKey = stringPreferencesKey("last_viewed_timestamps")
    
    // Save current user
    suspend fun saveCurrentUser(user: User) {
        context.dataStore.edit { preferences ->
            preferences[currentUserKey] = gson.toJson(user)
        }
    }
    
    // Get current user
    suspend fun getCurrentUser(): User? {
        val userJson = context.dataStore.data.first()[currentUserKey] ?: return null
        return try {
            gson.fromJson(userJson, User::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    // Save tickets for a specific user
    // For landlords: saves all tickets
    // For tenants: saves only their own tickets
    suspend fun saveTickets(userEmail: String, tickets: List<Ticket>) {
        context.dataStore.edit { preferences ->
            // Get all existing tickets from all users
            val allExistingTickets = mutableMapOf<String, Ticket>()
            
            preferences.asMap().forEach { (key, value) ->
                if (key.name.startsWith("tickets_")) {
                    try {
                        val listType = object : TypeToken<List<Ticket>>() {}.type
                        val userTickets = gson.fromJson<List<Ticket>>(value as String, listType)
                        userTickets?.forEach { ticket ->
                            allExistingTickets[ticket.id] = ticket
                        }
                    } catch (e: Exception) {
                        // Skip invalid entries
                    }
                }
            }
            
            // Update with new tickets (new tickets override old ones with same ID)
            tickets.forEach { ticket ->
                allExistingTickets[ticket.id] = ticket
            }
            
            // Save back to the user's key (for landlords, this contains all tickets)
            preferences[ticketsKey(userEmail)] = gson.toJson(allExistingTickets.values.toList())
        }
    }
    
    // Get tickets for a specific user
    @Suppress("UNUSED")
    suspend fun getTickets(userEmail: String): List<Ticket> {
        val ticketsJson = context.dataStore.data.first()[ticketsKey(userEmail)] ?: return emptyList()
        return try {
            val listType = object : TypeToken<List<Ticket>>() {}.type
            gson.fromJson(ticketsJson, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Save jobs for a specific user (contractor)
    suspend fun saveJobs(userEmail: String, jobs: List<Job>) {
        context.dataStore.edit { preferences ->
            preferences[jobsKey(userEmail)] = gson.toJson(jobs)
        }
    }
    
    // Get jobs for a specific user (contractor)
    @Suppress("UNUSED")
    suspend fun getJobs(userEmail: String): List<Job> {
        val jobsJson = context.dataStore.data.first()[jobsKey(userEmail)] ?: return emptyList()
        return try {
            val listType = object : TypeToken<List<Job>>() {}.type
            gson.fromJson(jobsJson, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Get all tickets across all users (for landlords to see all tickets)
    // Deduplicates by ticket ID
    suspend fun getAllTickets(): List<Ticket> {
        val allTicketsMap = mutableMapOf<String, Ticket>()
        val preferences = context.dataStore.data.first()
        
        preferences.asMap().forEach { (key, value) ->
            if (key.name.startsWith("tickets_")) {
                try {
                    val listType = object : TypeToken<List<Ticket>>() {}.type
                    val tickets = gson.fromJson<List<Ticket>>(value as String, listType)
                    tickets?.forEach { ticket ->
                        // Keep the most recent version of each ticket (by ID)
                        allTicketsMap[ticket.id] = ticket
                    }
                } catch (e: Exception) {
                    // Skip invalid entries
                }
            }
        }
        
        return allTicketsMap.values.toList()
    }
    
    // Get all jobs across all users (for contractors to see available jobs)
    suspend fun getAllJobs(): List<Job> {
        val allJobs = mutableListOf<Job>()
        val preferences = context.dataStore.data.first()
        
        preferences.asMap().forEach { (key, value) ->
            if (key.name.startsWith("jobs_")) {
                try {
                    val listType = object : TypeToken<List<Job>>() {}.type
                    val jobs = gson.fromJson<List<Job>>(value as String, listType)
                    allJobs.addAll(jobs ?: emptyList())
                } catch (e: Exception) {
                    // Skip invalid entries
                }
            }
        }
        
        return allJobs
    }
    
    // Clear user data (on logout)
    @Suppress("UNUSED")
    suspend fun clearUserData(userEmail: String) {
        context.dataStore.edit { preferences ->
            preferences.remove(ticketsKey(userEmail))
            preferences.remove(jobsKey(userEmail))
        }
    }
    
    // Save last viewed timestamps for messages
    suspend fun saveLastViewedTimestamps(timestamps: Map<String, String>) {
        context.dataStore.edit { preferences ->
            preferences[lastViewedTimestampsKey] = gson.toJson(timestamps)
        }
    }
    
    // Get last viewed timestamps for messages
    suspend fun getLastViewedTimestamps(): Map<String, String> {
        val timestampsJson = context.dataStore.data.first()[lastViewedTimestampsKey] ?: return emptyMap()
        return try {
            val mapType = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(timestampsJson, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

