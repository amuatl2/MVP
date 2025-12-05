# Installing Node.js - Step by Step Guide

## 🚨 Current Issue
You're seeing `'npm' is not recognized` because Node.js is not installed on your computer.

## ✅ Solution: Install Node.js

### Step 1: Download Node.js
1. Open your web browser
2. Go to: **https://nodejs.org/**
3. You'll see two buttons:
   - **LTS** (recommended) - This is the stable version
   - **Current** - Latest features
4. **Click the LTS button** to download

### Step 2: Install Node.js
1. **Find the downloaded file** (usually in your Downloads folder)
   - It will be named something like: `node-v20.x.x-x64.msi` or `node-v18.x.x-x64.msi`
2. **Double-click the installer file**
3. **Follow the installation wizard**:
   - Click "Next" on the welcome screen
   - Accept the license agreement → Next
   - Keep default installation path → Next
   - **IMPORTANT**: Make sure "Automatically install the necessary tools" is checked → Next
   - Click "Install" (you may need to enter admin password)
   - Wait for installation to complete
   - Click "Finish"

### Step 3: Restart Your Terminal ⚠️ CRITICAL STEP
**You MUST close and reopen your terminal/PowerShell for changes to take effect!**

1. Close your current PowerShell/Command Prompt window completely
2. Open a NEW PowerShell window
3. Navigate back to your project:
   ```powershell
   cd "C:\AMU\GATECH\Semester_5\CS 4803\home-prototype"
   ```

### Step 4: Verify Installation
Run these commands to check if it worked:

```powershell
node --version
npm --version
```

You should see version numbers (like `v20.10.0` and `10.2.3`)

### Step 5: Install Dependencies
Once Node.js is installed and verified:

```powershell
npm install
```

This will take 1-2 minutes. You'll see it downloading packages.

### Step 6: Run the Prototype
```powershell
npm run dev
```

Then open: **http://localhost:3000** in your browser

---

## ⚠️ Still Not Working?

### Option A: Check if Node.js is Installed but PATH is Wrong
1. Search for "Environment Variables" in Windows
2. Edit System Environment Variables
3. Check if Node.js path is in System PATH

### Option B: Use Chocolatey (If you have it)
```powershell
choco install nodejs-lts
```

### Option C: Alternative - Use Portable Version
1. Download the portable zip from nodejs.org
2. Extract it
3. Add the path to your PATH environment variable

---

## Need Help?
After installing Node.js and restarting your terminal, if you still see errors, let me know what happens when you run:
```powershell
node --version
```

