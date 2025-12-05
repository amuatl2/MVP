'use client'

import React, { useMemo } from 'react'
import { useRouter } from 'next/navigation'
import { useParams } from 'next/navigation'
import { useAuth } from '@/context/AuthContext'
import { useData } from '@/context/DataContext'
import Navigation from '@/components/Navigation'
import Link from 'next/link'
import { FiArrowLeft, FiClock, FiUser, FiCheckCircle } from 'react-icons/fi'
import { generateMockAIDiagnosis } from '@/utils/mockAIDiagnosis'

export default function TicketDetailPage() {
  const router = useRouter()
  const params = useParams()
  const { user } = useAuth()
  const { tickets, contractors } = useData()
  
  const ticket = tickets.find(t => t.id === params.id)

  if (!user) {
    router.push('/login')
    return null
  }

  if (!ticket) {
    return (
      <div className="min-h-screen bg-lightGray pt-16">
        <Navigation />
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="bg-white rounded-lg shadow-md p-6 text-center">
            <p className="text-gray-600">Ticket not found</p>
            <Link href="/dashboard" className="text-primary hover:underline mt-4 inline-block">
              Return to Dashboard
            </Link>
          </div>
        </div>
      </div>
    )
  }

  const assignedContractor = ticket.assignedTo 
    ? contractors.find(c => c.id === ticket.assignedTo)
    : null

  const statusSteps = [
    { key: 'submitted', label: 'Submitted' },
    { key: 'assigned', label: 'Assigned' },
    { key: 'scheduled', label: 'Scheduled' },
    { key: 'completed', label: 'Completed' },
  ]

  const currentStatusIndex = statusSteps.findIndex(s => s.key === ticket.status)

  const enhancedDiagnosis = useMemo(() => {
    if (!ticket.aiDiagnosis) return null
    return generateMockAIDiagnosis(
      ticket.title,
      ticket.description,
      ticket.category
    )
  }, [ticket.aiDiagnosis, ticket.title, ticket.description, ticket.category])

  return (
    <div className="min-h-screen bg-lightGray pt-16">
      <Navigation />
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <button
          onClick={() => router.back()}
          className="mb-4 flex items-center space-x-2 text-darkGray hover:text-primary"
        >
          <FiArrowLeft className="w-4 h-4" />
          <span>Back</span>
        </button>

        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="flex justify-between items-start mb-4">
            <div>
              <h1 className="text-2xl font-bold text-darkGray mb-2">{ticket.title}</h1>
              <div className="flex items-center space-x-4 text-sm text-gray-600">
                <span className="flex items-center space-x-1">
                  <FiClock className="w-4 h-4" />
                  <span>{new Date(ticket.createdAt).toLocaleDateString()}</span>
                </span>
                <span className="px-2 py-1 bg-blue-100 text-blue-800 rounded text-xs">
                  {ticket.category}
                </span>
              </div>
            </div>
            <span className={`px-3 py-1 rounded text-sm font-medium ${
              ticket.status === 'completed' ? 'bg-green-100 text-green-800' :
              ticket.status === 'assigned' ? 'bg-blue-100 text-blue-800' :
              ticket.status === 'scheduled' ? 'bg-yellow-100 text-yellow-800' :
              'bg-gray-100 text-gray-800'
            }`}>
              {ticket.status}
            </span>
          </div>

          <div className="mb-6">
            <h3 className="font-semibold text-darkGray mb-2">Description</h3>
            <p className="text-gray-700">{ticket.description}</p>
          </div>

          {enhancedDiagnosis && (
            <div className="mb-6 p-6 bg-blue-50 border border-blue-200 rounded-lg">
              <h3 className="font-semibold text-darkGray mb-4 flex items-center space-x-2">
                <span className="text-2xl">🤖</span>
                <span className="text-lg">AI Diagnosis</span>
              </h3>
              
              <div className="space-y-4">
                {/* Issue Type */}
                <div className="flex items-center space-x-3">
                  <span className="font-semibold text-darkGray">Issue Type:</span>
                  <span className="px-3 py-1 bg-primary text-white rounded-md font-medium text-sm">
                    {enhancedDiagnosis.issueType}
                  </span>
                </div>
                
                <div className="border-t border-blue-300"></div>
                
                {/* Description */}
                <div>
                  <h4 className="font-semibold text-darkGray mb-2">Description:</h4>
                  <p className="text-gray-700 leading-relaxed">{enhancedDiagnosis.description}</p>
                </div>
                
                <div className="border-t border-blue-300"></div>
                
                {/* Possible Solutions */}
                <div>
                  <h4 className="font-semibold text-darkGray mb-3">Possible Solutions:</h4>
                  <ul className="space-y-2">
                    {enhancedDiagnosis.possibleSolutions.map((solution, index) => (
                      <li key={index} className="flex items-start space-x-3">
                        <span className="font-bold text-primary mt-0.5">{index + 1}.</span>
                        <span className="text-gray-700 flex-1">{solution}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            </div>
          )}

          {assignedContractor && (
            <div className="mb-6 p-4 bg-gray-50 rounded-lg">
              <h3 className="font-semibold text-darkGray mb-2 flex items-center space-x-2">
                <FiUser className="w-4 h-4" />
                <span>Assigned Contractor</span>
              </h3>
              <Link
                href={`/contractor/${assignedContractor.id}`}
                className="text-primary hover:underline"
              >
                {assignedContractor.company} - {assignedContractor.name}
              </Link>
            </div>
          )}

          <div className="mb-6">
            <h3 className="font-semibold text-darkGray mb-4">Status Tracker</h3>
            <div className="flex items-center space-x-4">
              {statusSteps.map((step, index) => (
                <React.Fragment key={step.key}>
                  <div className="flex flex-col items-center">
                    <div className={`w-10 h-10 rounded-full flex items-center justify-center ${
                      index <= currentStatusIndex
                        ? 'bg-primary text-white'
                        : 'bg-lightGray text-gray-400'
                    }`}>
                      {index < currentStatusIndex ? (
                        <FiCheckCircle className="w-6 h-6" />
                      ) : (
                        <span>{index + 1}</span>
                      )}
                    </div>
                    <span className={`text-xs mt-2 ${
                      index <= currentStatusIndex ? 'text-darkGray' : 'text-gray-400'
                    }`}>
                      {step.label}
                    </span>
                  </div>
                  {index < statusSteps.length - 1 && (
                    <div className={`h-1 w-16 ${
                      index < currentStatusIndex ? 'bg-primary' : 'bg-lightGray'
                    }`} />
                  )}
                </React.Fragment>
              ))}
            </div>
          </div>

          {user.role === 'landlord' && !ticket.assignedTo && (
            <Link
              href={`/marketplace?ticket=${ticket.id}`}
              className="block w-full text-center px-6 py-2 bg-primary text-white rounded-lg font-medium hover:bg-blue-600 transition-colors mb-4"
            >
              Assign Contractor
            </Link>
          )}

          <div className="border-t pt-6">
            <h3 className="font-semibold text-darkGray mb-4">Comments / Chat</h3>
            <div className="space-y-4">
              <div className="p-3 bg-lightGray rounded-lg">
                <p className="text-sm text-gray-600">Chat functionality placeholder</p>
                <p className="text-xs text-gray-500 mt-1">Messages will appear here</p>
              </div>
              <div className="flex space-x-2">
                <input
                  type="text"
                  placeholder="Type a message..."
                  className="flex-1 px-4 py-2 border border-lightGray rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                />
                <button className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-blue-600 transition-colors">
                  Send
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

