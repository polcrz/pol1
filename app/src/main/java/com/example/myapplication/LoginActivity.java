package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.myapplication.databinding.ActivityLoginBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    ActivityLoginBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    DatabaseReference usersRef;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");  // Assuming you have a "users" node in your Realtime Database

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Logging in");
        progressDialog.setMessage("Please Wait");

        /*binding.SignupNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            }
        });*/

        binding.btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = Objects.requireNonNull(binding.email.getText()).toString();
                String password = Objects.requireNonNull(binding.password.getText()).toString();

                if (email.isEmpty()) {
                    binding.email.setError("Enter Your Email");
                } else if (password.isEmpty()) {
                    binding.password.setError("Enter Your Password");
                } else {
                    progressDialog.show();
                    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser currentUser = auth.getCurrentUser();
                                if (currentUser != null) {
                                    // Fetch user data from Firebase Realtime Database
                                    usersRef.child(currentUser.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DataSnapshot> task) {
                                            if (task.isSuccessful() && task.getResult() != null) {
                                                DataSnapshot snapshot = task.getResult();
                                                String role = snapshot.child("role").getValue(String.class); // Get user role

                                                progressDialog.dismiss();
                                                navigateBasedOnRole(role); // Navigate based on role
                                            } else {
                                                progressDialog.dismiss();
                                                Toast.makeText(LoginActivity.this, "Failed to fetch user data", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }
                            } else {
                                progressDialog.dismiss();
                                Toast.makeText(LoginActivity.this, Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

        binding.btnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, ForgetActivity.class));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // User is signed in, navigate to MainActivity (Home)
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish(); // Close LoginActivity to prevent returning to it
        }
    }

    // Method to navigate based on the user's role
    private void navigateBasedOnRole(String role) {
        if (role == null) {
            Toast.makeText(this, "Role not assigned to the user.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent;
        switch (role.toLowerCase()) {
            case "admin":
            case "superadmin": // Changed "super admin" to "superadmin"
                intent = new Intent(LoginActivity.this, adminSuperAdmin.class);
                break;
            case "vendor":
                intent = new Intent(LoginActivity.this, MainActivity.class);
                break;
            default:
                Toast.makeText(this, "Invalid role: " + role, Toast.LENGTH_SHORT).show();
                return;
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Disable back navigation by finishing the activity
        finishAffinity(); // Closes all activities and exits the app
    }
}
