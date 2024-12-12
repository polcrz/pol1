package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.Model.SalesModel;
import com.example.myapplication.databinding.ActivitySalesBinding;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.FirebaseDatabase;

public class SalesActivity extends BaseActivity {

    RecyclerView recyclerView;
    SalesAdapter adapter;
    ImageButton historyBtn;

    ActivitySalesBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales);  // Ensure the correct layout is loaded

        binding = ActivitySalesBinding.inflate(getLayoutInflater());
        setupDrawer(binding.getRoot());

        // Safely set the title in the ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Sales");
        } else {
            Log.e("Sales", "ActionBar is null!");
        }

        // Initialize the history button and set its click listener
        historyBtn = findViewById(R.id.historyBtn);
        historyBtn.setOnClickListener(v -> {
            startActivity(new Intent(SalesActivity.this, SalesRecord.class));
        });

        // Set up RecyclerView with FirebaseAdapter
        recyclerView = findViewById(R.id.salesRecyclerV);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        FirebaseRecyclerOptions<SalesModel> options =
                new FirebaseRecyclerOptions.Builder<SalesModel>()
                        .setQuery(FirebaseDatabase.getInstance().getReference().child("Products"), SalesModel.class)
                        .build();

        adapter = new SalesAdapter(options);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(SalesActivity.this, MainActivity.class));
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }
}
