package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivityProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Profile extends BaseActivity {

    private ActivityProfileBinding binding;

    private TextView userDetailsTextView;
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

        // Ensure ActionBar setup
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Profile");
        } else {
            Log.e("Profile", "ActionBar is null!");
        }


        // Initialize Firebase services
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // Initialize TextView
        userDetailsTextView = findViewById(R.id.userDetails);

        // Fetch and display user data
        fetchUserDetails();
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

                    // Display the user details in the TextView
                    String userDetails = "Name: " + name + "\nEmail: " + email + "\nRole: " + role;
                    userDetailsTextView.setText(userDetails);
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(Profile.this, MainActivity.class));
        finish();
    }
}

