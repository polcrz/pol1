package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 1000; // 2 seconds for splash screen
    private static final int FADE_ANIMATION_COUNT = 3; // Number of times to repeat the fade-in/fade-out animation

    private CircleImageView img1;
    private TextView companyName, subtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // Set the splash screen layout

        // Initialize views
        img1 = findViewById(R.id.img1);
        companyName = findViewById(R.id.companyName);
        subtitle = findViewById(R.id.subtitle);

        // Load fade-in and fade-out animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);

        // Set up loop for loading effect
        final Handler handler = new Handler();

        final Runnable fadeRunnable = new Runnable() {
            @Override
            public void run() {
                // Apply fade-out animation
                img1.startAnimation(fadeOut);
                companyName.startAnimation(fadeOut);
                subtitle.startAnimation(fadeOut);

                // After fade-out, apply fade-in to create a loop effect
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Apply fade-in animation
                        img1.startAnimation(fadeIn);
                        companyName.startAnimation(fadeIn);
                        subtitle.startAnimation(fadeIn);
                    }
                }, 800); // Wait for the fade-out duration before applying fade-in
            }
        };

        // Start the loading animation loop, repeat 3 times
        for (int i = 0; i < FADE_ANIMATION_COUNT; i++) {
            handler.postDelayed(fadeRunnable, i * 1600); // 1600ms (fadeIn + fadeOut duration) delay for each loop
        }

        // After 3 loops, proceed to check user role
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkUserRole(); // Proceed to check user role after splash screen
            }
        }, FADE_ANIMATION_COUNT * 1600);  // Wait for 3 loops (3 * 1600ms = 4800ms)
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
