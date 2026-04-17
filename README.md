# FamilyTracker
FamilyTracker is a real-time family location sharing Android app built with Kotlin, Firebase, and OpenStreetMap. 
It is designed for parents and family members who want to stay connected and ensure each other's safety by 
tracking live GPS locations on an interactive map. Each user registers with their basic details — name, age, 
gender, and family relation — and joins a shared family group using a unique 6-digit group code. Once in a 
group, any member can request to view another member's live location, and the other person receives an 
instant popup notification to allow or deny the request, ensuring complete privacy and consent. Approved 
location requests show real-time movement on a map with color-coded initials markers for easy identification. 
Members can also see who is currently viewing their location, toggle their own location sharing on or off 
at any time, edit their profile details, and invite new family members directly through WhatsApp or email 
by sharing the APK file. The app uses Firebase Firestore for real-time data sync, Firebase Authentication 
for secure login, and Firebase Cloud Messaging for push notifications — making it a lightweight, fully 
functional family safety tool that works without any subscription or payment.

## Install the App
The ready-to-install APK is located at:
app/build/outputs/apk/debug/app-debug.apk
To install:
1. Transfer `app-debug.apk` to your Android phone via WhatsApp, email, or USB
2. Open the file on your phone
3. If prompted, go to Settings → Install unknown apps and allow it
4. Tap Install

## Run from Source
1. Clone the repo:
   git clone https://github.com/Priyanshu0930/Family_Loc_Tracker.git
2. Open the project in Android Studio
3. Place your google-services.json from Firebase inside the app/ folder
4. Sync Gradle and build the project
5. Run on a physical Android device (API 24+)

## Requirements
- Android Studio Hedgehog or later
- Android device running API 24 (Android 7.0) or above
- Firebase project with Firestore and Authentication enabled
- Internet connection on the device
