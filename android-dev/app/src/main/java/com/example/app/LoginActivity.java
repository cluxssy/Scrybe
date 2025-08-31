package com.example.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView signUpTextView;
    private ApiService apiService;
    private SessionManager sessionManager;
    private AppCompatButton googleSignInButton;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(getApplicationContext());
        apiService = RetrofitClient.getApiService(); // Initialize here to be safe

        // If a token exists, check profile and navigate
        if (sessionManager.fetchAuthToken() != null) {
            // We already have a token, so just proceed to check the profile
            onLoginSuccess(sessionManager.fetchAuthToken());
            return; // Important to stop further execution of onCreate
        }

        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        signUpTextView = findViewById(R.id.signUpTextView);
        googleSignInButton = findViewById(R.id.googleSignInButton);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        handleGoogleSignInResult(task);
                    }
                });

        loginButton.setOnClickListener(v -> loginUser());

        signUpTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String idToken = account.getIdToken();
            if (idToken != null) {
                sendTokenToBackend(idToken);
            }
        } catch (ApiException e) {
            Log.w("LoginActivity", "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(this, "Google sign in failed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendTokenToBackend(String idToken) {
        apiService.loginWithGoogle(new GoogleToken(idToken)).enqueue(new Callback<Token>() {
            @Override
            public void onResponse(@NonNull Call<Token> call, @NonNull Response<Token> response) {
                if (response.isSuccessful() && response.body() != null) {
                    onLoginSuccess(response.body().accessToken);
                } else {
                    Toast.makeText(LoginActivity.this, "Backend authentication failed.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Token> call, @NonNull Throwable t) {
                Toast.makeText(LoginActivity.this, "An error occurred: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginUser() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.login(username, password).enqueue(new Callback<Token>() {
            @Override
            public void onResponse(@NonNull Call<Token> call, @NonNull Response<Token> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                    onLoginSuccess(response.body().accessToken);
                } else {
                    Toast.makeText(LoginActivity.this, "Login failed. Check credentials.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Token> call, @NonNull Throwable t) {
                Toast.makeText(LoginActivity.this, "An error occurred: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Centralized logic for handling navigation after a successful login.
     * Fetches the user profile to decide the next screen.
     * @param token The user's authentication token.
     */
    private void onLoginSuccess(String token) {
        // Ensure the token is saved before making the next API call
        if (sessionManager.fetchAuthToken() == null) {
            sessionManager.saveAuthToken(token);
        }

        apiService.readUsersMe().enqueue(new Callback<UserRead>() {
            @Override
            public void onResponse(@NonNull Call<UserRead> call, @NonNull Response<UserRead> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserRead user = response.body();
                    // If the user has NOT named the AI yet, go to the Welcome screen.
                    if (user.ai_name == null || user.ai_name.isEmpty()) {
                        Intent intent = new Intent(LoginActivity.this, WelcomeActivity.class);
                        startActivity(intent);
                    } else {
                        // Otherwise, the user is set up, so go directly to the Library.
                        Intent intent = new Intent(LoginActivity.this, LibraryActivity.class);
                        startActivity(intent);
                    }
                    finish(); // Close LoginActivity so the user can't navigate back to it
                } else {
                    Toast.makeText(LoginActivity.this, "Failed to load user profile.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserRead> call, @NonNull Throwable t) {
                Toast.makeText(LoginActivity.this, "Network error while loading profile.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}