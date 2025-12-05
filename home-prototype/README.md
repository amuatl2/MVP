# HOME MVP Prototype

Housing Operations & Maintenance Engine - Interactive Prototype

## Overview

This is a functional web-based prototype for the HOME (Housing Operations & Maintenance Engine) platform. It demonstrates all user flows and interactions across Tenant, Landlord, and Contractor roles.

## Features

### User Roles
- **Tenant**: Report issues, track tickets, view status
- **Landlord**: Manage tickets, AI diagnosis, assign contractors, view marketplace
- **Contractor**: View jobs, accept assignments, mark completion, track performance

### Key Screens
1. **Login** - Role-based authentication with "Remember me"
2. **Dashboard** - Role-specific landing pages
3. **Create Ticket** - Issue submission with AI category suggestions
4. **Ticket Detail** - Full ticket view with status tracker, AI diagnosis, chat
5. **Marketplace** - Contractor browsing with filters
6. **Contractor Profile** - Individual contractor details
7. **Contractor Dashboard** - Job management for contractors
8. **Job Detail** - Contractor job view with completion workflow
9. **Schedule** - Calendar-based appointment scheduling
10. **Rating** - Post-job feedback system
11. **History** - Completed jobs log with analytics

## Setup & Installation

1. Install dependencies:
```bash
cd home-prototype
npm install
```

2. Run the development server:
```bash
npm run dev
```

3. Open [http://localhost:3000](http://localhost:3000) in your browser

## Usage

1. Start at the Login page
2. Select a user role (Tenant, Landlord, or Contractor)
3. Click "Login" (no actual credentials needed for prototype)
4. Navigate through the app using the top navigation bar
5. Test the various flows:
   - Tenant: Create Ticket → View Ticket Details
   - Landlord: View Open Tickets → AI Diagnosis → Marketplace → Assign Contractor
   - Contractor: View Jobs → Job Details → Mark Complete → Rating

## Design Specifications

- **Colors**: Primary Blue (#3A86FF), Light Gray (#E2E8F0), Dark Gray (#2D3748), White (#FFFFFF)
- **Font**: Inter / Poppins
- **Buttons**: Rounded corners (8-10px), primary blue with hover effects
- **Navigation**: Fixed top bar with all tabs visible

## Technology Stack

- Next.js 14 (React Framework)
- TypeScript
- Tailwind CSS
- React Context for state management
- React Icons

## Data Flow

The prototype simulates realistic data flow:
- Tickets created by tenants appear in landlord dashboard
- Landlord assignments update contractor dashboard
- Job completions update history and enable ratings
- Ratings update contractor profiles

All data is stored in React Context for the prototype session.

## Notes

- This is a prototype/mockup - no actual backend or database
- Data persists only during the session (localStorage for "Remember me")
- All interactions are simulated but fully functional within the prototype
- Perfect for demonstrations, user testing, and stakeholder reviews

