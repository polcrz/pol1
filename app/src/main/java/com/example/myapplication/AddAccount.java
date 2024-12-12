package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AddAccount extends AppCompatActivity {

    FirebaseAuth auth;
    DatabaseReference databaseReference; // Realtime Database reference
    ProgressDialog progressDialog;
    Button addAccount, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account); // Ensure this matches your XML layout file.

        // Initialize views
        addAccount = findViewById(R.id.addAccount);
        btnBack = findViewById(R.id.btnBack);

        // Initialize Firebase services
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users"); // Root node in Realtime Database

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Create Your Account");
        progressDialog.setMessage("Please Wait");

        addAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get input values
                String name = ((EditText) findViewById(R.id.name)).getText().toString();
                String email = ((EditText) findViewById(R.id.email)).getText().toString();
                String password = ((EditText) findViewById(R.id.password)).getText().toString();
                String role = ((EditText) findViewById(R.id.role)).getText().toString().trim().toLowerCase();

                // Validate inputs
                if (name.isEmpty()) {
                    Toast.makeText(AddAccount.this, "Enter Your Name", Toast.LENGTH_SHORT).show();
                } else if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(AddAccount.this, "Enter a valid Email", Toast.LENGTH_SHORT).show();
                } else if (password.isEmpty() || password.length() < 6) {
                    Toast.makeText(AddAccount.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                } else if (role.isEmpty() || (!role.equals("admin") && !role.equals("vendor"))) {
                    Toast.makeText(AddAccount.this, "Role must be either 'admin' or 'vendor'", Toast.LENGTH_SHORT).show();
                } else {
                    progressDialog.show();

                    // Create account with Firebase Authentication
                    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            progressDialog.dismiss();
                            if (task.isSuccessful()) {
                                String id = Objects.requireNonNull(task.getResult().getUser()).getUid();

                                // Save user data in Realtime Database
                                Map<String, Object> userMap = new HashMap<>();
                                userMap.put("email", email);
                                userMap.put("name", name);
                                userMap.put("password", password);
                                userMap.put("role", role);

                                // Save the data under the user's unique ID
                                databaseReference.child(id).setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(AddAccount.this, "Account added to Realtime Database!", Toast.LENGTH_SHORT).show();
                                            finish(); // Close the activity after account creation
                                        } else {
                                            Toast.makeText(AddAccount.this, "Failed to save in Realtime Database: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            } else {
                                Toast.makeText(AddAccount.this, "Auth failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

        // Handle back button
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddAccount.this, Accounts.class); // Replace Accounts.class with the correct target activity
                startActivity(intent);
            }
        });
    }
}
