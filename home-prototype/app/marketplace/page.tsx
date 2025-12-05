'use client'

import React, { useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { useAuth } from '@/context/AuthContext'
import { useData } from '@/context/DataContext'
import Navigation from '@/components/Navigation'
import Link from 'next/link'
import { FiStar, FiMapPin, FiCheck, FiUsers } from 'react-icons/fi'

export default function MarketplacePage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { user } = useAuth()
  const { contractors, assignContractor } = useData()
  const [filterCategory, setFilterCategory] = useState('')
  const [filterDistance, setFilterDistance] = useState('')
  const ticketId = searchParams.get('ticket')

  if (!user) {
    router.push('/login')
    return null
  }

  const handleAssign = (contractorId: string) => {
    if (ticketId) {
      assignContractor(ticketId, contractorId)
      router.push(`/ticket/${ticketId}`)
    }
  }

  const handleAccept = (contractorId: string) => {
    // For contractor accepting a job
    router.push('/contractor-dashboard')
  }

  const filteredContractors = contractors.filter(c => {
    if (filterCategory && !c.specialization.includes(filterCategory)) return false
    if (filterDistance) {
      const maxDistance = parseInt(filterDistance)
      if (c.distance > maxDistance) return false
    }
    return true
  })

  return (
    <div className="min-h-screen bg-lightGray pt-16">
      <Navigation />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-darkGray mb-2">Contractor Marketplace</h1>
          <p className="text-gray-600">Browse and select contractors</p>
        </div>

        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-darkGray mb-2">
                Filter by Category
              </label>
              <select
                value={filterCategory}
                onChange={(e) => setFilterCategory(e.target.value)}
                className="w-full px-4 py-2 border border-lightGray rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
              >
                <option value="">All Categories</option>
                <option value="Plumbing">Plumbing</option>
                <option value="Electrical">Electrical</option>
                <option value="HVAC">HVAC</option>
                <option value="Appliance">Appliance</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-darkGray mb-2">
                Max Distance (miles)
              </label>
              <select
                value={filterDistance}
                onChange={(e) => setFilterDistance(e.target.value)}
                className="w-full px-4 py-2 border border-lightGray rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
              >
                <option value="">Any Distance</option>
                <option value="5">Within 5 miles</option>
                <option value="10">Within 10 miles</option>
                <option value="25">Within 25 miles</option>
              </select>
            </div>
            <div className="flex items-end">
              <button
                onClick={() => {
                  setFilterCategory('')
                  setFilterDistance('')
                }}
                className="w-full px-4 py-2 border border-lightGray rounded-lg text-darkGray hover:bg-lightGray transition-colors"
              >
                Clear Filters
              </button>
            </div>
          </div>
        </div>

        {user.role === 'landlord' && (
          <div className="mb-6">
            <button
              onClick={() => router.push('/marketplace/invite')}
              className="px-6 py-2 bg-primary text-white rounded-lg font-medium hover:bg-blue-600 transition-colors flex items-center space-x-2"
            >
              <FiUsers className="w-4 h-4" />
              <span>Invite Contractor</span>
            </button>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredContractors.map((contractor) => (
            <div key={contractor.id} className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow">
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-center space-x-3">
                  <div className="w-12 h-12 bg-primary/10 rounded-full flex items-center justify-center">
                    <span className="text-primary font-bold">
                      {contractor.name.charAt(0)}
                    </span>
                  </div>
                  <div>
                    <h3 className="font-semibold text-darkGray">{contractor.name}</h3>
                    <p className="text-sm text-gray-600">{contractor.company}</p>
                  </div>
                </div>
                {contractor.preferred && (
                  <span className="px-2 py-1 bg-yellow-100 text-yellow-800 text-xs rounded">
                    Preferred
                  </span>
                )}
              </div>

              <div className="mb-4">
                <div className="flex items-center space-x-2 mb-2">
                  <FiStar className="w-4 h-4 text-yellow-500 fill-current" />
                  <span className="font-medium text-darkGray">{contractor.rating}</span>
                  <span className="text-sm text-gray-600">
                    ({contractor.completedJobs} jobs)
                  </span>
                </div>
                <div className="flex items-center space-x-2 text-sm text-gray-600">
                  <FiMapPin className="w-4 h-4" />
                  <span>{contractor.distance} miles away</span>
                </div>
              </div>

              <div className="mb-4">
                <div className="flex flex-wrap gap-2">
                  {contractor.specialization.map((spec) => (
                    <span
                      key={spec}
                      className="px-2 py-1 bg-lightGray text-darkGray text-xs rounded"
                    >
                      {spec}
                    </span>
                  ))}
                </div>
              </div>

              <div className="flex space-x-2">
                <Link
                  href={`/contractor/${contractor.id}`}
                  className="flex-1 text-center px-4 py-2 border border-lightGray rounded-lg text-darkGray hover:bg-lightGray transition-colors"
                >
                  View Profile
                </Link>
                {user.role === 'landlord' && ticketId && (
                  <button
                    onClick={() => handleAssign(contractor.id)}
                    className="flex-1 px-4 py-2 bg-primary text-white rounded-lg hover:bg-blue-600 transition-colors"
                  >
                    Assign
                  </button>
                )}
                {user.role === 'contractor' && (
                  <button
                    onClick={() => handleAccept(contractor.id)}
                    className="flex-1 px-4 py-2 bg-primary text-white rounded-lg hover:bg-blue-600 transition-colors"
                  >
                    Accept Job
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

