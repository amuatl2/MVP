'use client'

import React, { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from '@/context/AuthContext'
import { useData } from '@/context/DataContext'
import Navigation from '@/components/Navigation'
import Link from 'next/link'
import { FiClock, FiCheckCircle, FiBriefcase } from 'react-icons/fi'

export default function ContractorDashboardPage() {
  const router = useRouter()
  const { user } = useAuth()
  const { jobs, tickets } = useData()
  const [activeTab, setActiveTab] = useState<'assigned' | 'completed'>('assigned')

  if (!user || user.role !== 'contractor') {
    router.push('/login')
    return null
  }

  const assignedJobs = jobs.filter(j => j.status !== 'completed')
  const completedJobs = jobs.filter(j => j.status === 'completed')

  // Calculate rating from COMPLETED tickets that have been reviewed (have ratings)
  // Only count tickets that are completed AND have ratings
  // Find contractor ID from jobs assigned to this user
  const userJobs = jobs.filter(j => j.contractorId && j.contractorId.includes('contractor'))
  const contractorId = userJobs.length > 0 ? userJobs[0].contractorId : null
  
  // Get tickets assigned to this contractor (via jobs)
  const contractorTicketIds = jobs
    .filter(j => j.contractorId === contractorId)
    .map(j => j.ticketId)
  
  const contractorTickets = tickets.filter(t => 
    contractorTicketIds.includes(t.id)
  )
  
  // Get only COMPLETED tickets that have been reviewed (have ratings)
  const ratedCompletedTickets = contractorTickets.filter(t => 
    t.status === 'completed' && 
    t.rating != null && 
    t.rating > 0
  )
  
  const avgRating = ratedCompletedTickets.length > 0
    ? ratedCompletedTickets.reduce((sum, t) => sum + (t.rating || 0), 0) / ratedCompletedTickets.length
    : 0

  return (
    <div className="min-h-screen bg-lightGray pt-16 pb-20">
      <Navigation />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-darkGray mb-2">Contractor Dashboard</h1>
          <p className="text-gray-600">Manage your jobs and assignments</p>
        </div>

        {/* Rating Display */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-lg font-semibold text-darkGray mb-1">Your Rating</h3>
              <p className="text-sm text-gray-600">
                {ratedCompletedTickets.length > 0 
                  ? `Based on ${ratedCompletedTickets.length} completed ${ratedCompletedTickets.length === 1 ? 'job' : 'jobs'}`
                  : 'No completed jobs with ratings yet'}
              </p>
            </div>
            <div className="text-right">
              <div className="text-4xl font-bold text-primary">
                {avgRating > 0 ? avgRating.toFixed(1) : 'N/A'}
              </div>
              {avgRating > 0 && (
                <div className="flex items-center justify-end space-x-1 mt-1">
                  <span className="text-yellow-500 text-xl">★</span>
                  <span className="text-sm text-gray-600">({ratedCompletedTickets.length} reviews)</span>
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-md mb-6">
          <div className="flex border-b">
            <button
              onClick={() => setActiveTab('assigned')}
              className={`flex-1 px-6 py-4 font-medium transition-colors ${
                activeTab === 'assigned'
                  ? 'border-b-2 border-primary text-primary'
                  : 'text-gray-600 hover:text-darkGray'
              }`}
            >
              <div className="flex items-center justify-center space-x-2">
                <FiBriefcase className="w-4 h-4" />
                <span>Assigned Jobs ({assignedJobs.length})</span>
              </div>
            </button>
            <button
              onClick={() => setActiveTab('completed')}
              className={`flex-1 px-6 py-4 font-medium transition-colors ${
                activeTab === 'completed'
                  ? 'border-b-2 border-primary text-primary'
                  : 'text-gray-600 hover:text-darkGray'
              }`}
            >
              <div className="flex items-center justify-center space-x-2">
                <FiCheckCircle className="w-4 h-4" />
                <span>Completed Jobs ({completedJobs.length})</span>
              </div>
            </button>
          </div>

          <div className="p-6">
            {activeTab === 'assigned' ? (
              <div className="space-y-4">
                {assignedJobs.length > 0 ? (
                  assignedJobs.map((job) => (
                    <Link
                      key={job.id}
                      href={`/job/${job.id}`}
                      className="block p-4 border border-lightGray rounded-lg hover:bg-lightGray transition-colors"
                    >
                      <div className="flex justify-between items-start">
                        <div>
                          <p className="font-medium text-darkGray">{job.issueType}</p>
                          <p className="text-sm text-gray-600">{job.propertyAddress}</p>
                          <div className="flex items-center space-x-4 mt-2 text-sm text-gray-500">
                            <span className="flex items-center space-x-1">
                              <FiClock className="w-4 h-4" />
                              <span>Date: {job.date}</span>
                            </span>
                          </div>
                        </div>
                        <span className="px-3 py-1 bg-blue-100 text-blue-800 rounded text-sm">
                          {job.status}
                        </span>
                      </div>
                    </Link>
                  ))
                ) : (
                  <div className="text-center py-8">
                    <FiBriefcase className="w-12 h-12 text-gray-400 mx-auto mb-2" />
                    <p className="text-gray-600">No assigned jobs</p>
                  </div>
                )}
              </div>
            ) : (
              <div className="space-y-4">
                {completedJobs.length > 0 ? (
                  completedJobs.map((job) => (
                    <div
                      key={job.id}
                      className="p-4 border border-lightGray rounded-lg"
                    >
                      <div className="flex justify-between items-start">
                        <div>
                          <p className="font-medium text-darkGray">{job.issueType}</p>
                          <p className="text-sm text-gray-600">{job.propertyAddress}</p>
                          <p className="text-sm text-gray-500">Completed: {job.date}</p>
                        </div>
                        <span className="px-3 py-1 bg-green-100 text-green-800 rounded text-sm">
                          Completed
                        </span>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="text-center py-8">
                    <FiCheckCircle className="w-12 h-12 text-gray-400 mx-auto mb-2" />
                    <p className="text-gray-600">No completed jobs</p>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

