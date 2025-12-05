'use client'

import React from 'react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useAuth } from '@/context/AuthContext'
import { 
  FiHome, FiTool, FiMessageCircle, FiCalendar, 
  FiStar, FiFileText, FiUsers, FiBell
} from 'react-icons/fi'

const BottomNavigation = () => {
  const pathname = usePathname()
  const { user } = useAuth()

  if (!user || pathname === '/login') return null

  let navItems: Array<{ icon: any, label: string, path: string }> = []

  if (user.role === 'tenant') {
    navItems = [
      { icon: FiHome, label: 'Dashboard', path: '/dashboard' },
      { icon: FiTool, label: 'Ticket', path: '/ticket/create' },
      { icon: FiMessageCircle, label: 'Messages', path: '/dashboard' }, // Placeholder for messages
      { icon: FiStar, label: 'Review', path: '/rating' },
      { icon: FiFileText, label: 'History', path: '/history' },
      { icon: FiBell, label: 'AI Chat', path: '/chat' },
    ]
  } else if (user.role === 'landlord') {
    navItems = [
      { icon: FiHome, label: 'Dashboard', path: '/dashboard' },
      { icon: FiUsers, label: 'Tenants', path: '/dashboard' }, // Placeholder
      { icon: FiUsers, label: 'Marketplace', path: '/marketplace' },
      { icon: FiMessageCircle, label: 'Messages', path: '/dashboard' }, // Placeholder
      { icon: FiFileText, label: 'History', path: '/history' },
      { icon: FiBell, label: 'AI Chat', path: '/chat' },
    ]
  } else if (user.role === 'contractor') {
    navItems = [
      { icon: FiTool, label: 'Jobs', path: '/contractor-dashboard' },
      { icon: FiCalendar, label: 'Schedule', path: '/schedule' },
      { icon: FiMessageCircle, label: 'Messages', path: '/dashboard' }, // Placeholder
      { icon: FiFileText, label: 'History', path: '/history' },
      { icon: FiBell, label: 'AI Chat', path: '/chat' },
    ]
  }

  return (
    <nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-lightGray z-50 shadow-lg">
      <div className="max-w-7xl mx-auto">
        <div className="flex justify-around items-center h-16">
          {navItems.map((item) => {
            const Icon = item.icon
            const isActive = pathname === item.path || pathname?.startsWith(item.path + '/')
            return (
              <Link
                key={item.path}
                href={item.path}
                className={`flex flex-col items-center justify-center px-3 py-2 rounded-lg transition-colors min-w-[60px] ${
                  isActive
                    ? 'text-primary'
                    : 'text-gray-600 hover:text-primary'
                }`}
              >
                <Icon className={`w-5 h-5 mb-1 ${isActive ? 'text-primary' : ''}`} />
                <span className={`text-xs font-medium ${isActive ? 'text-primary' : 'text-gray-600'}`}>
                  {item.label}
                </span>
              </Link>
            )
          })}
        </div>
      </div>
    </nav>
  )
}

export default BottomNavigation

