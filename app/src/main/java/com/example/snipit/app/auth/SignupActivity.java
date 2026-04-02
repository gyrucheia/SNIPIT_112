package com.example.snipit.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import android.content.DialogInterface;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.snipit.app.MainActivity;
import com.example.snipit.app.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText email;
    private EditText password;
    private EditText confirm;
    private ProgressBar progress;
    private TextView error;
    private View btnSignup;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
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
        confirm = findViewById(R.id.input_password_confirm);
        progress = findViewById(R.id.progress_signup);
        error = findViewById(R.id.error_text);
        btnSignup = findViewById(R.id.btn_signup);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnSignup.setOnClickListener(v -> signup());
    }

    private void signup() {
        String e = email.getText() != null ? email.getText().toString().trim() : "";
        String p = password.getText() != null ? password.getText().toString() : "";
        String c = confirm.getText() != null ? confirm.getText().toString() : "";
        
        if (e.isEmpty() || p.isEmpty()) {
            showError("Enter email and password.");
            return;
        }
        if (p.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (!p.equals(c)) {
            showError("Passwords do not match.");
            return;
        }

        // Confirmation Dialog before creating account
        new MaterialAlertDialogBuilder(this)
                .setTitle("Create Account")
                .setMessage("Are you sure you want to create an account with " + e + "?")
                .setPositiveButton("Create", (dialog, which) -> performSignup(e, p))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performSignup(String e, String p) {
        setLoading(true);
        error.setVisibility(View.GONE);

        auth.createUserWithEmailAndPassword(e, p)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verifyTask -> {
                                        setLoading(false);
                                        if (verifyTask.isSuccessful()) {
                                            // Success Dialog
                                            new MaterialAlertDialogBuilder(SignupActivity.this)
                                                    .setTitle("Account Created!")
                                                    .setMessage("A verification email has been sent to " + e + ". Please verify your email before logging in.")
                                                    .setPositiveButton("Done", (dialog, which) -> {
                                                        auth.signOut(); // Ensure they aren't logged in yet
                                                        finish(); // Redirect back to LoginActivity
                                                    })
                                                    .setCancelable(false)
                                                    .show();
                                        } else {
                                            showError("Failed to send verification email: " + 
                                                (verifyTask.getException() != null ? verifyTask.getException().getMessage() : "Unknown error"));
                                        }
                                    });
                        }
                    } else {
                        setLoading(false);
                        showError(task.getException() != null ? task.getException().getMessage() : "Sign up failed.");
                    }
                });
    }

    private void setLoading(boolean on) {
        progress.setVisibility(on ? View.VISIBLE : View.GONE);
        btnSignup.setEnabled(!on);
    }

    private void showError(String msg) {
        error.setText(msg);
        error.setVisibility(View.VISIBLE);
    }
}
