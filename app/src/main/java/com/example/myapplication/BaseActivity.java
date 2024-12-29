package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // Set up drawer for each activity
    protected void setupDrawer(View rootView) {
        setContentView(rootView); // Use the binding's root view

        // Setup toolbar and drawer
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar((androidx.appcompat.widget.Toolbar) toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, (androidx.appcompat.widget.Toolbar) toolbar, R.string.open_nav, R.string.close_nav);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            handleNavigation(item);
            drawerLayout.closeDrawers();
            return true;
        });
    }

    // Handle navigation item selection
    protected void handleNavigation(MenuItem item) {
        Intent intent = null;

        if (item.getItemId() == R.id.nav_home) {
            if (!(this instanceof MainActivity)) {  // Check if already in MainActivity
                intent = new Intent(this, MainActivity.class);
            }
        }  else if (item.getItemId() == R.id.nav_logout) {
            // Log out the user from Firebase
            FirebaseAuth.getInstance().signOut();

            // Show a Toast message (optional)
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

            // Redirect to LoginActivity
            intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // Start the LoginActivity and finish this one
            startActivity(intent);
            finish(); // Ensure the current activity is removed from the stack
        } else if (item.getItemId() == R.id.nav_profile) {
            if (!(this instanceof Profile)) {  // Check if already in Accounts Activity
                intent = new Intent(this, Profile.class);
            }
        }

        // Start the new activity only if it's not the same as the current one
        if (intent != null) {
            startActivity(intent);
            finish();  // Finish the current activity to remove it from the stack
        }
    }
}
