// Mock AI Diagnosis Service - No API key required

interface AIDiagnosisResult {
  issueType: string
  description: string
  possibleSolutions: string[]
}

function getRandomItem<T>(items: T[]): T {
  return items[Math.floor(Math.random() * items.length)]
}

function shuffleArray<T>(array: T[]): T[] {
  const shuffled = [...array]
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]]
  }
  return shuffled
}

export function generateMockAIDiagnosis(
  ticketTitle: string,
  ticketDescription: string,
  category: string
): AIDiagnosisResult {
  const lowerTitle = ticketTitle.toLowerCase()
  const lowerDesc = ticketDescription.toLowerCase()
  const lowerCategory = category.toLowerCase()

  // Determine issue type and generate diagnosis based on category/keywords
  if (
    lowerCategory.includes('plumbing') || lowerTitle.includes('leak') ||
    lowerTitle.includes('water') || lowerDesc.includes('leak') ||
    lowerDesc.includes('water') || lowerDesc.includes('pipe') ||
    lowerDesc.includes('faucet') || lowerDesc.includes('drain')
  ) {
    return generatePlumbingDiagnosis(lowerTitle, lowerDesc)
  }

  if (
    lowerCategory.includes('electrical') || lowerTitle.includes('electrical') ||
    lowerDesc.includes('electrical') || lowerDesc.includes('outlet') ||
    lowerDesc.includes('light') || lowerDesc.includes('power') ||
    lowerDesc.includes('circuit') || lowerDesc.includes('fuse')
  ) {
    return generateElectricalDiagnosis(lowerTitle, lowerDesc)
  }

  if (
    lowerCategory.includes('hvac') || lowerTitle.includes('hvac') ||
    lowerTitle.includes('heating') || lowerTitle.includes('cooling') ||
    lowerTitle.includes('air') || lowerDesc.includes('hvac') ||
    lowerDesc.includes('heating') || lowerDesc.includes('cooling') ||
    lowerDesc.includes('thermostat') || lowerDesc.includes('furnace') ||
    lowerDesc.includes('ac') || lowerDesc.includes('air conditioning')
  ) {
    return generateHVACDiagnosis(lowerTitle, lowerDesc)
  }

  if (
    lowerCategory.includes('appliance') || lowerTitle.includes('appliance') ||
    lowerDesc.includes('appliance') || lowerDesc.includes('refrigerator') ||
    lowerDesc.includes('dishwasher') || lowerDesc.includes('washer') ||
    lowerDesc.includes('dryer') || lowerDesc.includes('oven') ||
    lowerDesc.includes('stove') || lowerDesc.includes('microwave')
  ) {
    return generateApplianceDiagnosis(lowerTitle, lowerDesc)
  }

  return generateGenericDiagnosis(lowerTitle, lowerDesc, category)
}

function generatePlumbingDiagnosis(title: string, desc: string): AIDiagnosisResult {
  const descriptions = [
    "Based on the description, this appears to be a plumbing issue involving water flow or leakage. Common causes include worn seals, pipe corrosion, or blockages in the drainage system.",
    "The issue described suggests a plumbing problem that may involve water pressure, leaks, or drainage. This typically requires inspection of pipes, fixtures, and connections.",
    "This plumbing concern likely involves water-related problems such as leaks, clogs, or fixture malfunctions. Professional assessment is recommended to identify the root cause."
  ]

  const solutions = [
    "Inspect all visible pipes and connections for leaks or signs of water damage",
    "Check water pressure at affected fixtures - low pressure may indicate a blockage",
    "Test drain functionality - slow drains suggest clogs that may need professional clearing",
    "Examine seals and gaskets on faucets and fixtures for wear or damage",
    "Consider shutting off water supply to affected area if leak is severe",
    "Schedule professional inspection to identify hidden leaks or pipe issues"
  ]

  return {
    issueType: "Plumbing",
    description: getRandomItem(descriptions),
    possibleSolutions: shuffleArray(solutions).slice(0, 3)
  }
}

