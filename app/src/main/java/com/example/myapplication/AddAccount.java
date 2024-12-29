package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AddAccount extends AppCompatActivity {

    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private ProgressDialog progressDialog;
    private Button addAccount, btnBack;
    private Spinner roleSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account);

        // Initialize Firebase services
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("users");

        // Initialize views
        addAccount = findViewById(R.id.addAccount);
        btnBack = findViewById(R.id.btnBack);
        roleSpinner = findViewById(R.id.roleSpinner);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Create Your Account");
        progressDialog.setMessage("Please Wait");

        // Set up the Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.roles_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);
        roleSpinner.setSelection(0); // Set the default selection to "Select Role"

        // Check current user's role
        String currentUserId = auth.getCurrentUser().getUid();
        databaseReference.child(currentUserId).child("role").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful() && task.getResult().exists()) {
                    String role = task.getResult().getValue(String.class);
                    if (!"superadmin".equalsIgnoreCase(role)) {
                        Toast.makeText(AddAccount.this, "Access Denied: Only SuperAdmins can add accounts", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(AddAccount.this, "Failed to retrieve user role. Access Denied.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });

        addAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = ((EditText) findViewById(R.id.name)).getText().toString();
                String email = ((EditText) findViewById(R.id.email)).getText().toString();
                String password = ((EditText) findViewById(R.id.password)).getText().toString();
                String role = roleSpinner.getSelectedItem().toString();

                // Validate inputs
                if (name.isEmpty()) {
                    Toast.makeText(AddAccount.this, "Enter Your Name", Toast.LENGTH_SHORT).show();
                } else if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(AddAccount.this, "Enter a valid Email", Toast.LENGTH_SHORT).show();
                } else if (password.isEmpty() || password.length() < 6) {
                    Toast.makeText(AddAccount.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                } else if (role.equals("Select Role")) { // Validate the role selection
                    Toast.makeText(AddAccount.this, "Please select a valid role: Vendor or Admin", Toast.LENGTH_SHORT).show();
                } else {
                    progressDialog.show();

                    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            progressDialog.dismiss();
                            if (task.isSuccessful()) {
                                String id = Objects.requireNonNull(task.getResult().getUser()).getUid();

                                Map<String, Object> userMap = new HashMap<>();
                                userMap.put("email", email);
                                userMap.put("name", name);
                                userMap.put("password", password);
                                userMap.put("role", role);

                                databaseReference.child(id).setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(AddAccount.this, "Account added successfully!", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(AddAccount.this, Accounts.class));
                                            finish();
                                        } else {
                                            Toast.makeText(AddAccount.this, "Failed to save account: " +
                                                    Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            } else {
                                Toast.makeText(AddAccount.this, "Auth failed: " +
                                        Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

        // Back button action
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AddAccount.this, Accounts.class));
            }
        });
    }
}
