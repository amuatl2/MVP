import type { Metadata } from 'next'
import './globals.css'
import { AuthProvider } from '@/context/AuthContext'
import { DataProvider } from '@/context/DataContext'
import LayoutWrapper from '@/components/LayoutWrapper'

export const metadata: Metadata = {
  title: 'HOME - Housing Operations & Maintenance Engine',
  description: 'MVP Prototype for HOME Platform',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body>
        <AuthProvider>
          <DataProvider>
            <LayoutWrapper>
              {children}
            </LayoutWrapper>
          </DataProvider>
        </AuthProvider>
      </body>
    </html>
  )
}

