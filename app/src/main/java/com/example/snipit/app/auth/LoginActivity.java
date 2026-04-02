package com.example.snipit.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.snipit.app.MainActivity;
import com.example.snipit.app.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.OAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private GoogleSignInClient googleClient;
    private ActivityResultLauncher<Intent> googleLauncher;

    private EditText email;
    private EditText password;
    private ProgressBar progress;
    private TextView error;
    private View btnLogin;
    private View btnGoogle;
    private View btnGithub;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content),
                (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                    int bottom = Math.max(bars.bottom, ime.bottom);
                    v.setPadding(bars.left, bars.top, bars.right, bottom);
                    return insets;
                });

        auth = FirebaseAuth.getInstance();

        email = findViewById(R.id.input_email);
        password = findViewById(R.id.input_password);
        progress = findViewById(R.id.progress_login);
        error = findViewById(R.id.error_text);
        btnLogin = findViewById(R.id.btn_login);
        btnGoogle = findViewById(R.id.btn_google);
        btnGithub = findViewById(R.id.btn_github);

        String webClientId = getString(R.string.default_web_client_id);
        GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build();
        googleClient = GoogleSignIn.getClient(this, gso);
        googleLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result == null || result.getData() == null) {
                                setLoading(false);
                                return;
                            }
                            Task<GoogleSignInAccount> task =
                                    GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            handleGoogleResult(task);
                        });

        btnLogin.setOnClickListener(v -> loginWithEmail());
        btnGoogle.setOnClickListener(v -> loginWithGoogle());
        btnGithub.setOnClickListener(v -> loginWithGithub());

        findViewById(R.id.btn_go_signup)
                .setOnClickListener(v -> startActivity(new Intent(this, SignupActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser u = auth.getCurrentUser();
        if (u != null && u.isEmailVerified()) {
            goToApp();
        }
    }

    private void loginWithEmail() {
        String e = email.getText() != null ? email.getText().toString().trim() : "";
        String p = password.getText() != null ? password.getText().toString() : "";
        if (e.isEmpty() || p.isEmpty()) {
            showError("Enter email and password.");
            return;
        }
        setLoading(true);
        auth.signInWithEmailAndPassword(e, p)
                .addOnCompleteListener(
                        task -> {
                            setLoading(false);
                            if (task.isSuccessful()) {
                                FirebaseUser user = auth.getCurrentUser();
                                if (user != null && user.isEmailVerified()) {
                                    goToApp();
                                } else if (user != null) {
                                    showError("Please verify your email address first.");
                                    user.sendEmailVerification();
                                    auth.signOut();
                                }
                            } else {
                                showError(
                                        task.getException() != null
                                                ? task.getException().getMessage()
                                                : "Login failed.");
                            }
                        });
    }

    private void loginWithGoogle() {
        setLoading(true);
        error.setVisibility(View.GONE);
        googleLauncher.launch(googleClient.getSignInIntent());
    }

    private void loginWithGithub() {
        setLoading(true);
        error.setVisibility(View.GONE);

        OAuthProvider.Builder provider = OAuthProvider.newBuilder("github.com");
        provider.setScopes(java.util.Arrays.asList("read:user", "user:email"));

        Task<com.google.firebase.auth.AuthResult> pending = auth.getPendingAuthResult();
        if (pending != null) {
            pending.addOnCompleteListener(
                            task -> {
                                setLoading(false);
                                if (task.isSuccessful()) {
                                    goToApp();
                                } else {
                                    showError(
                                            task.getException() != null
                                                    ? task.getException().getMessage()
                                                    : "GitHub sign-in failed.");
                                }
                            })
                    .addOnFailureListener(
                            e -> {
                                setLoading(false);
                                showError(e.getMessage() != null ? e.getMessage() : "GitHub sign-in failed.");
                            });
            return;
        }

        auth.startActivityForSignInWithProvider(this, provider.build())
                .addOnCompleteListener(
                        task -> {
                            setLoading(false);
                            if (task.isSuccessful()) {
                                goToApp();
                            } else {
                                showError(
                                        task.getException() != null
                                                ? task.getException().getMessage()
                                                : "GitHub sign-in failed.");
                                }
                        })
                .addOnFailureListener(
                        e -> {
                            setLoading(false);
                            showError(e.getMessage() != null ? e.getMessage() : "GitHub sign-in failed.");
                        });
    }

    private void handleGoogleResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account == null || account.getIdToken() == null) {
                setLoading(false);
                showError("Google sign-in failed.");
                return;
            }
            auth.signInWithCredential(GoogleAuthProvider.getCredential(account.getIdToken(), null))
                    .addOnCompleteListener(
                            task -> {
                                setLoading(false);
                                if (task.isSuccessful()) {
                                    goToApp();
                                } else {
                                    showError(
                                            task.getException() != null
                                                    ? task.getException().getMessage()
                                                    : "Google sign-in failed.");
                                }
                            });
        } catch (ApiException e) {
            setLoading(false);
            showError(e.getMessage() != null ? e.getMessage() : "Google sign-in cancelled.");
        }
    }

    private void goToApp() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    private void setLoading(boolean on) {
        progress.setVisibility(on ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!on);
        btnGoogle.setEnabled(!on);
        btnGithub.setEnabled(!on);
    }

    private void showError(String msg) {
        error.setText(msg);
        error.setVisibility(View.VISIBLE);
    }
}
