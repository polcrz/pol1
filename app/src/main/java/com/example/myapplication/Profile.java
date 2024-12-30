package com.example.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.myapplication.databinding.ActivityProfileBinding;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Profile extends BaseActivity {

    private ActivityProfileBinding binding;

    private TextInputEditText editName;
    private TextInputEditText editEmail;
    private TextInputEditText editPassword;
    private TextView roleTextView;
    private Button saveButton;
    private Button uploadImageButton;
    private CheckBox showPasswordCheckBox;
    private ImageView pImage;
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
        editPassword = findViewById(R.id.editPassword);
        roleTextView = findViewById(R.id.roleTextView);
        saveButton = findViewById(R.id.saveButton);
        showPasswordCheckBox = findViewById(R.id.showPasswordCheckBox);
        pImage = findViewById(R.id.pImage);
        uploadImageButton = findViewById(R.id.uploadImageButton);

        // Fetch and display user data
        fetchUserDetails();

        // Set OnClickListener for Save button
        saveButton.setOnClickListener(v -> showSaveConfirmationDialog());

        // Set OnCheckedChangeListener for Checkbox to toggle password visibility
        showPasswordCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Show the password
                editPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                // Hide the password
                editPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            // Move the cursor to the end of the text
            editPassword.setSelection(editPassword.getText().length());
        });

        // Set OnClickListener for upload image button
        uploadImageButton.setOnClickListener(v -> showImageUrlInputDialog());
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
                    String password = dataSnapshot.child("password").getValue(String.class); // Assume password is stored in plain text (not recommended)
                    String role = dataSnapshot.child("role").getValue(String.class);
                    String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class);

                    // Set the data into EditText and TextView
                    editName.setText(name);
                    editEmail.setText(email);
                    editPassword.setText(password);
                    roleTextView.setText("Role: " + role);

                    // Load the image using Picasso
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Picasso.get().load(imageUrl).into(pImage);
                    }
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

    private void showSaveConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Save Changes")
                .setMessage("Are you sure you want to save these changes?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> saveUserProfile())
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void saveUserProfile() {
        String userId = auth.getCurrentUser().getUid();
        String updatedName = editName.getText().toString();
        String updatedEmail = editEmail.getText().toString();
        String updatedPassword = editPassword.getText().toString();

        if (updatedName.isEmpty() || updatedEmail.isEmpty()) {
            Toast.makeText(Profile.this, "Please fill in both name and email", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update the user's details in Firebase Realtime Database
        databaseReference.child(userId).child("name").setValue(updatedName);
        databaseReference.child(userId).child("email").setValue(updatedEmail);
        databaseReference.child(userId).child("password").setValue(updatedPassword); // Assume password is stored in plain text (not recommended)

        // Update the user's password if it's not empty
        if (!updatedPassword.isEmpty()) {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                user.updatePassword(updatedPassword).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(Profile.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Profile.this, "Failed to update password", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        Toast.makeText(Profile.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
    }

    private void showImageUrlInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Image URL");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String imageUrl = input.getText().toString();
            if (!imageUrl.isEmpty()) {
                Picasso.get().load(imageUrl).into(pImage);
                // Save the imageUrl to Firebase
                String userId = auth.getCurrentUser().getUid();
                databaseReference.child(userId).child("imageUrl").setValue(imageUrl);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(Profile.this, MainActivity.class));
        finish();
    }
}