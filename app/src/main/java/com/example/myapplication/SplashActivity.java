package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // You can create a splash screen layout here if desired

        // Delay for splash screen
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkUserRole(); // Check the user's role after the splash delay
            }
        }, SPLASH_TIME_OUT);
    }

    // Method to check the role of the signed-in user
    private void checkUserRole() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            // No user is logged in, redirect to login
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // If user is logged in, fetch their role from the database
        String userId = auth.getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        userRef.child("role").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String role = dataSnapshot.getValue(String.class);
                if ("admin".equals(role) || "superadmin".equals(role)) {
                    // Redirect to adminSuperAdmin if the role is admin or superadmin
                    Intent intent = new Intent(SplashActivity.this, adminSuperAdmin.class);
                    startActivity(intent);
                } else {
                    // Otherwise, redirect to MainActivity
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(intent);
                }
                finish(); // Close the SplashActivity
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error if the role can't be fetched
            }
        });
    }
}
