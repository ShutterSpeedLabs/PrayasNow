package com.example.prayasnow.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.prayasnow.viewmodel.AuthState
import com.example.prayasnow.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.example.prayasnow.repository.QuizRepository
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
    quizRepository: QuizRepository,
    firestore: FirebaseFirestore
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var usernameSuggestion by remember { mutableStateOf("") }
    var usernameCheckInProgress by remember { mutableStateOf(false) }
    var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var emailOrUsername by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }

    // Username check logic
    fun checkUsernameAvailability(input: String) {
        if (input.isBlank()) {
            usernameAvailable = null
            usernameSuggestion = ""
            return
        }
        usernameCheckInProgress = true
        coroutineScope.launch {
            val snapshot = firestore.collection("users")
                .whereEqualTo("username", input)
                .get().await()
            usernameAvailable = snapshot.isEmpty
            if (!snapshot.isEmpty) {
                // Suggest a username
                val suggestion = "$input${(100..999).random()}"
                usernameSuggestion = suggestion
            } else {
                usernameSuggestion = ""
            }
            usernameCheckInProgress = false
        }
    }

    // Google Sign-In launcher
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("117987622399-3n1k9kamrtan737h2ch8sdg1o9dfugi9.apps.googleusercontent.com") // Replace with your actual web client ID
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { token ->
                authViewModel.signInWithGoogle(token)
            }
        } catch (e: ApiException) {
            // Handle Google Sign-In error
        }
    }

    // Handle authentication state changes
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn && authState.user != null) {
            onNavigateToHome()
        }
    }
    
    // Debug: Log auth state changes
    LaunchedEffect(authState) {
        println("Auth State: isLoggedIn=${authState.isLoggedIn}, user=${authState.user?.email}, isLoading=${authState.isLoading}")
    }
    
    // Auto-fill stored credentials
    LaunchedEffect(Unit) {
        try {
            val storedCredentials = authViewModel.getStoredCredentials()
            if (storedCredentials != null && !isSignUp) {
                emailOrUsername = storedCredentials.emailOrUsername
                password = storedCredentials.password
            }
        } catch (e: Exception) {
            // Handle any errors silently
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo/Title
        Text(
            text = "PrayasNow",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        if (isSignUp) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    checkUsernameAvailability(it)
                },
                label = { Text("Username") },
                isError = usernameAvailable == false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            if (usernameCheckInProgress) {
                Text("Checking username...", style = MaterialTheme.typography.bodySmall)
            } else if (usernameAvailable == false) {
                Text("Username taken. Try: $usernameSuggestion", color = MaterialTheme.colorScheme.error)
            } else if (usernameAvailable == true) {
                Text("Username available!", color = MaterialTheme.colorScheme.primary)
            }
        }
        if (!isSignUp) {
            OutlinedTextField(
                value = emailOrUsername,
                onValueChange = { emailOrUsername = it },
                label = { Text("Username or Email") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        } else {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Forgot Password Link
        TextButton(
            onClick = { showForgotPasswordDialog = true },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Forgot Password?")
        }

        // Login/Sign Up Button
        Button(
            onClick = {
                if (isSignUp) {
                    authViewModel.signUpWithEmailAndPassword(email, password, name, username)
                } else {
                    authViewModel.signInWithUsernameOrEmail(emailOrUsername, password)
                }
            },
            enabled = if (isSignUp) {
                email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty() && username.isNotEmpty() && usernameAvailable == true
            } else {
                emailOrUsername.isNotEmpty() && password.isNotEmpty()
            } && !authState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            if (authState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(if (isSignUp) "Sign Up" else "Login")
            }
        }

        // Toggle between Login and Sign Up
        TextButton(
            onClick = { isSignUp = !isSignUp }
        ) {
            Text(if (isSignUp) "Already have an account? Login" else "Don't have an account? Sign Up")
        }

        // Divider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "OR",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        // Google Sign-In Button
        OutlinedButton(
            onClick = {
                launcher.launch(googleSignInClient.signInIntent)
            },
            enabled = !authState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Sign in with Google")
        }

        // Error Message
        authState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        
        // Test Button (for development only)
        if (!isSignUp) {
            TextButton(
                onClick = {
                    authViewModel.createTestUser()
                    // Add sample quizzes for the test user
                    coroutineScope.launch {
                        try {
                            quizRepository.insertSampleQuizzes("testuser")
                        } catch (e: Exception) {
                            // Handle error
                        }
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Test Login (Create Test User)")
            }
            
            // Simple test button to force navigation
            TextButton(
                onClick = {
                    // Force navigation to profile for testing
                    onNavigateToHome()
                },
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text("Force Navigate to Profile (Test)")
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("Reset Password") },
            text = {
                Column {
                    Text("Enter your email address to receive a password reset link.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = forgotPasswordEmail,
                        onValueChange = { forgotPasswordEmail = it },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (forgotPasswordEmail.isNotEmpty()) {
                            authViewModel.resetPassword(forgotPasswordEmail)
                            showForgotPasswordDialog = false
                            forgotPasswordEmail = ""
                        }
                    }
                ) {
                    Text("Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
} 