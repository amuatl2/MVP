# Firebase Setup Guide

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" or select an existing project
3. Follow the setup wizard:
   - Enter project name (e.g., "HOME-MVP")
   - Enable Google Analytics (optional but recommended)
   - Create project

## Step 2: Add Android App to Firebase

1. In Firebase Console, click the Android icon (or "Add app")
2. Enter package name: `com.example.mvp` (check your `build.gradle.kts` to confirm)
3. Enter app nickname: "HOME Android App"
4. Register app
5. **Download `google-services.json`** - This is critical!
6. Place `google-services.json` in `app/` directory (same level as `build.gradle.kts`)

## Step 3: Enable Firebase Services

### Enable Authentication:
1. In Firebase Console, go to "Authentication"
2. Click "Get started"
3. Enable "Email/Password" sign-in method
4. Save

### Enable Firestore Database:
1. In Firebase Console, go to "Firestore Database"
2. Click "Create database"
3. Choose "Start in test mode" (for MVP/prototype)
4. Select a location (choose closest to your users)
5. Enable

### Set Firestore Security Rules (Important!):
Go to Firestore Database → Rules tab, and use:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can read/write their own user document
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Tickets - all authenticated users can read, only tenants can create
    match /tickets/{ticketId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth != null;
      allow delete: if false; // No deletes for MVP
    }
    
    // Jobs - contractors can read their own, all can read
    match /jobs/{jobId} {
      allow read: if request.auth != null;
      allow create, update: if request.auth != null;
    }
    
    // Contractors - read only for all authenticated users
    match /contractors/{contractorId} {
      allow read: if request.auth != null;
      allow write: if false; // Managed by admin only
    }
  }
}
```

## Step 4: Update Project Dependencies

The project already has Firebase dependencies configured. Just sync Gradle after adding `google-services.json`.

## Step 5: Seed Contractors Data (Optional but Recommended)

After setting up Firestore, you should add contractor data:

1. Go to Firestore Database in Firebase Console
2. Click "Start collection"
3. Collection ID: `contractors`
4. Add documents with the following structure:

**Document 1:**
- Document ID: `contractor1`
- Fields:
  - `id` (string): `contractor1`
  - `name` (string): `John Smith`
  - `company` (string): `ABC Plumbing`
  - `specialization` (array): `["Plumbing", "HVAC"]`
  - `rating` (number): `4.8`
  - `distance` (number): `2.5`
  - `preferred` (boolean): `true`
  - `completedJobs` (number): `45`

**Document 2:**
- Document ID: `contractor2`
- Fields:
  - `id` (string): `contractor2`
  - `name` (string): `Sarah Johnson`
  - `company` (string): `Electric Solutions`
  - `specialization` (array): `["Electrical"]`
  - `rating` (number): `4.6`
  - `distance` (number): `5.2`
  - `preferred` (boolean): `false`
  - `completedJobs` (number): `32`

**Document 3:**
- Document ID: `contractor3`
- Fields:
  - `id` (string): `contractor3`
  - `name` (string): `Mike Davis`
  - `company` (string): `All-in-One Maintenance`
  - `specialization` (array): `["Plumbing", "Electrical", "HVAC"]`
  - `rating` (number): `4.9`
  - `distance` (number): `1.8`
  - `preferred` (boolean): `true`
  - `completedJobs` (number): `78`

## Step 6: Verify Setup

1. **Sync Gradle** in Android Studio (File → Sync Project with Gradle Files)
2. **Build the project** - it should compile without errors
3. **Run the app** - Firebase should initialize automatically
4. **Check Logcat** for any Firebase initialization errors

## Troubleshooting

- **"FirebaseApp not initialized"**: 
  - Make sure `google-services.json` is in `app/` directory (same level as `build.gradle.kts`)
  - Clean and rebuild project (Build → Clean Project, then Build → Rebuild Project)
  
- **Build errors**: 
  - Make sure Google Services plugin is applied in `app/build.gradle.kts`
  - Check that `google-services.json` is valid JSON
  
- **Authentication not working**: 
  - Check that Email/Password is enabled in Firebase Console → Authentication → Sign-in method
  
- **Data not saving**: 
  - Check Firestore security rules (should allow authenticated users)
  - Check Logcat for Firestore errors
  - Verify user is authenticated (check Firebase Console → Authentication → Users)

## How It Works

The app now uses a **hybrid approach**:

1. **If Firebase is configured** (`google-services.json` present):
   - Uses Firebase Authentication for login
   - Uses Firestore for all data storage
   - Real-time updates across all devices
   - Data persists in the cloud

2. **If Firebase is NOT configured**:
   - Falls back to local DataStore
   - Simulated authentication (works for testing)
   - Data stored locally on device

## Benefits of Firebase

✅ **Scalability**: Handles thousands of users  
✅ **Real-time sync**: Changes appear instantly across devices  
✅ **Cloud storage**: Data backed up and accessible from anywhere  
✅ **Authentication**: Secure user management  
✅ **Team collaboration**: Multiple developers can work with same data  

## Next Steps After Setup

1. Test the app - create tickets, assign contractors, send messages
2. Check Firebase Console to see data being created in real-time
3. Test on multiple devices - changes should sync automatically
4. Share Firebase project with your team members

