package com.example.snipit.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.snipit.app.MainActivity;
import com.example.snipit.app.R;
import com.example.snipit.app.auth.LoginActivity;
import com.example.snipit.app.database.SnipRepository;
import com.example.snipit.app.util.XpManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import android.widget.ImageView;

public class ProfileFragment extends Fragment {

    private FirebaseAuth auth;
    private SnipRepository snipRepo;
    private ImageView profileImg;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    profileImg.setImageURI(uri);
                    // Persist locally for this session or save to cloud in Phase 4
                    Toast.makeText(getContext(), "Profile identity updated", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        auth = FirebaseAuth.getInstance();
        snipRepo = new SnipRepository(requireActivity().getApplication());

        FirebaseUser user = auth.getCurrentUser();
        final TextView nameTv = v.findViewById(R.id.profile_name);
        final TextView avatarTv = v.findViewById(R.id.profile_avatar);
        final TextView emailTv = v.findViewById(R.id.prof_email_val);
        final TextView handleTv = v.findViewById(R.id.profile_handle);
        profileImg = v.findViewById(R.id.profile_avatar_img);

        if (profileImg != null) {
            profileImg.setOnClickListener(view -> {
                pickMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            });
        }

        if (user != null) {
            String name = user.getDisplayName();
            if (name == null || name.isEmpty()) {
                name = "Developer";
            }
            if (nameTv != null) {
                nameTv.setText(name);
                avatarTv.setText(name.substring(0, 1).toUpperCase());
            }
            if (emailTv != null) emailTv.setText(user.getEmail());
            if (handleTv != null) {
                String handle = user.getEmail().split("@")[0];
                handleTv.setText("@" + handle);
            }
        } else {
            // User not logged in, redirect to login
            startActivity(new Intent(getContext(), com.example.snipit.app.auth.LoginActivity.class));
            if (getActivity() != null) getActivity().finish();
            return;
        }

        // Load identity and bio from Firebase
        DatabaseReference profileDb = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("profile");

        profileDb.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("displayName").getValue(String.class);
                    String bio = snapshot.child("bio").getValue(String.class);
                    
                    if (name != null) {
                        nameTv.setText(name);
                        avatarTv.setText(name.substring(0, 1).toUpperCase());
                    }
                    if (bio != null) {
                        TextView bioTv = v.findViewById(R.id.profile_bio);
                        bioTv.setText(bio);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
        });

        // Stats
        TextView snipCountTv = v.findViewById(R.id.stat_snips);
        snipRepo.getAllSnips().observe(getViewLifecycleOwner(), snips -> {
            if (snips != null) {
                snipCountTv.setText(String.valueOf(snips.size()));
            }
        });

        TextView xpTv = v.findViewById(R.id.stat_xp);
        xpTv.setText(String.valueOf(XpManager.getTotalXp(requireContext())));

        // Beams (Static for now or load from prefs if tracked)
        TextView beamsTv = v.findViewById(R.id.stat_beams);
        beamsTv.setText("0");

        // Actions with Fragment Safety
        View btnEdit = v.findViewById(R.id.btn_edit_profile);
        if (btnEdit != null) {
            btnEdit.setOnClickListener(view -> {
                if (isAdded()) startActivity(new Intent(requireContext(), com.example.snipit.app.EditProfileActivity.class));
            });
        }
        
        View btnPair = v.findViewById(R.id.btn_pair_device);
        if (btnPair != null) {
            btnPair.setOnClickListener(view -> {
                if (isAdded() && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchTab(2); // Switch to Beam Tab
                }
            });
        }

        View btnLogout = v.findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(view -> {
                if (auth != null && isAdded()) {
                    auth.signOut();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    if (getActivity() != null) getActivity().finish();
                }
            });
        }
    }
}
