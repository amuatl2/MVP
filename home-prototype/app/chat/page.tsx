'use client'

import React from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from '@/context/AuthContext'
import Navigation from '@/components/Navigation'
import ChatInterface from '@/components/ChatInterface'

export default function ChatPage() {
  const router = useRouter()
  const { user } = useAuth()

  if (!user) {
    router.push('/login')
    return null
  }

  return (
    <div className="min-h-screen bg-lightGray pt-16 pb-20">
      <Navigation />
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-2xl font-bold text-darkGray mb-4">AI Chat Assistant</h2>
          <p className="text-gray-600 mb-6">
            Chat with our AI assistant for help with maintenance questions, ticket tracking, 
            contractor recommendations, and more.
          </p>
          <div className="h-[600px]">
            <ChatInterface />
          </div>
        </div>
      </div>
    </div>
  )
}

