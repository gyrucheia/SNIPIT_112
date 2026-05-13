package com.example.snipit.app;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText nameInput, bioInput;
    private ProgressBar progress;
    private FirebaseAuth auth;
    private DatabaseReference db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        db = FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("profile");

        nameInput = findViewById(R.id.edit_display_name);
        bioInput = findViewById(R.id.edit_bio);
        progress = findViewById(R.id.progress_edit);

        // Load existing
        nameInput.setText(user.getDisplayName());
        db.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String bio = snapshot.child("bio").getValue(String.class);
                    if (bio != null) bioInput.setText(bio);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });

        findViewById(R.id.btn_save_profile).setOnClickListener(v -> saveChanges());
        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
    }

    private void saveChanges() {
        String name = nameInput.getText().toString().trim();
        String bio = bioInput.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        progress.setVisibility(View.VISIBLE);
        FirebaseUser user = auth.getCurrentUser();

        // 1. Update Auth Name
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 2. Update Database Profile (Bio, etc.)
                Map<String, Object> data = new HashMap<>();
                data.put("displayName", name);
                data.put("bio", bio);
                data.put("lastUpdated", System.currentTimeMillis());

                db.updateChildren(data).addOnCompleteListener(dbTask -> {
                    progress.setVisibility(View.GONE);
                    if (dbTask.isSuccessful()) {
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to sync to cloud.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                progress.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to update identity.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
