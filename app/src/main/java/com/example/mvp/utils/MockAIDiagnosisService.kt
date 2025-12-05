package com.example.mvp.utils

import kotlin.random.Random

data class AIDiagnosisResult(
    val issueType: String,
    val description: String,
    val possibleSolutions: List<String>
)

object MockAIDiagnosisService {
    
    fun generateDiagnosis(ticketTitle: String, ticketDescription: String, category: String): AIDiagnosisResult {
        val lowerTitle = ticketTitle.lowercase()
        val lowerDesc = ticketDescription.lowercase()
        val lowerCategory = category.lowercase()
        
        // Determine issue type and generate diagnosis based on category/keywords
        return when {
            lowerCategory.contains("plumbing") || lowerTitle.contains("leak") || 
            lowerTitle.contains("water") || lowerDesc.contains("leak") || 
            lowerDesc.contains("water") || lowerDesc.contains("pipe") ||
            lowerDesc.contains("faucet") || lowerDesc.contains("drain") -> {
                generatePlumbingDiagnosis(lowerTitle, lowerDesc)
            }
            lowerCategory.contains("electrical") || lowerTitle.contains("electrical") ||
            lowerDesc.contains("electrical") || lowerDesc.contains("outlet") ||
            lowerDesc.contains("light") || lowerDesc.contains("power") ||
            lowerDesc.contains("circuit") || lowerDesc.contains("fuse") -> {
                generateElectricalDiagnosis(lowerTitle, lowerDesc)
            }
            lowerCategory.contains("hvac") || lowerTitle.contains("hvac") ||
            lowerTitle.contains("heating") || lowerTitle.contains("cooling") ||
            lowerTitle.contains("air") || lowerDesc.contains("hvac") ||
            lowerDesc.contains("heating") || lowerDesc.contains("cooling") ||
            lowerDesc.contains("thermostat") || lowerDesc.contains("furnace") ||
            lowerDesc.contains("ac") || lowerDesc.contains("air conditioning") -> {
                generateHVACDiagnosis(lowerTitle, lowerDesc)
            }
            lowerCategory.contains("appliance") || lowerTitle.contains("appliance") ||
            lowerDesc.contains("appliance") || lowerDesc.contains("refrigerator") ||
            lowerDesc.contains("dishwasher") || lowerDesc.contains("washer") ||
            lowerDesc.contains("dryer") || lowerDesc.contains("oven") ||
            lowerDesc.contains("stove") || lowerDesc.contains("microwave") -> {
                generateApplianceDiagnosis(lowerTitle, lowerDesc)
            }
            else -> {
                generateGenericDiagnosis(lowerTitle, lowerDesc, category)
            }
        }
    }
    
    private fun generatePlumbingDiagnosis(title: String, desc: String): AIDiagnosisResult {
        val descriptions = listOf(
            "Based on the description, this appears to be a plumbing issue involving water flow or leakage. Common causes include worn seals, pipe corrosion, or blockages in the drainage system.",
            "The issue described suggests a plumbing problem that may involve water pressure, leaks, or drainage. This typically requires inspection of pipes, fixtures, and connections.",
            "This plumbing concern likely involves water-related problems such as leaks, clogs, or fixture malfunctions. Professional assessment is recommended to identify the root cause."
        )
        
        val solutions = listOf(
            "Inspect all visible pipes and connections for leaks or signs of water damage",
            "Check water pressure at affected fixtures - low pressure may indicate a blockage",
            "Test drain functionality - slow drains suggest clogs that may need professional clearing",
            "Examine seals and gaskets on faucets and fixtures for wear or damage",
            "Consider shutting off water supply to affected area if leak is severe",
            "Schedule professional inspection to identify hidden leaks or pipe issues"
        )
        
        return AIDiagnosisResult(
            issueType = "Plumbing",
            description = descriptions[Random.nextInt(descriptions.size)],
            possibleSolutions = solutions.shuffled().take(3)
        )
    }
    
