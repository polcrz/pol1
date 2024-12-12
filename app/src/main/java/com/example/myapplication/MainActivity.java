package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.util.Log;

import com.example.myapplication.Model.ReportActivity;
import com.example.myapplication.databinding.ActivityMainBinding;

public class MainActivity extends BaseActivity {

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        // Pass binding's root view to setupDrawer
        setupDrawer(binding.getRoot());

        // Ensure ActionBar setup
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Home");
        } else {
            Log.e("Home", "ActionBar is null!");
        }

        // Set click listeners
        binding.reportBtn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ReportActivity.class))
        );

        binding.inventoryButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, InventoryActivity.class))
        );

        binding.sales.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SalesActivity.class))
        );
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Start the home activity
        startActivity(intent);

        // Finish the current activity (optional, depending on your needs)
        finish();
    }
}
