'use client'

import React, { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from '@/context/AuthContext'
import Navigation from '@/components/Navigation'

export default function LoginPage() {
  const router = useRouter()
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<'tenant' | 'landlord' | 'contractor'>('tenant')
  const [rememberMe, setRememberMe] = useState(false)

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault()
    login(email || 'user@example.com', password || 'password', role, rememberMe)
    router.push('/dashboard')
  }

  return (
    <div className="min-h-screen bg-lightGray flex items-center justify-center p-4">
      <div className="bg-white rounded-lg shadow-lg p-8 w-full max-w-md">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-primary mb-2">HOME</h1>
          <p className="text-darkGray">Housing Operations & Maintenance Engine</p>
        </div>
        
        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-darkGray mb-2">
              Email
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="user@example.com"
              className="w-full px-4 py-2 border border-lightGray rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-darkGray mb-2">
              Password
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              className="w-full px-4 py-2 border border-lightGray rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-darkGray mb-2">
              User Role
            </label>
            <div className="space-y-2">
              <label className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="radio"
                  name="role"
                  value="tenant"
                  checked={role === 'tenant'}
                  onChange={(e) => setRole(e.target.value as any)}
                  className="w-4 h-4 text-primary"
                />
                <span>Tenant</span>
              </label>
              <label className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="radio"
                  name="role"
                  value="landlord"
                  checked={role === 'landlord'}
                  onChange={(e) => setRole(e.target.value as any)}
                  className="w-4 h-4 text-primary"
                />
                <span>Landlord</span>
              </label>
              <label className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="radio"
                  name="role"
                  value="contractor"
                  checked={role === 'contractor'}
                  onChange={(e) => setRole(e.target.value as any)}
                  className="w-4 h-4 text-primary"
                />
                <span>Contractor</span>
              </label>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <input
              type="checkbox"
              id="remember"
              checked={rememberMe}
              onChange={(e) => setRememberMe(e.target.checked)}
              className="w-4 h-4 text-primary rounded"
            />
            <label htmlFor="remember" className="text-sm text-darkGray cursor-pointer">
              Remember me
            </label>
          </div>

          <button
            type="submit"
            className="w-full bg-primary text-white py-2 rounded-lg font-medium hover:bg-blue-600 transition-colors"
          >
            Login
          </button>
        </form>

        <div className="mt-4 text-center">
          <a href="#" className="text-sm text-primary hover:underline">
            Create Account
          </a>
        </div>
      </div>
    </div>
  )
}

