'use client'

import React from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from '@/context/AuthContext'
import { useData } from '@/context/DataContext'
import Navigation from '@/components/Navigation'
import Link from 'next/link'
import { FiArrowLeft, FiCheckCircle } from 'react-icons/fi'

export default function AIDiagnosisPage() {
  const router = useRouter()
  const { user } = useAuth()
  const { tickets } = useData()

  if (!user || user.role !== 'landlord') {
    router.push('/dashboard')
    return null
  }

  const ticketsWithAI = tickets.filter(t => t.aiDiagnosis && t.status === 'submitted')

  return (
    <div className="min-h-screen bg-lightGray pt-16">
      <Navigation />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <button
          onClick={() => router.back()}
          className="mb-4 flex items-center space-x-2 text-darkGray hover:text-primary"
        >
          <FiArrowLeft className="w-4 h-4" />
          <span>Back</span>
        </button>

        <div className="mb-6">
          <h1 className="text-3xl font-bold text-darkGray mb-2">AI Diagnosis</h1>
          <p className="text-gray-600">Review AI-suggested diagnoses for open tickets</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {ticketsWithAI.map((ticket) => (
            <div key={ticket.id} className="bg-white rounded-lg shadow-md p-6">
              <div className="mb-4">
                <h3 className="text-lg font-semibold text-darkGray mb-2">{ticket.title}</h3>
                <p className="text-sm text-gray-600">{ticket.description}</p>
              </div>

              <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg mb-4">
                <div className="flex items-center space-x-2 mb-2">
                  <span className="text-2xl">🤖</span>
                  <h4 className="font-semibold text-blue-900">AI Diagnosis</h4>
                </div>
                <p className="text-blue-800">{ticket.aiDiagnosis}</p>
              </div>

              <div className="flex space-x-3">
                <button className="flex-1 px-4 py-2 bg-green-600 text-white rounded-lg font-medium hover:bg-green-700 transition-colors flex items-center justify-center space-x-2">
                  <FiCheckCircle className="w-4 h-4" />
                  <span>Accept Suggestion</span>
                </button>
                <Link
                  href={`/marketplace?ticket=${ticket.id}`}
                  className="flex-1 text-center px-4 py-2 bg-primary text-white rounded-lg font-medium hover:bg-blue-600 transition-colors"
                >
                  Assign Contractor
                </Link>
              </div>
            </div>
          ))}

          {ticketsWithAI.length === 0 && (
            <div className="col-span-2 bg-white rounded-lg shadow-md p-8 text-center">
              <p className="text-gray-600">No tickets with AI diagnoses available</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

