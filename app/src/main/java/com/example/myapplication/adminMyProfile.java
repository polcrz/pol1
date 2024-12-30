package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class adminMyProfile extends AppCompatActivity {

    private EditText editName;
    private EditText editEmail;
    private EditText editPassword;
    private TextView roleTextView;
    private Button saveButton;
    private Button uploadImageButton;
    private CheckBox showPasswordCheckBox;
    private ImageView pImage;
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private Button back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adming_my_profile);

        // Initialize Firebase services
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // Initialize views
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        roleTextView = findViewById(R.id.roleTextView);
        saveButton = findViewById(R.id.saveButton);
        back = findViewById(R.id.backBtn);
        pImage = findViewById(R.id.pImage);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        showPasswordCheckBox = findViewById(R.id.showPasswordCheckBox);

        back.setOnClickListener(v -> {
            Intent intent = new Intent(adminMyProfile.this, adminSuperAdmin.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Fetch and display user data
        fetchUserDetails();

        // Set OnClickListener for Save button
        saveButton.setOnClickListener(v -> showSaveConfirmationDialog());

        // Set OnClickListener for upload image button
        uploadImageButton.setOnClickListener(v -> showImageUrlInputDialog());

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
                    Toast.makeText(adminMyProfile.this, "User data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(adminMyProfile.this, "Failed to fetch user data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(adminMyProfile.this, "Please fill in both name and email", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update the user's details in Firebase Realtime Database
        databaseReference.child(userId).child("name").setValue(updatedName);
        databaseReference.child(userId).child("email").setValue(updatedEmail);
        databaseReference.child(userId).child("password").setValue(updatedPassword); // Assume password is stored in plain text (not recommended)

        Toast.makeText(adminMyProfile.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
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
        startActivity(new Intent(adminMyProfile.this, adminSuperAdmin.class));
        finish();
    }
}