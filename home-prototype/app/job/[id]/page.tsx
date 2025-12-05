'use client'

import React, { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useParams } from 'next/navigation'
import { useAuth } from '@/context/AuthContext'
import { useData } from '@/context/DataContext'
import Navigation from '@/components/Navigation'
import { FiArrowLeft, FiCheckCircle, FiMessageCircle } from 'react-icons/fi'

export default function JobDetailPage() {
  const router = useRouter()
  const params = useParams()
  const { user } = useAuth()
  const { jobs, tickets, completeJob } = useData()
  const [showConfirm, setShowConfirm] = useState(false)
  const [completed, setCompleted] = useState(false)
  
  const job = jobs.find(j => j.id === params.id)
  const ticket = job ? tickets.find(t => t.id === job.ticketId) : null

  if (!user) {
    router.push('/login')
    return null
  }

  if (!job || !ticket) {
    return (
      <div className="min-h-screen bg-lightGray pt-16">
        <Navigation />
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="bg-white rounded-lg shadow-md p-6 text-center">
            <p className="text-gray-600">Job not found</p>
            <button
              onClick={() => router.back()}
              className="text-primary hover:underline mt-4"
            >
              Go Back
            </button>
          </div>
        </div>
      </div>
    )
  }

  const handleComplete = () => {
    completeJob(job.id)
    setCompleted(true)
    setShowConfirm(false)
    setTimeout(() => {
      router.push('/rating?job=' + job.id)
    }, 2000)
  }

  if (completed) {
    return (
      <div className="min-h-screen bg-lightGray pt-16">
        <Navigation />
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="bg-white rounded-lg shadow-md p-8 text-center">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <FiCheckCircle className="w-8 h-8 text-green-600" />
            </div>
            <h2 className="text-2xl font-bold text-darkGray mb-2">Job Marked as Complete!</h2>
            <p className="text-gray-600">Redirecting to rating page...</p>
          </div>
        </div>
      </div>
    )
  }

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
          <h1 className="text-2xl font-bold text-darkGray mb-6">Job Details</h1>

          <div className="space-y-6">
            <div>
              <h3 className="font-semibold text-darkGray mb-2">Job Summary</h3>
              <p className="text-gray-700">{ticket.description}</p>
            </div>

            <div>
              <h3 className="font-semibold text-darkGray mb-2">Issue Type</h3>
              <span className="px-3 py-1 bg-blue-100 text-blue-800 rounded">
                {job.issueType}
              </span>
            </div>

            <div>
              <h3 className="font-semibold text-darkGray mb-2">Property Address</h3>
              <p className="text-gray-700">{job.propertyAddress}</p>
            </div>

            <div>
              <h3 className="font-semibold text-darkGray mb-2">Status</h3>
              <span className={`px-3 py-1 rounded ${
                job.status === 'completed' ? 'bg-green-100 text-green-800' :
                'bg-blue-100 text-blue-800'
              }`}>
                {job.status}
              </span>
            </div>

            {ticket.photos && ticket.photos.length > 0 && (
              <div>
                <h3 className="font-semibold text-darkGray mb-2">Photos</h3>
                <div className="grid grid-cols-2 gap-4">
                  {ticket.photos.map((photo, index) => (
                    <div key={index} className="bg-lightGray rounded-lg h-32 flex items-center justify-center">
                      <span className="text-gray-500 text-sm">Photo {index + 1}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <div className="border-t pt-6">
              <h3 className="font-semibold text-darkGray mb-4 flex items-center space-x-2">
                <FiMessageCircle className="w-4 h-4" />
                <span>Chat / Messages</span>
              </h3>
              <div className="space-y-4">
                <div className="p-3 bg-lightGray rounded-lg">
                  <p className="text-sm text-gray-600">Message placeholder</p>
                  <p className="text-xs text-gray-500 mt-1">Chat functionality will be available here</p>
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

            {job.status !== 'completed' && user.role === 'contractor' && (
              <div className="border-t pt-6">
                <button
                  onClick={() => setShowConfirm(true)}
                  className="w-full px-6 py-3 bg-green-600 text-white rounded-lg font-medium hover:bg-green-700 transition-colors flex items-center justify-center space-x-2"
                >
                  <FiCheckCircle className="w-5 h-5" />
                  <span>Mark as Complete</span>
                </button>
              </div>
            )}
          </div>
        </div>

        {showConfirm && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg p-6 max-w-md mx-4">
              <h3 className="text-xl font-bold text-darkGray mb-4">Confirm Completion</h3>
              <p className="text-gray-600 mb-6">
                Are you sure you want to mark this job as complete?
              </p>
              <div className="flex space-x-4">
                <button
                  onClick={() => setShowConfirm(false)}
                  className="flex-1 px-4 py-2 border border-lightGray rounded-lg text-darkGray hover:bg-lightGray transition-colors"
                >
                  Cancel
                </button>
                <button
                  onClick={handleComplete}
                  className="flex-1 px-4 py-2 bg-primary text-white rounded-lg hover:bg-blue-600 transition-colors"
                >
                  Confirm
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

