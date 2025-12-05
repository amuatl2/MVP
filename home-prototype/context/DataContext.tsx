'use client'

import React, { createContext, useContext, useState } from 'react'

export interface Ticket {
  id: string
  title: string
  description: string
  category: string
  status: 'submitted' | 'assigned' | 'scheduled' | 'completed'
  submittedBy: string
  assignedTo?: string
  aiDiagnosis?: string
  photos?: string[]
  createdAt: string
  scheduledDate?: string
  completedDate?: string
  rating?: number
}

export interface Contractor {
  id: string
  name: string
  company: string
  specialization: string[]
  rating: number
  distance: number
  preferred: boolean
  completedJobs: number
}

export interface Job {
  id: string
  ticketId: string
  contractorId: string
  propertyAddress: string
  issueType: string
  date: string
  status: string
  cost?: number
  duration?: number
  rating?: number
}

interface DataContextType {
  tickets: Ticket[]
  contractors: Contractor[]
  jobs: Job[]
  addTicket: (ticket: Ticket) => void
  updateTicket: (id: string, updates: Partial<Ticket>) => void
  assignContractor: (ticketId: string, contractorId: string) => void
  completeJob: (jobId: string) => void
  addRating: (jobId: string, rating: number) => void
}

const DataContext = createContext<DataContextType | undefined>(undefined)

const mockTickets: Ticket[] = [
  {
    id: '1',
    title: 'Leaky Faucet in Kitchen',
    description: 'The kitchen faucet has been dripping constantly for the past week.',
    category: 'Plumbing',
    status: 'assigned',
    submittedBy: 'tenant@example.com',
    assignedTo: 'contractor1',
    aiDiagnosis: 'Plumbing - Faucet Repair',
    createdAt: '2024-01-15T10:00:00Z',
  },
  {
    id: '2',
    title: 'Broken Light Switch',
    description: 'The light switch in the hallway is not working.',
    category: 'Electrical',
    status: 'submitted',
    submittedBy: 'tenant@example.com',
    aiDiagnosis: 'Electrical - Switch Replacement',
    createdAt: '2024-01-16T14:30:00Z',
  },
]

const mockContractors: Contractor[] = [
  {
    id: 'contractor1',
    name: 'John Smith',
    company: 'ABC Plumbing',
    specialization: ['Plumbing', 'HVAC'],
    rating: 4.8,
    distance: 2.5,
    preferred: true,
    completedJobs: 45,
  },
  {
    id: 'contractor2',
    name: 'Sarah Johnson',
    company: 'Electric Solutions',
    specialization: ['Electrical'],
    rating: 4.6,
    distance: 5.2,
    preferred: false,
    completedJobs: 32,
  },
  {
    id: 'contractor3',
    name: 'Mike Davis',
    company: 'All-in-One Maintenance',
    specialization: ['Plumbing', 'Electrical', 'HVAC'],
    rating: 4.9,
    distance: 1.8,
    preferred: true,
    completedJobs: 78,
  },
]

const mockJobs: Job[] = [
  {
    id: 'job1',
    ticketId: '1',
    contractorId: 'contractor1',
    propertyAddress: '123 Main St, Apt 4B',
    issueType: 'Plumbing',
    date: '2024-01-15',
    status: 'assigned',
  },
]

export function DataProvider({ children }: { children: React.ReactNode }) {
  const [tickets, setTickets] = useState<Ticket[]>(mockTickets)
  const [contractors] = useState<Contractor[]>(mockContractors)
  const [jobs, setJobs] = useState<Job[]>(mockJobs)

  const addTicket = (ticket: Ticket) => {
    setTickets([...tickets, ticket])
  }

  const updateTicket = (id: string, updates: Partial<Ticket>) => {
    setTickets(tickets.map(t => t.id === id ? { ...t, ...updates } : t))
  }

  const assignContractor = (ticketId: string, contractorId: string) => {
    updateTicket(ticketId, { assignedTo: contractorId, status: 'assigned' })
    const ticket = tickets.find(t => t.id === ticketId)
    if (ticket) {
      setJobs([...jobs, {
        id: `job${jobs.length + 1}`,
        ticketId,
        contractorId,
        propertyAddress: '123 Main St, Apt 4B',
        issueType: ticket.category,
        date: new Date().toISOString().split('T')[0],
        status: 'assigned',
      }])
    }
  }

  const completeJob = (jobId: string) => {
    const job = jobs.find(j => j.id === jobId)
    if (job) {
      setJobs(jobs.map(j => j.id === jobId ? { ...j, status: 'completed', completedDate: new Date().toISOString() } : j))
      updateTicket(job.ticketId, { status: 'completed', completedDate: new Date().toISOString() })
    }
  }

  const addRating = (jobId: string, rating: number) => {
    setJobs(jobs.map(j => j.id === jobId ? { ...j, rating } : j))
  }

  return (
    <DataContext.Provider value={{
      tickets,
      contractors,
      jobs,
      addTicket,
      updateTicket,
      assignContractor,
      completeJob,
      addRating,
    }}>
      {children}
    </DataContext.Provider>
  )
}

export function useData() {
  const context = useContext(DataContext)
  if (context === undefined) {
    throw new Error('useData must be used within a DataProvider')
  }
  return context
}

