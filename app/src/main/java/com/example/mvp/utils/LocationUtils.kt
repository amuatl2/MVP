package com.example.mvp.utils

object LocationUtils {
    /**
     * Calculate approximate distance between two locations
     * Returns distance in miles
     * Simplified calculation: same city = 0, same state = 50, different state = 200
     */
    fun calculateDistance(
        city1: String?,
        state1: String?,
        city2: String?,
        state2: String?
    ): Float {
        if (city1 == null || state1 == null || city2 == null || state2 == null) {
            return 200f // Default to far if location data missing
        }
        
        return when {
            city1.equals(city2, ignoreCase = true) && state1.equals(state2, ignoreCase = true) -> 0f
            state1.equals(state2, ignoreCase = true) -> 50f
            else -> 200f
        }
    }
}





