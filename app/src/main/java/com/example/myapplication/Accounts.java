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

public class Accounts extends BaseActivity {
    private ActivityAccountsBinding binding;
    private DatabaseReference usersReference;
    private TextView textViewName;
    private TextView addAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize binding
        binding = ActivityAccountsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupDrawer(binding.getRoot());

        // Ensure ActionBar setup
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Accounts");
        } else {
            Log.e("Accounts", "ActionBar is null!");
        }

        // Initialize Realtime Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersReference = database.getReference("users"); // Reference to the users node

        // Initialize TextView
        textViewName = findViewById(R.id.textViewName);

        addAccount = findViewById(R.id.AccountAdd);
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
        // Fetching data from the "users" node in Realtime Database
        usersReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                StringBuilder data = new StringBuilder();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String name = userSnapshot.child("name").getValue(String.class);
                    String email = userSnapshot.child("email").getValue(String.class);
                    String role = userSnapshot.child("role").getValue(String.class);
                    data.append("Name: ").append(name).append("\nEmail: ").append(email).append("\nRole: ").append(role).append("\n\n");
                }
                textViewName.setText(data.toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                textViewName.setText("Error: " + databaseError.getMessage());
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(Accounts.this, MainActivity.class));
        finish();
    }
}
