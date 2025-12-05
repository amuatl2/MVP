# Quick Start Guide

## Prerequisites

Make sure you have **Node.js** installed (version 18 or higher).

To check if you have Node.js:
```bash
node --version
npm --version
```

If you don't have Node.js, download it from: https://nodejs.org/

## Step-by-Step Instructions

### 1. Open Terminal/PowerShell

Navigate to the project folder:
```bash
cd "C:\AMU\GATECH\Semester_5\CS 4803\home-prototype"
```

### 2. Install Dependencies

Run this command to install all required packages:
```bash
npm install
```

This will take a minute or two. You should see a `node_modules` folder created.

### 3. Start the Development Server

Run:
```bash
npm run dev
```

You should see output like:
```
▲ Next.js 14.0.0
- Local:        http://localhost:3000
✓ Ready in 2.5s
```

### 4. Open in Browser

Open your web browser and go to:
```
http://localhost:3000
```

### 5. Test the Prototype

1. **Login Page** - Select any role (Tenant, Landlord, or Contractor) and click Login
2. **No credentials needed** - Just click Login to proceed
3. **Navigate** - Use the top navigation bar to explore all screens
4. **Try flows**:
   - Create a ticket as Tenant
   - View tickets as Landlord
   - Assign contractors from Marketplace
   - Complete jobs as Contractor

## Troubleshooting

### Port 3000 already in use?
If you see an error about port 3000 being busy, you can run on a different port:
```bash
npm run dev -- -p 3001
```

Then open `http://localhost:3001` instead.

### Module not found errors?
Delete `node_modules` folder and `package-lock.json`, then run `npm install` again.

### Still having issues?
Make sure you're in the correct directory (`home-prototype` folder) and Node.js is properly installed.

## Stopping the Server

Press `Ctrl + C` in the terminal to stop the development server.

