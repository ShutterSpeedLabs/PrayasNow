# PrayasNow - Android Login System

A comprehensive Android login system with Firebase authentication, SQLite local storage, and Google Sign-In integration.

## Features

- **Email/Password Authentication**: Sign up and login with email and password
- **Google Sign-In**: Login with Google account
- **Forgot Password**: Password reset functionality
- **Local Data Storage**: SQLite database for offline data sync
- **Firebase Integration**: Real-time authentication and data sync
- **Modern UI**: Material Design 3 with Jetpack Compose
- **Virtual Keyboard Support**: Proper keyboard handling for input fields

## Setup Instructions

### 1. Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or select an existing one
3. Add an Android app to your Firebase project:
   - Package name: `com.example.prayasnow`
   - App nickname: `PrayasNow`
4. Download the `google-services.json` file
5. Replace the sample `google-services.json` in the `app/` directory with your actual Firebase configuration

### 2. Google Sign-In Setup

1. In Firebase Console, go to Authentication > Sign-in method
2. Enable Google Sign-in provider
3. Add your SHA-1 fingerprint to the project settings
4. Update the web client ID in `LoginScreen.kt`:
   ```kotlin
   .requestIdToken("your-actual-web-client-id")
   ```

### 3. Build and Run

1. Open the project in Android Studio
2. Sync the project with Gradle files
3. Build and run the app

## Project Structure

```
app/src/main/java/com/example/prayasnow/
├── data/                    # Database entities and DAOs
│   ├── User.kt             # User entity
│   ├── UserDao.kt          # Database access object
│   └── AppDatabase.kt      # Room database
├── repository/              # Data layer
│   └── AuthRepository.kt   # Authentication repository
├── viewmodel/              # ViewModels
│   └── AuthViewModel.kt    # Authentication ViewModel
├── ui/                     # UI components
│   ├── screens/            # Screen composables
│   │   ├── LoginScreen.kt  # Login screen
│   │   └── HomeScreen.kt   # Home screen
│   └── navigation/         # Navigation
│       └── AppNavigation.kt
├── di/                     # Dependency injection
│   └── AppModule.kt        # App module
└── MainActivity.kt         # Main activity
```

## Key Features Implementation

### Virtual Keyboard Support
- Input fields automatically show the virtual keyboard
- Keyboard type is configured for email and password fields
- `windowSoftInputMode="adjustResize"` in AndroidManifest.xml

### Firebase Authentication
- Email/password authentication
- Google Sign-In integration
- Password reset functionality
- Real-time authentication state management

### SQLite Local Storage
- Room database for local user data
- Automatic sync with Firebase on login
- Offline data persistence
- Data sync only happens once after login

### Google Sign-In
- Integration with device's Google account
- Automatic token handling
- Error handling for sign-in failures

## Dependencies

- **Firebase**: Authentication and Firestore
- **Room**: Local SQLite database
- **Jetpack Compose**: Modern UI framework
- **Navigation**: Screen navigation
- **ViewModel**: State management
- **Coroutines**: Asynchronous operations

## Configuration

### Firebase Configuration
Replace the sample `google-services.json` with your actual Firebase project configuration.

### Google Sign-In
Update the web client ID in `LoginScreen.kt` with your actual Google OAuth client ID.

## Usage

1. **First Time Setup**: Users can sign up with email/password or Google account
2. **Login**: Users can login with their credentials or Google account
3. **Forgot Password**: Users can reset their password via email
4. **Data Sync**: User data is synced to local SQLite database on first login
5. **Offline Access**: App works offline with locally stored data
6. **Logout**: Users can logout, which clears local data and requires re-authentication

## Security Features

- Password visibility toggle
- Secure password input
- Firebase security rules
- Local data encryption (Room)
- Token-based authentication

## Troubleshooting

1. **Firebase Connection Issues**: Ensure `google-services.json` is properly configured
2. **Google Sign-In Issues**: Verify SHA-1 fingerprint and web client ID
3. **Build Errors**: Make sure all dependencies are properly synced
4. **Database Issues**: Clear app data if database schema changes

## License

This project is for educational purposes. Please ensure compliance with Firebase and Google services terms of use. 