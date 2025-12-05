'use client'

import React from 'react'
import { useRouter } from 'next/navigation'
import { useParams } from 'next/navigation'
import { useAuth } from '@/context/AuthContext'
import { useData } from '@/context/DataContext'
import Navigation from '@/components/Navigation'
import { FiStar, FiMapPin, FiCheck } from 'react-icons/fi'

export default function ContractorProfilePage() {
  const router = useRouter()
  const params = useParams()
  const { user } = useAuth()
  const { contractors, jobs } = useData()
  
  const contractor = contractors.find(c => c.id === params.id)

  if (!user) {
    router.push('/login')
    return null
  }

  if (!contractor) {
    return (
      <div className="min-h-screen bg-lightGray pt-16">
        <Navigation />
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="bg-white rounded-lg shadow-md p-6 text-center">
            <p className="text-gray-600">Contractor not found</p>
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

  const pastJobs = jobs.filter(j => j.contractorId === contractor.id && j.status === 'completed')

  return (
    <div className="min-h-screen bg-lightGray pt-16">
      <Navigation />
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <button
          onClick={() => router.back()}
          className="mb-4 flex items-center space-x-2 text-darkGray hover:text-primary"
        >
          <span>← Back</span>
        </button>

        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="flex items-start space-x-6">
            <div className="w-24 h-24 bg-primary/10 rounded-full flex items-center justify-center">
              <span className="text-3xl font-bold text-primary">
                {contractor.name.charAt(0)}
              </span>
            </div>
            <div className="flex-1">
              <div className="flex items-start justify-between mb-4">
                <div>
                  <h1 className="text-2xl font-bold text-darkGray mb-1">{contractor.name}</h1>
                  <p className="text-lg text-gray-600 mb-2">{contractor.company}</p>
                  <div className="flex items-center space-x-4">
                    <div className="flex items-center space-x-1">
                      <FiStar className="w-5 h-5 text-yellow-500 fill-current" />
                      <span className="font-semibold text-darkGray">{contractor.rating}</span>
                    </div>
                    <div className="flex items-center space-x-1 text-gray-600">
                      <FiMapPin className="w-4 h-4" />
                      <span>{contractor.distance} miles away</span>
                    </div>
                  </div>
                </div>
                <label className="flex items-center space-x-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={contractor.preferred}
                    readOnly
                    className="w-4 h-4 text-primary rounded"
                  />
                  <span className="text-sm text-darkGray">Preferred</span>
                </label>
              </div>

              <div className="mb-4">
                <h3 className="font-semibold text-darkGray mb-2">Specialization</h3>
                <div className="flex flex-wrap gap-2">
                  {contractor.specialization.map((spec) => (
                    <span
                      key={spec}
                      className="px-3 py-1 bg-primary/10 text-primary text-sm rounded-lg"
                    >
                      {spec}
                    </span>
                  ))}
                </div>
              </div>

              <div className="border-t pt-4">
                <p className="text-sm text-gray-600">
                  <span className="font-semibold">{contractor.completedJobs}</span> completed jobs
                </p>
              </div>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-xl font-bold text-darkGray mb-4">Past Jobs</h2>
          <div className="space-y-3">
            {pastJobs.length > 0 ? (
              pastJobs.map((job) => (
                <div
                  key={job.id}
                  className="p-4 border border-lightGray rounded-lg hover:bg-lightGray transition-colors"
                >
                  <div className="flex justify-between items-start">
                    <div>
                      <p className="font-medium text-darkGray">{job.issueType}</p>
                      <p className="text-sm text-gray-600">{job.propertyAddress}</p>
                      <p className="text-sm text-gray-500">Date: {job.date}</p>
                    </div>
                    {job.rating && (
                      <div className="flex items-center space-x-1">
                        <FiStar className="w-4 h-4 text-yellow-500 fill-current" />
                        <span>{job.rating}</span>
                      </div>
                    )}
                  </div>
                </div>
              ))
            ) : (
              <p className="text-gray-600 text-center py-4">No past jobs available</p>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

