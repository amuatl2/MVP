'use client'

import React, { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from '@/context/AuthContext'
import Navigation from '@/components/Navigation'
import { FiCalendar, FiClock } from 'react-icons/fi'

export default function SchedulePage() {
  const router = useRouter()
  const { user } = useAuth()
  const [selectedDate, setSelectedDate] = useState<string>('')
  const [selectedTime, setSelectedTime] = useState<string>('')
  const [confirmed, setConfirmed] = useState(false)

  if (!user) {
    router.push('/login')
    return null
  }

  const availableDates = [
    { date: '2024-11-03', day: 'Sat', slots: ['10:00 AM', '2:00 PM', '4:00 PM'] },
    { date: '2024-11-04', day: 'Sun', slots: ['9:00 AM', '1:00 PM', '3:00 PM'] },
    { date: '2024-11-05', day: 'Mon', slots: ['10:00 AM', '2:00 PM', '5:00 PM'] },
    { date: '2024-11-06', day: 'Tue', slots: ['11:00 AM', '3:00 PM'] },
  ]

  const handleConfirm = () => {
    setConfirmed(true)
    setTimeout(() => {
      router.push('/dashboard')
    }, 2000)
  }

  if (confirmed) {
    return (
      <div className="min-h-screen bg-lightGray pt-16">
        <Navigation />
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="bg-white rounded-lg shadow-md p-8 text-center">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <FiCalendar className="w-8 h-8 text-green-600" />
            </div>
            <h2 className="text-2xl font-bold text-darkGray mb-2">Schedule Confirmed!</h2>
            <p className="text-gray-600 mb-2">
              Appointment scheduled for {selectedDate} at {selectedTime}
            </p>
            <p className="text-sm text-gray-500">You will receive a confirmation notification.</p>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-lightGray pt-16">
      <Navigation />
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-darkGray mb-2">Scheduling</h1>
          <p className="text-gray-600">Select an available time slot</p>
        </div>

        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="flex items-center space-x-3 mb-6">
            <FiCalendar className="w-6 h-6 text-primary" />
            <h2 className="text-xl font-semibold text-darkGray">Available Dates</h2>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            {availableDates.map((item) => (
              <button
                key={item.date}
                onClick={() => {
                  setSelectedDate(item.date)
                  setSelectedTime('')
                }}
                className={`p-4 border-2 rounded-lg text-center transition-colors ${
                  selectedDate === item.date
                    ? 'border-primary bg-primary/10'
                    : 'border-lightGray hover:border-primary/50'
                }`}
              >
                <p className="text-sm text-gray-600">{item.day}</p>
                <p className="font-semibold text-darkGray">
                  {new Date(item.date).getDate()}
                </p>
                <p className="text-xs text-gray-500">
                  {new Date(item.date).toLocaleDateString('en-US', { month: 'short' })}
                </p>
              </button>
            ))}
          </div>

          {selectedDate && (
            <div>
              <div className="flex items-center space-x-3 mb-4">
                <FiClock className="w-5 h-5 text-primary" />
                <h3 className="font-semibold text-darkGray">Available Time Slots</h3>
              </div>
              <div className="grid grid-cols-3 md:grid-cols-4 gap-3">
                {availableDates.find(d => d.date === selectedDate)?.slots.map((slot) => (
                  <button
                    key={slot}
                    onClick={() => setSelectedTime(slot)}
                    className={`px-4 py-2 border rounded-lg transition-colors ${
                      selectedTime === slot
                        ? 'border-primary bg-primary text-white'
                        : 'border-lightGray hover:border-primary'
                    }`}
                  >
                    {slot}
                  </button>
                ))}
              </div>
            </div>
          )}

          {!selectedDate && (
            <div className="text-center py-8 text-gray-500">
              <p>Select a date to see available time slots</p>
            </div>
          )}
        </div>

        {selectedDate && selectedTime && (
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
            <p className="text-blue-800">
              <strong>Next available slot:</strong> {selectedDate} at {selectedTime}
            </p>
          </div>
        )}

        {selectedDate && selectedTime && (
          <button
            onClick={handleConfirm}
            className="w-full px-6 py-3 bg-primary text-white rounded-lg font-medium hover:bg-blue-600 transition-colors"
          >
            Confirm Schedule
          </button>
        )}
      </div>
    </div>
  )
}

