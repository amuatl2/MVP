'use client'

import React from 'react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useAuth } from '@/context/AuthContext'
import { 
  FiHome, FiTool, FiMessageCircle, FiCalendar, 
  FiStar, FiFileText, FiUsers, FiLogOut 
} from 'react-icons/fi'

const Navigation = () => {
  const pathname = usePathname()
  const { user, logout } = useAuth()

  if (!user || pathname === '/login') return null

  const navItems = [
    { icon: FiHome, label: 'Dashboard', path: '/dashboard' },
    { icon: FiTool, label: 'Ticket', path: '/ticket' },
    { icon: FiMessageCircle, label: 'Chat', path: '/chat' },
    { icon: FiCalendar, label: 'Schedule', path: '/schedule' },
    { icon: FiStar, label: 'Rating', path: '/rating' },
    { icon: FiFileText, label: 'History', path: '/history' },
  ]

  if (user.role === 'landlord') {
    navItems.splice(2, 0, { icon: FiUsers, label: 'Marketplace', path: '/marketplace' })
  }

  if (user.role === 'contractor') {
    navItems.splice(1, 0, { icon: FiUsers, label: 'Jobs', path: '/contractor-dashboard' })
  }

  return (
    <nav className="fixed top-0 left-0 right-0 bg-white border-b border-lightGray z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          <div className="flex items-center space-x-1">
            <span className="text-2xl font-bold text-primary">HOME</span>
          </div>
          <div className="flex items-center space-x-4">
            {navItems.map((item) => {
              const Icon = item.icon
              const isActive = pathname === item.path || pathname?.startsWith(item.path + '/')
              return (
                <Link
                  key={item.path}
                  href={item.path}
                  className={`flex items-center space-x-1 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-primary text-white'
                      : 'text-darkGray hover:bg-lightGray'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  <span>{item.label}</span>
                </Link>
              )
            })}
            <button
              onClick={logout}
              className="flex items-center space-x-1 px-3 py-2 rounded-lg text-sm font-medium text-darkGray hover:bg-lightGray"
            >
              <FiLogOut className="w-4 h-4" />
              <span>Logout</span>
            </button>
          </div>
        </div>
      </div>
    </nav>
  )
}

export default Navigation