    private fun generateElectricalDiagnosis(title: String, desc: String): AIDiagnosisResult {
        val descriptions = listOf(
            "This electrical issue may involve power supply problems, faulty wiring, or malfunctioning outlets. Electrical problems require careful diagnosis to ensure safety.",
            "Based on the symptoms, this appears to be an electrical concern that could involve circuit overload, wiring issues, or component failure. Professional evaluation is essential.",
            "The described electrical problem likely relates to power distribution, connections, or device functionality. Safety should be the primary concern when addressing electrical issues."
        )
        
        val solutions = listOf(
            "Check circuit breaker panel for tripped breakers - reset if safe to do so",
            "Test outlets with a voltage tester to identify dead circuits",
            "Inspect visible wiring for damage, fraying, or signs of overheating",
            "Unplug devices from affected circuits to rule out overload",
            "Check GFCI outlets - press reset button if present",
            "Contact licensed electrician for comprehensive inspection and repair"
        )
        
        return AIDiagnosisResult(
            issueType = "Electrical",
            description = descriptions[Random.nextInt(descriptions.size)],
            possibleSolutions = solutions.shuffled().take(3)
        )
    }
    
    private fun generateHVACDiagnosis(title: String, desc: String): AIDiagnosisResult {
        val descriptions = listOf(
            "This HVAC issue likely involves heating, cooling, or air circulation problems. Common causes include filter blockages, thermostat malfunctions, or system component failures.",
            "The described problem suggests an HVAC system concern affecting temperature control or air quality. Regular maintenance and proper diagnosis are key to resolution.",
            "Based on the symptoms, this HVAC issue may involve airflow restrictions, refrigerant problems, or control system malfunctions. Professional service is typically required."
        )
        
        val solutions = listOf(
            "Replace or clean air filters - dirty filters restrict airflow and reduce efficiency",
            "Check thermostat settings and batteries - ensure it's set to correct mode and temperature",
            "Inspect air vents and registers - ensure they're open and unobstructed",
            "Check outdoor unit for debris, ice buildup, or damage",
            "Verify circuit breakers for HVAC system are not tripped",
            "Schedule professional HVAC service for system inspection and maintenance"
        )
        
        return AIDiagnosisResult(
            issueType = "HVAC",
            description = descriptions[Random.nextInt(descriptions.size)],
            possibleSolutions = solutions.shuffled().take(3)
        )
    }
    
    private fun generateApplianceDiagnosis(title: String, desc: String): AIDiagnosisResult {
        val descriptions = listOf(
            "This appliance issue may involve mechanical failure, electrical problems, or operational malfunctions. Appliance repairs often require specific expertise and parts.",
            "Based on the description, this appliance problem likely relates to functionality, power supply, or component wear. Professional diagnosis can identify the specific issue.",
            "The described appliance concern suggests a malfunction that may require repair or replacement of components. Proper diagnosis is essential before attempting repairs."
        )
        
        val solutions = listOf(
            "Check power supply - ensure appliance is plugged in and outlet is functioning",
            "Review user manual for troubleshooting steps specific to the appliance model",
            "Inspect for visible damage, leaks, or unusual sounds during operation",
            "Clean filters, vents, and accessible components as recommended by manufacturer",
            "Check for error codes or indicator lights that may provide diagnostic information",
            "Contact appliance repair service or manufacturer support for professional assistance"
        )
        
        return AIDiagnosisResult(
            issueType = "Appliance",
            description = descriptions[Random.nextInt(descriptions.size)],
            possibleSolutions = solutions.shuffled().take(3)
        )
    }
    
    private fun generateGenericDiagnosis(title: String, desc: String, category: String): AIDiagnosisResult {
        val descriptions = listOf(
            "Based on the provided information, this maintenance issue requires professional assessment to determine the root cause and appropriate solution.",
            "The described problem suggests a maintenance concern that would benefit from expert evaluation to ensure proper diagnosis and repair.",
            "This issue appears to require professional inspection to identify the specific cause and determine the best course of action."
        )
        
        val solutions = listOf(
            "Document the issue with photos and detailed notes for contractor reference",
            "Check if issue is covered under warranty or service agreement",
            "Research similar issues online for potential DIY solutions if safe",
            "Gather information about when the problem started and any recent changes",
            "Consider urgency - address immediately if safety concern, otherwise schedule inspection",
            "Contact qualified contractor specializing in $category for professional assessment"
        )
        
        return AIDiagnosisResult(
            issueType = category.replaceFirstChar { it.uppercaseChar() },
            description = descriptions[Random.nextInt(descriptions.size)],
            possibleSolutions = solutions.shuffled().take(3)
        )
    }
}

