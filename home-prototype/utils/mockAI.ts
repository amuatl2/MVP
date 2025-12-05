// Mock AI Service for Demo - No API key required

function getRandomResponse(responses: string[]): string {
  return responses[Math.floor(Math.random() * responses.length)]
}

export function generateMockAIResponse(userMessage: string): string {
  const lowerMessage = userMessage.toLowerCase().trim()
  
  // Maintenance/Issues
  if (lowerMessage.includes('maintenance') || lowerMessage.includes('issue') || 
      lowerMessage.includes('problem') || lowerMessage.includes('broken') ||
      lowerMessage.includes('leak') || lowerMessage.includes('not working')) {
    return getRandomResponse([
      "I can help you with maintenance issues! You can create a ticket through the 'Create Ticket' tab. Describe your issue, select a category (Plumbing, Electrical, HVAC, or Appliance), and I'll help diagnose it. What type of issue are you facing?",
      "For maintenance issues, I recommend creating a ticket right away. Go to the Create Ticket section, provide details about the problem, and select the appropriate category. Would you like help identifying what category your issue falls under?",
      "I'm here to help with maintenance! The quickest way to get help is to create a ticket. You can include photos, describe the issue, and our system will help match you with qualified contractors. What's the problem you're experiencing?"
    ])
  }
  
  // Tickets/Status
  if (lowerMessage.includes('ticket') || lowerMessage.includes('status') || 
      lowerMessage.includes('update') || lowerMessage.includes('check')) {
    return getRandomResponse([
      "To check your ticket status, go to your Dashboard and tap on any ticket card. You'll see a status tracker showing: Submitted → Assigned → Scheduled → Completed. You can also view all tickets in the History tab. Need help with a specific ticket?",
      "Your tickets are tracked through several stages: Submitted, Assigned, Scheduled, and Completed. You can view all your tickets in the Dashboard or History sections. Is there a particular ticket you'd like to check on?",
      "I can help you track your tickets! All your maintenance requests are visible in the Dashboard. Each ticket shows its current status and progress. Would you like to know more about a specific ticket?"
    ])
  }
  
  // Contractors
  if (lowerMessage.includes('contractor') || lowerMessage.includes('find') || 
      lowerMessage.includes('recommend') || lowerMessage.includes('assign') ||
      lowerMessage.includes('plumber') || lowerMessage.includes('electrician')) {
    return getRandomResponse([
      "To find contractors, navigate to the Marketplace tab. You can filter by category, distance, ratings, and preferred status. I recommend checking contractors with 4.5+ star ratings for the best service. Would you like help finding a contractor for a specific issue?",
      "The Marketplace is your go-to place for finding qualified contractors. You can browse by service type, see ratings and reviews, and check availability. Contractors with preferred status have been verified and highly rated. What type of contractor are you looking for?",
      "Finding the right contractor is easy! Use the Marketplace to search by category, location, and ratings. I suggest looking for contractors with high ratings (4+ stars) and good reviews. Need help finding someone for a specific job?"
    ])
  }
  
  // Scheduling
  if (lowerMessage.includes('schedule') || lowerMessage.includes('appointment') || 
      lowerMessage.includes('visit') || lowerMessage.includes('when') ||
      lowerMessage.includes('time') || lowerMessage.includes('available')) {
    return getRandomResponse([
      "You can schedule appointments through the Schedule tab. Select an available date from the calendar, choose a time slot that works for you, and confirm your selection. The system will automatically notify all parties. Ready to schedule?",
      "Scheduling is simple! Go to the Schedule section, pick a date from the calendar, and select your preferred time slot. The contractor and landlord will be notified automatically. What date works best for you?",
      "I can help you schedule! Use the Schedule tab to view available dates and times. Once you select a slot, everyone involved will be notified. Would you like to schedule something now?"
    ])
  }
  
  // Ratings/Reviews
  if (lowerMessage.includes('rating') || lowerMessage.includes('review') || 
      lowerMessage.includes('feedback') || lowerMessage.includes('rate')) {
    return getRandomResponse([
      "After a job is completed, you can rate the contractor through the Rating tab. Your feedback helps improve our service and helps other users find reliable contractors. Ratings are based on a 5-star system. Have you completed a job recently?",
      "Rating contractors is important! After a job is finished, go to the Rating section to leave feedback. Your reviews help other users make informed decisions. Have you had a recent service completed?",
      "You can rate contractors after job completion in the Rating section. Share your experience with a 5-star rating and optional comments. This helps build trust in our community. Is there a recent job you'd like to rate?"
    ])
  }
  
  // Greetings
  if (lowerMessage.includes('hello') || lowerMessage.includes('hi') || 
      lowerMessage.includes('hey') || lowerMessage.includes('good morning') ||
      lowerMessage.includes('good afternoon') || lowerMessage.includes('good evening')) {
    return getRandomResponse([
      "Hello! I'm here to help you with HOME platform questions. You can ask me about creating tickets, finding contractors, scheduling appointments, rating contractors, viewing history, or anything else related to property maintenance!",
      "Hi there! I'm your HOME AI Assistant. I can help with ticket management, contractor recommendations, scheduling, and navigating the platform. What would you like to know?",
      "Hey! Welcome to HOME. I'm here to assist with maintenance requests, contractor matching, scheduling, and more. How can I help you today?"
    ])
  }
  
  // Help/What can you do
  if (lowerMessage.includes('help') || lowerMessage.includes('what can you do') ||
      lowerMessage.includes('how') || (lowerMessage.includes('what') && lowerMessage.length < 20)) {
    return getRandomResponse([
      "I can help with: ✓ Creating maintenance tickets\n✓ Tracking ticket status\n✓ Finding and recommending contractors\n✓ Scheduling appointments\n✓ Rating contractors\n✓ Viewing maintenance history\n✓ Navigating the HOME platform\n\nWhat would you like to know?",
      "I'm your HOME assistant! I can help you:\n• Create and track maintenance tickets\n• Find qualified contractors\n• Schedule service appointments\n• Rate completed jobs\n• Navigate the app features\n\nWhat do you need help with?",
      "I'm here to assist with:\n- Ticket creation and tracking\n- Contractor search and recommendations\n- Appointment scheduling\n- Job ratings and reviews\n- Platform navigation\n\nHow can I assist you?"
    ])
  }
  
  // Emergency/Urgent
  if (lowerMessage.includes('emergency') || lowerMessage.includes('urgent') ||
      lowerMessage.includes('asap') || lowerMessage.includes('immediately')) {
    return "For urgent maintenance issues, I recommend creating a ticket immediately and marking it as urgent. You can also contact your landlord directly through the Messages section. What's the emergency situation?"
  }
  
  // Thank you
  if (lowerMessage.includes('thank') || lowerMessage.includes('thanks')) {
    return getRandomResponse([
      "You're welcome! I'm always here to help. Feel free to ask if you need anything else!",
      "Happy to help! If you have any other questions, just let me know.",
      "You're welcome! Don't hesitate to reach out if you need assistance with anything else."
    ])
  }
  
  // Default responses
  return getRandomResponse([
    "Thank you for your message! As the HOME AI Assistant, I specialize in helping with ticket management, contractor selection, scheduling, and platform navigation. Feel free to ask specific questions about maintenance issues, ticket status, contractor recommendations, or how to use any feature of the app.",
    "I understand! I'm here to help with HOME platform features. You can ask me about creating tickets, finding contractors, scheduling appointments, or navigating the app. What specific help do you need?",
    "I'm here to assist with property maintenance and the HOME platform. Try asking me about:\n• Creating a maintenance ticket\n• Finding contractors\n• Checking ticket status\n• Scheduling appointments\n\nWhat would you like to know?",
    "That's a great question! I can help you with maintenance requests, contractor matching, scheduling, and more. Could you provide a bit more detail about what you're looking for?",
    "I'm your HOME assistant, ready to help! I can assist with tickets, contractors, scheduling, and navigating the platform. What specific information are you looking for?"
  ])
}

export function getTypingDelay(response: string): number {
  const baseDelay = 800
  const charDelay = response.length * 20
  const randomVariation = Math.random() * 500 + 300
  return Math.min(Math.max(baseDelay + charDelay + randomVariation, 800), 3000)
}

