'use client'

import React, { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from '@/context/AuthContext'
import { useData } from '@/context/DataContext'
import Navigation from '@/components/Navigation'
import { FiUpload, FiX } from 'react-icons/fi'

export default function CreateTicketPage() {
  const router = useRouter()
  const { user } = useAuth()
  const { addTicket } = useData()
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [category, setCategory] = useState('')
  const [showAIMessage, setShowAIMessage] = useState(false)
  const [uploadedFiles, setUploadedFiles] = useState<string[]>([])
  const [submitted, setSubmitted] = useState(false)

  if (!user) {
    router.push('/login')
    return null
  }

  const categories = ['Plumbing', 'Electrical', 'HVAC', 'Appliance', 'General Maintenance']

  const handleCategoryChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setCategory(e.target.value)
    if (e.target.value) {
      setShowAIMessage(true)
      setTimeout(() => setShowAIMessage(false), 3000)
    }
  }

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const files = Array.from(e.target.files).map(f => URL.createObjectURL(f))
      setUploadedFiles([...uploadedFiles, ...files])
    }
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const newTicket = {
      id: `ticket-${Date.now()}`,
      title,
      description,
      category,
      status: 'submitted' as const,
      submittedBy: user.email,
      createdAt: new Date().toISOString(),
    }
    addTicket(newTicket)
    setSubmitted(true)
    setTimeout(() => {
      router.push('/dashboard')
    }, 2000)
  }

  if (submitted) {
    return (
      <div className="min-h-screen bg-lightGray pt-16">
        <Navigation />
        <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="bg-white rounded-lg shadow-md p-8 text-center">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
            </div>
            <h2 className="text-2xl font-bold text-darkGray mb-2">Ticket Submitted Successfully!</h2>
            <p className="text-gray-600">Your maintenance request has been created and will be reviewed soon.</p>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-lightGray pt-16">
      <Navigation />
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="bg-white rounded-lg shadow-md p-6">
          <h1 className="text-2xl font-bold text-darkGray mb-6">Create Ticket</h1>
          
          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-darkGray mb-2">
                Title
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
                placeholder="Brief description of the issue"
                className="w-full px-4 py-2 border border-lightGray rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-darkGray mb-2">
                Description
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                required
                rows={5}
                placeholder="Provide detailed information about the issue..."
                className="w-full px-4 py-2 border border-lightGray rounded-lg focus:outline-none focus:ring-2 focus:ring-primary resize-none"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-darkGray mb-2">
                Category
              </label>
              <select
                value={category}
                onChange={handleCategoryChange}
                required
                className="w-full px-4 py-2 border border-lightGray rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
              >
                <option value="">Select a category</option>
                {categories.map((cat) => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
              {showAIMessage && (
                <div className="mt-2 p-3 bg-blue-50 border border-blue-200 rounded-lg">
                  <p className="text-sm text-blue-800">
                    🤖 AI Suggestion: Category "{category}" detected. Suggested diagnosis will appear after submission.
                  </p>
                </div>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-darkGray mb-2">
                Upload Photo/Video
              </label>
              <div className="border-2 border-dashed border-lightGray rounded-lg p-6 text-center">
                <FiUpload className="w-8 h-8 text-gray-400 mx-auto mb-2" />
                <input
                  type="file"
                  accept="image/*,video/*"
                  multiple
                  onChange={handleFileUpload}
                  className="hidden"
                  id="file-upload"
                />
                <label
                  htmlFor="file-upload"
                  className="cursor-pointer text-primary hover:underline"
                >
                  Click to upload or drag and drop
                </label>
                <p className="text-xs text-gray-500 mt-1">PNG, JPG, MP4 up to 10MB</p>
              </div>
              {uploadedFiles.length > 0 && (
                <div className="mt-4 grid grid-cols-3 gap-4">
                  {uploadedFiles.map((file, index) => (
                    <div key={index} className="relative">
                      <img
                        src={file}
                        alt={`Upload ${index + 1}`}
                        className="w-full h-24 object-cover rounded-lg"
                      />
                      <button
                        type="button"
                        onClick={() => setUploadedFiles(uploadedFiles.filter((_, i) => i !== index))}
                        className="absolute top-1 right-1 bg-red-500 text-white rounded-full p-1"
                      >
                        <FiX className="w-3 h-3" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="flex space-x-4">
              <button
                type="button"
                onClick={() => router.back()}
                className="flex-1 px-6 py-2 border border-lightGray rounded-lg text-darkGray font-medium hover:bg-lightGray transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="flex-1 px-6 py-2 bg-primary text-white rounded-lg font-medium hover:bg-blue-600 transition-colors"
              >
                Submit Ticket
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}

