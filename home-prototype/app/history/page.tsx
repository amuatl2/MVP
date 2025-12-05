'use client'

import React, { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from '@/context/AuthContext'
import { useData } from '@/context/DataContext'
import Navigation from '@/components/Navigation'
import { FiStar, FiFilter, FiCalendar } from 'react-icons/fi'

export default function HistoryPage() {
  const router = useRouter()
  const { user } = useAuth()
  const { jobs, contractors } = useData()
  const [filterIssueType, setFilterIssueType] = useState('')
  const [filterContractor, setFilterContractor] = useState('')

  if (!user) {
    router.push('/login')
    return null
  }

  const completedJobs = jobs.filter(j => j.status === 'completed')
  
  const filteredJobs = completedJobs.filter(job => {
    if (filterIssueType && job.issueType !== filterIssueType) return false
    if (filterContractor && job.contractorId !== filterContractor) return false
    return true
  })

  const issueTypes = Array.from(new Set(jobs.map(j => j.issueType)))

  // Simple analytics data
  const jobsByMonth = [
    { month: 'Jan', count: 2 },
    { month: 'Feb', count: 3 },
    { month: 'Mar', count: 5 },
    { month: 'Apr', count: 4 },
    { month: 'May', count: 6 },
    { month: 'Jun', count: 3 },
  ]

  const maxCount = Math.max(...jobsByMonth.map(j => j.count))

  return (
    <div className="min-h-screen bg-lightGray pt-16">
      <Navigation />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-darkGray mb-2">History</h1>
          <p className="text-gray-600">View completed jobs and analytics</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
          <div className="lg:col-span-2">
            <div className="bg-white rounded-lg shadow-md p-6 mb-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-semibold text-darkGray flex items-center space-x-2">
                  <FiFilter className="w-5 h-5" />
                  <span>Filters</span>
                </h2>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-darkGray mb-2">
                    Issue Type
                  </label>
                  <select
                    value={filterIssueType}
                    onChange={(e) => setFilterIssueType(e.target.value)}
                    className="w-full px-4 py-2 border border-lightGray rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                  >
                    <option value="">All Types</option>
                    {issueTypes.map(type => (
                      <option key={type} value={type}>{type}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-darkGray mb-2">
                    Contractor
                  </label>
                  <select
                    value={filterContractor}
                    onChange={(e) => setFilterContractor(e.target.value)}
                    className="w-full px-4 py-2 border border-lightGray rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                  >
                    <option value="">All Contractors</option>
                    {contractors.map(c => (
                      <option key={c.id} value={c.id}>{c.company}</option>
                    ))}
                  </select>
                </div>
              </div>
            </div>

            <div className="bg-white rounded-lg shadow-md p-6">
              <h2 className="text-xl font-semibold text-darkGray mb-4">Completed Jobs</h2>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left py-3 px-4 font-semibold text-darkGray">Contractor</th>
                      <th className="text-left py-3 px-4 font-semibold text-darkGray">Date</th>
                      <th className="text-left py-3 px-4 font-semibold text-darkGray">Issue Type</th>
                      <th className="text-left py-3 px-4 font-semibold text-darkGray">Cost</th>
                      <th className="text-left py-3 px-4 font-semibold text-darkGray">Duration</th>
                      <th className="text-left py-3 px-4 font-semibold text-darkGray">Rating</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredJobs.length > 0 ? (
                      filteredJobs.map((job) => {
                        const contractor = contractors.find(c => c.id === job.contractorId)
                        return (
                          <tr key={job.id} className="border-b hover:bg-lightGray">
                            <td className="py-3 px-4">{contractor?.company || 'N/A'}</td>
                            <td className="py-3 px-4">{job.date}</td>
                            <td className="py-3 px-4">{job.issueType}</td>
                            <td className="py-3 px-4">${job.cost || 'N/A'}</td>
                            <td className="py-3 px-4">{job.duration ? `${job.duration} hrs` : 'N/A'}</td>
                            <td className="py-3 px-4">
                              {job.rating ? (
                                <div className="flex items-center space-x-1">
                                  <FiStar className="w-4 h-4 text-yellow-500 fill-current" />
                                  <span>{job.rating}</span>
                                </div>
                              ) : (
                                'N/A'
                              )}
                            </td>
                          </tr>
                        )
                      })
                    ) : (
                      <tr>
                        <td colSpan={6} className="py-8 text-center text-gray-600">
                          No completed jobs found
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div>
            <div className="bg-white rounded-lg shadow-md p-6">
              <h2 className="text-xl font-semibold text-darkGray mb-4 flex items-center space-x-2">
                <FiCalendar className="w-5 h-5" />
                <span>Issues per Month</span>
              </h2>
              <div className="space-y-3">
                {jobsByMonth.map((item, index) => (
                  <div key={index}>
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-sm text-darkGray">{item.month}</span>
                      <span className="text-sm text-gray-600">{item.count}</span>
                    </div>
                    <div className="w-full bg-lightGray rounded-full h-2">
                      <div
                        className="bg-primary h-2 rounded-full"
                        style={{ width: `${(item.count / maxCount) * 100}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