function generateElectricalDiagnosis(title: string, desc: string): AIDiagnosisResult {
  const descriptions = [
    "This electrical issue may involve power supply problems, faulty wiring, or malfunctioning outlets. Electrical problems require careful diagnosis to ensure safety.",
    "Based on the symptoms, this appears to be an electrical concern that could involve circuit overload, wiring issues, or component failure. Professional evaluation is essential.",
    "The described electrical problem likely relates to power distribution, connections, or device functionality. Safety should be the primary concern when addressing electrical issues."
  ]

  const solutions = [
    "Check circuit breaker panel for tripped breakers - reset if safe to do so",
    "Test outlets with a voltage tester to identify dead circuits",
    "Inspect visible wiring for damage, fraying, or signs of overheating",
    "Unplug devices from affected circuits to rule out overload",
    "Check GFCI outlets - press reset button if present",
    "Contact licensed electrician for comprehensive inspection and repair"
  ]

  return {
    issueType: "Electrical",
    description: getRandomItem(descriptions),
    possibleSolutions: shuffleArray(solutions).slice(0, 3)
  }
}

function generateHVACDiagnosis(title: string, desc: string): AIDiagnosisResult {
  const descriptions = [
    "This HVAC issue likely involves heating, cooling, or air circulation problems. Common causes include filter blockages, thermostat malfunctions, or system component failures.",
    "The described problem suggests an HVAC system concern affecting temperature control or air quality. Regular maintenance and proper diagnosis are key to resolution.",
    "Based on the symptoms, this HVAC issue may involve airflow restrictions, refrigerant problems, or control system malfunctions. Professional service is typically required."
  ]

  const solutions = [
    "Replace or clean air filters - dirty filters restrict airflow and reduce efficiency",
    "Check thermostat settings and batteries - ensure it's set to correct mode and temperature",
    "Inspect air vents and registers - ensure they're open and unobstructed",
    "Check outdoor unit for debris, ice buildup, or damage",
    "Verify circuit breakers for HVAC system are not tripped",
    "Schedule professional HVAC service for system inspection and maintenance"
  ]

  return {
    issueType: "HVAC",
    description: getRandomItem(descriptions),
    possibleSolutions: shuffleArray(solutions).slice(0, 3)
  }
}

function generateApplianceDiagnosis(title: string, desc: string): AIDiagnosisResult {
  const descriptions = [
    "This appliance issue may involve mechanical failure, electrical problems, or operational malfunctions. Appliance repairs often require specific expertise and parts.",
    "Based on the description, this appliance problem likely relates to functionality, power supply, or component wear. Professional diagnosis can identify the specific issue.",
    "The described appliance concern suggests a malfunction that may require repair or replacement of components. Proper diagnosis is essential before attempting repairs."
  ]

  const solutions = [
    "Check power supply - ensure appliance is plugged in and outlet is functioning",
    "Review user manual for troubleshooting steps specific to the appliance model",
    "Inspect for visible damage, leaks, or unusual sounds during operation",
    "Clean filters, vents, and accessible components as recommended by manufacturer",
    "Check for error codes or indicator lights that may provide diagnostic information",
    "Contact appliance repair service or manufacturer support for professional assistance"
  ]

  return {
    issueType: "Appliance",
    description: getRandomItem(descriptions),
    possibleSolutions: shuffleArray(solutions).slice(0, 3)
  }
}

function generateGenericDiagnosis(title: string, desc: string, category: string): AIDiagnosisResult {
  const descriptions = [
    "Based on the provided information, this maintenance issue requires professional assessment to determine the root cause and appropriate solution.",
    "The described problem suggests a maintenance concern that would benefit from expert evaluation to ensure proper diagnosis and repair.",
    "This issue appears to require professional inspection to identify the specific cause and determine the best course of action."
  ]

  const solutions = [
    "Document the issue with photos and detailed notes for contractor reference",
    "Check if issue is covered under warranty or service agreement",
    "Research similar issues online for potential DIY solutions if safe",
    "Gather information about when the problem started and any recent changes",
    "Consider urgency - address immediately if safety concern, otherwise schedule inspection",
    `Contact qualified contractor specializing in ${category} for professional assessment`
  ]

  return {
    issueType: category.charAt(0).toUpperCase() + category.slice(1),
    description: getRandomItem(descriptions),
    possibleSolutions: shuffleArray(solutions).slice(0, 3)
  }
}

