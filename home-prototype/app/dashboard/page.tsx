'use client'

import React from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from '@/context/AuthContext'
import { useData } from '@/context/DataContext'
import Navigation from '@/components/Navigation'
import Link from 'next/link'
import { FiPlus, FiClock, FiCheckCircle, FiAlertCircle } from 'react-icons/fi'

export default function DashboardPage() {
  const router = useRouter()
  const { user } = useAuth()
  const { tickets, jobs } = useData()

  if (!user) {
    router.push('/login')
    return null
  }

  if (user.role === 'tenant') {
    return <TenantDashboard />
  } else if (user.role === 'landlord') {
    return <LandlordDashboard />
  } else {
    return <ContractorDashboard />
  }
}

function TenantDashboard() {
  const { tickets } = useData()
  const myTickets = tickets.filter(t => t.submittedBy === 'tenant@example.com')

  return (
    <div className="min-h-screen bg-lightGray pt-16 pb-20">
      <Navigation />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-darkGray mb-2">Tenant Dashboard</h1>
          <p className="text-gray-600">Manage your maintenance requests</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
          <Link
            href="/ticket/create"
            className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow cursor-pointer"
          >
            <div className="flex items-center space-x-4">
              <div className="bg-primary/10 p-3 rounded-lg">
                <FiPlus className="w-6 h-6 text-primary" />
              </div>
              <div>
                <h3 className="text-xl font-semibold text-darkGray">Report Issue</h3>
                <p className="text-gray-600">Create a new maintenance ticket</p>
              </div>
            </div>
          </Link>

          <div className="bg-white rounded-lg shadow-md p-6">
            <h3 className="text-lg font-semibold text-darkGray mb-4">My Tickets</h3>
            <div className="space-y-3">
              {myTickets.slice(0, 3).map((ticket) => (
                <Link
                  key={ticket.id}
                  href={`/ticket/${ticket.id}`}
                  className="block p-3 border border-lightGray rounded-lg hover:bg-lightGray transition-colors"
                >
                  <div className="flex justify-between items-start">
                    <div>
                      <p className="font-medium text-darkGray">{ticket.title}</p>
                      <p className="text-sm text-gray-600">{ticket.category}</p>
                    </div>
                    <span className={`text-xs px-2 py-1 rounded ${
                      ticket.status === 'completed' ? 'bg-green-100 text-green-800' :
                      ticket.status === 'assigned' ? 'bg-blue-100 text-blue-800' :
                      'bg-yellow-100 text-yellow-800'
                    }`}>
                      {ticket.status}
                    </span>
                  </div>
                </Link>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

function LandlordDashboard() {
  const { tickets } = useData()
  const openTickets = tickets.filter(t => t.status !== 'completed')

  return (
    <div className="min-h-screen bg-lightGray pt-16 pb-20">
      <Navigation />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-darkGray mb-2">Landlord Dashboard</h1>
          <p className="text-gray-600">Manage properties and maintenance</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div className="bg-white rounded-lg shadow-md p-6">
            <div className="flex items-center space-x-3 mb-4">
              <FiAlertCircle className="w-6 h-6 text-yellow-500" />
              <h3 className="text-lg font-semibold text-darkGray">Open Tickets</h3>
            </div>
            <p className="text-3xl font-bold text-darkGray">{openTickets.length}</p>
          </div>

          <Link
            href="/ticket/ai-diagnosis"
            className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow cursor-pointer"
          >
            <div className="flex items-center space-x-3 mb-4">
              <FiCheckCircle className="w-6 h-6 text-primary" />
              <h3 className="text-lg font-semibold text-darkGray">AI Diagnosis</h3>
            </div>
            <p className="text-gray-600">Review AI suggestions</p>
          </Link>

          <Link
            href="/marketplace"
            className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow cursor-pointer"
          >
            <div className="flex items-center space-x-3 mb-4">
              <FiPlus className="w-6 h-6 text-primary" />
              <h3 className="text-lg font-semibold text-darkGray">Assign Contractor</h3>
            </div>
            <p className="text-gray-600">Browse marketplace</p>
          </Link>
        </div>

        <div className="bg-white rounded-lg shadow-md p-6">
          <h3 className="text-lg font-semibold text-darkGray mb-4">Recent Tickets</h3>
          <div className="space-y-3">
            {openTickets.map((ticket) => (
              <Link
                key={ticket.id}
                href={`/ticket/${ticket.id}`}
                className="block p-4 border border-lightGray rounded-lg hover:bg-lightGray transition-colors"
              >
                <div className="flex justify-between items-start">
                  <div>
                    <p className="font-medium text-darkGray">{ticket.title}</p>
                    <p className="text-sm text-gray-600">{ticket.description}</p>
                    {ticket.aiDiagnosis && (
                      <span className="inline-block mt-2 text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded">
                        AI: {ticket.aiDiagnosis}
                      </span>
                    )}
                  </div>
                  <span className={`text-xs px-2 py-1 rounded ${
                    ticket.status === 'assigned' ? 'bg-blue-100 text-blue-800' :
                    'bg-yellow-100 text-yellow-800'
                  }`}>
                    {ticket.status}
                  </span>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

function ContractorDashboard() {
  const { jobs } = useData()
  const myJobs = jobs.filter(j => j.status !== 'completed')
  const completedJobs = jobs.filter(j => j.status === 'completed')

  return (
    <div className="min-h-screen bg-lightGray pt-16 pb-20">
      <Navigation />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-darkGray mb-2">Contractor Dashboard</h1>
          <p className="text-gray-600">Manage your jobs</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div className="bg-white rounded-lg shadow-md p-6">
            <h3 className="text-lg font-semibold text-darkGray mb-4">Assigned Jobs</h3>
            <p className="text-3xl font-bold text-primary">{myJobs.length}</p>
          </div>
          <div className="bg-white rounded-lg shadow-md p-6">
            <h3 className="text-lg font-semibold text-darkGray mb-4">Completed Jobs</h3>
            <p className="text-3xl font-bold text-green-600">{completedJobs.length}</p>
          </div>
          <div className="bg-white rounded-lg shadow-md p-6">
            <h3 className="text-lg font-semibold text-darkGray mb-4">Performance</h3>
            <p className="text-3xl font-bold text-darkGray">4.8★</p>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-md p-6">
          <h3 className="text-lg font-semibold text-darkGray mb-4">My Jobs</h3>
          <div className="space-y-3">
            {myJobs.map((job) => (
              <Link
                key={job.id}
                href={`/job/${job.id}`}
                className="block p-4 border border-lightGray rounded-lg hover:bg-lightGray transition-colors"
              >
                <div className="flex justify-between items-start">
                  <div>
                    <p className="font-medium text-darkGray">{job.issueType}</p>
                    <p className="text-sm text-gray-600">{job.propertyAddress}</p>
                    <p className="text-sm text-gray-500">Date: {job.date}</p>
                  </div>
                  <span className="text-xs px-2 py-1 rounded bg-blue-100 text-blue-800">
                    {job.status}
                  </span>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

