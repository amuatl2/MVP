'use client'

import React, { createContext, useContext, useState, useEffect } from 'react'

type UserRole = 'tenant' | 'landlord' | 'contractor'

interface User {
  email: string
  role: UserRole
  name: string
}

interface AuthContextType {
  user: User | null
  login: (email: string, password: string, role: UserRole, rememberMe: boolean) => void
  logout: () => void
  isAuthenticated: boolean
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)

  useEffect(() => {
    // Check for stored session
    const storedUser = localStorage.getItem('home_user')
    if (storedUser) {
      setUser(JSON.parse(storedUser))
    }
  }, [])

  const login = (email: string, password: string, role: UserRole, rememberMe: boolean) => {
    const newUser: User = {
      email,
      role,
      name: email.split('@')[0],
    }
    setUser(newUser)
    if (rememberMe) {
      localStorage.setItem('home_user', JSON.stringify(newUser))
    }
  }

  const logout = () => {
    setUser(null)
    localStorage.removeItem('home_user')
  }

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

