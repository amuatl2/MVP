'use client'

import React, { useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { useAuth } from '@/context/AuthContext'
import { useData } from '@/context/DataContext'
import Navigation from '@/components/Navigation'
import { FiStar } from 'react-icons/fi'

export default function RatingPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { user } = useAuth()
  const { jobs, contractors, addRating } = useData()
  const [rating, setRating] = useState(0)
  const [hoverRating, setHoverRating] = useState(0)
  const [comment, setComment] = useState('')
  const [submitted, setSubmitted] = useState(false)

  const jobId = searchParams.get('job')
  const job = jobId ? jobs.find(j => j.id === jobId) : null
  const contractor = job ? contractors.find(c => c.id === job.contractorId) : null

  if (!user) {
    router.push('/login')
    return null
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (jobId && rating > 0) {
      addRating(jobId, rating)
      setSubmitted(true)
      setTimeout(() => {
        router.push('/history')
      }, 2000)
    }
  }

  if (submitted) {
    return (
      <div className="min-h-screen bg-lightGray pt-16">
        <Navigation />
        <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="bg-white rounded-lg shadow-md p-8 text-center">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <FiStar className="w-8 h-8 text-yellow-500 fill-current" />
            </div>
            <h2 className="text-2xl font-bold text-darkGray mb-2">Rating Submitted!</h2>
            <p className="text-gray-600">Thank you for your feedback.</p>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-lightGray pt-16">
      <Navigation />
      <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="bg-white rounded-lg shadow-md p-6">
          <h1 className="text-2xl font-bold text-darkGray mb-6">Rate Contractor</h1>
          
          {contractor && (
            <div className="mb-6 p-4 bg-gray-50 rounded-lg">
              <p className="text-sm text-gray-600 mb-1">Job completed by</p>
              <p className="font-semibold text-darkGray">{contractor.company} - {contractor.name}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-darkGray mb-4">
                Rating (5-star)
              </label>
              <div className="flex items-center space-x-2">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setRating(star)}
                    onMouseEnter={() => setHoverRating(star)}
                    onMouseLeave={() => setHoverRating(0)}
                    className="focus:outline-none"
                  >
                    <FiStar
                      className={`w-10 h-10 transition-colors ${
                        star <= (hoverRating || rating)
                          ? 'text-yellow-500 fill-current'
                          : 'text-gray-300'
                      }`}
                    />
                  </button>
                ))}
                {rating > 0 && (
                  <span className="ml-4 text-darkGray font-medium">{rating} / 5</span>
                )}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-darkGray mb-2">
                Additional Comments (optional)
              </label>
              <textarea
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                rows={4}
                placeholder="Share your experience..."
                className="w-full px-4 py-2 border border-lightGray rounded-lg focus:outline-none focus:ring-2 focus:ring-primary resize-none"
              />
            </div>

            <div className="flex space-x-4">
              <button
                type="button"
                onClick={() => router.back()}
                className="flex-1 px-6 py-2 border border-lightGray rounded-lg text-darkGray font-medium hover:bg-lightGray transition-colors"
              >
                Skip
              </button>
              <button
                type="submit"
                disabled={rating === 0}
                className="flex-1 px-6 py-2 bg-primary text-white rounded-lg font-medium hover:bg-blue-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Submit Rating
              </button>
            </div>
          </form>

          {(user.role === 'tenant' || user.role === 'landlord') && (
            <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
              <p className="text-sm text-blue-800">
                💡 Both tenants and landlords can rate contractors after job completion. 
                Ratings are aggregated to show contractor reliability and average score.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

