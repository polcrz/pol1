package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.databinding.ActivitySalesBinding;
import com.example.myapplication.databinding.ActivitySalesRecordBinding;

public class SalesRecord extends BaseActivity {

    ActivitySalesRecordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_record);

        binding = ActivitySalesRecordBinding.inflate(getLayoutInflater());
        setupDrawer(binding.getRoot());

        // Safely set the title in the ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Sales Record");
        } else {
            Log.e("Sales Record", "ActionBar is null!");
        }




    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(SalesRecord.this, SalesActivity.class));
        finish();
    }
}

