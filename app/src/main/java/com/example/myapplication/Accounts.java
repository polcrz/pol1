package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.Model.User;
import com.example.myapplication.databinding.ActivityAccountsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class Accounts extends AppCompatActivity {
    private ActivityAccountsBinding binding;
    private DatabaseReference usersReference;
    private Button backBTN, addBTN;

    private RecyclerView recyclerViewAccounts;
    private UserAdapter userAdapter;
    private List<User> userList = new ArrayList<>();
    private boolean isSuperAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize binding
        binding = ActivityAccountsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersReference = database.getReference("users"); // Ensure the "users" node exists

        // Initialize RecyclerView
        recyclerViewAccounts = binding.recyclerViewAccounts; // From binding
        recyclerViewAccounts.setLayoutManager(new LinearLayoutManager(this));

        // Initialize Adapter
        userAdapter = new UserAdapter(userList, this, getCurrentUserRole());
        recyclerViewAccounts.setAdapter(userAdapter);

        // Fetch user data
        fetchData();

        // Initialize buttons
        backBTN = findViewById(R.id.backBTN);
        addBTN = findViewById(R.id.AccountAdd);

        // Check if the current user is a superadmin
        checkSuperAdminStatus();

        // Set back button listener
        backBTN.setOnClickListener(v -> {
            // Go back to the previous activity
            onBackPressed();
        });

        // Set add account button listener (only visible/enabled for superadmin)
        addBTN.setOnClickListener(v -> {
            // Logic to add new account (e.g., navigate to AddAccountActivity)
            Intent intent = new Intent(Accounts.this, AddAccount.class);
            startActivity(intent);
        });
    }

    private void fetchData() {
        if (usersReference == null) {
            Log.e("Accounts", "Database reference is null!");
            return;
        }

        usersReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null) {
                        Log.d("Accounts", "Fetched User: " + user.getName());
                        userList.add(user);
                    }
                }
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Accounts", "Database error: " + databaseError.getMessage());
            }
        });
    }

    private void checkSuperAdminStatus() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference currentUserRef = usersReference.child(currentUserId);

        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String role = dataSnapshot.child("role").getValue(String.class);
                if ("superadmin".equalsIgnoreCase(role)) {
                    isSuperAdmin = true;
                    addBTN.setVisibility(View.VISIBLE); // Show add button if superadmin
                } else {
                    isSuperAdmin = false;
                    addBTN.setVisibility(View.GONE); // Hide add button if not superadmin
                }
                userAdapter.setSuperAdmin(isSuperAdmin); // Update adapter with superadmin status
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Accounts", "Failed to fetch user role", databaseError.toException());
            }
        });
    }

    private String getCurrentUserRole() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference currentUserRef = usersReference.child(currentUserId);
        final String[] role = {null};

        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                role[0] = dataSnapshot.child("role").getValue(String.class);
                if (role[0] == null) {
                    role[0] = "user"; // Default role if not found
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Accounts", "Failed to fetch user role", databaseError.toException());
            }
        });

        return role[0];
    }

    // Handle account deletion with a confirmation dialog (only for superadmin)
    public void deleteAccount(final String userId) {
        if (isSuperAdmin) {
            // Create confirmation dialog
            new AlertDialog.Builder(this)
                    .setTitle("Delete Account")
                    .setMessage("Are you sure you want to delete this account?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Perform account deletion here (e.g., remove from Firebase)
                        usersReference.child(userId).removeValue();
                        Toast.makeText(Accounts.this, "Account deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        // Do nothing if canceled
                        dialog.dismiss();
                    })
                    .create()
                    .show();
        } else {
            // Show message if the user does not have permission
            Toast.makeText(this, "You don't have permission to delete accounts", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(Accounts.this, adminSuperAdmin.class));
        finish();
    }
}