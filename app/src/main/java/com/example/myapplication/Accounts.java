package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivityAccountsBinding;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class Accounts extends AppCompatActivity {
    private ActivityAccountsBinding binding;
    private DatabaseReference usersReference;
    private TextView textViewName;

    private Button backBTN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize binding
        binding = ActivityAccountsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Realtime Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersReference = database.getReference().child("users"); // Reference to the users node

        usersReference.keepSynced(true);  // Keep the data synced across devices


        // Initialize TextView
        textViewName = findViewById(R.id.textViewName);
        backBTN = findViewById(R.id.backBTN);

        backBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Accounts.this, adminSuperAdmin.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        // Set up the Add Account button
        binding.AccountAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Accounts.this, AddAccount.class);
                startActivity(intent);
            }
        });

        // Fetch and display data from Realtime Database
        fetchData();
    }

    private void fetchData() {
        String currentUserRole = getIntent().getStringExtra("currentUserRole");

        usersReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                StringBuilder data = new StringBuilder();

                int userCount = 0;  // Add counter to track number of users
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    userCount++;
                    String name = userSnapshot.child("name").getValue(String.class);
                    String email = userSnapshot.child("email").getValue(String.class);
                    String role = userSnapshot.child("role").getValue(String.class);


                        data.append("Name: ").append(name).append("\n");
                        data.append("Email: ").append(email).append("\n");
                        data.append("Role: ").append(role).append("\n");
                        data.append("----------------------------------------------------\n");

                        Log.d("Accounts", "User count: " + userCount);  // Log user count

                        data.append("\n");

                }

                // Add a log to check total number of users fetched
                Log.d("Accounts", "Total users fetched: " + userCount);

                textViewName.setText(data.toString());
            }


            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Accounts", "Error fetching data: " + databaseError.getMessage());
                textViewName.setText("Error: " + databaseError.getMessage());
            }
        });
    }



    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(Accounts.this, adminSuperAdmin.class));
        finish();
    }
}
