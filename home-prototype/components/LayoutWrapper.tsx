'use client'

import React from 'react'
import { usePathname } from 'next/navigation'
import { useAuth } from '@/context/AuthContext'
import BottomNavigation from './BottomNavigation'

export default function LayoutWrapper({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const { user } = useAuth()
  const showBottomNav = user && pathname !== '/login'

  return (
    <div className={showBottomNav ? 'pb-20' : ''}>
      {children}
      {showBottomNav && <BottomNavigation />}
    </div>
  )
}

