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

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
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
    private SignInButton googleSignInButton;
    private GoogleSignInClient mGoogleSignInClient; // Declared as a class member
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(getApplicationContext());

        // If a token already exists, skip login and go to the main app
        if (sessionManager.fetchAuthToken() != null) {
            navigateToWelcome();
            return;
        }

        setContentView(R.layout.activity_login);

        apiService = RetrofitClient.getApiService();

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        signUpTextView = findViewById(R.id.signUpTextView);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id)) // Use your Web Client ID here
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize the launcher for the sign-in intent
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
            sendTokenToBackend(idToken);
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
                    sessionManager.saveAuthToken(response.body().accessToken);
                    navigateToWelcome();
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

                    String token = response.body().accessToken;
                    sessionManager.saveAuthToken(token);

                    navigateToWelcome();
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

    private void navigateToWelcome() {
        Intent intent = new Intent(LoginActivity.this, WelcomeActivity.class);
        startActivity(intent);
        finish(); // Prevent user from going back to the login screen
    }
}