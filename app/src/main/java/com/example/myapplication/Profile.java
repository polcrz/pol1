package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.myapplication.databinding.ActivityProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Profile extends BaseActivity {

    private ActivityProfileBinding binding;

    private EditText editName;
    private EditText editEmail;
    private TextView roleTextView;
    private Button saveButton;
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize binding
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupDrawer(binding.getRoot());

        // Initialize Firebase services
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // Initialize views
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        roleTextView = findViewById(R.id.roleTextView);
        saveButton = findViewById(R.id.saveButton);

        // Fetch and display user data
        fetchUserDetails();

        // Set OnClickListener for Save button
        saveButton.setOnClickListener(v -> saveUserProfile());
    }

    private void fetchUserDetails() {
        String userId = auth.getCurrentUser().getUid(); // Get the current user's UID

        // Reference to the user's data in Realtime Database
        databaseReference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Retrieve user details from the database
                    String name = dataSnapshot.child("name").getValue(String.class);
                    String email = dataSnapshot.child("email").getValue(String.class);
                    String role = dataSnapshot.child("role").getValue(String.class);

                    // Set the data into EditText and TextView
                    editName.setText(name);
                    editEmail.setText(email);
                    roleTextView.setText("Role: " + role);
                } else {
                    Toast.makeText(Profile.this, "User data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(Profile.this, "Failed to fetch user data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserProfile() {
        String userId = auth.getCurrentUser().getUid();
        String updatedName = editName.getText().toString();
        String updatedEmail = editEmail.getText().toString();

        if (updatedName.isEmpty() || updatedEmail.isEmpty()) {
            Toast.makeText(Profile.this, "Please fill in both name and email", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update the user's details in Firebase Realtime Database
        databaseReference.child(userId).child("name").setValue(updatedName);
        databaseReference.child(userId).child("email").setValue(updatedEmail);

        Toast.makeText(Profile.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(Profile.this, MainActivity.class));
        finish();
    }
}
