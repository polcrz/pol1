package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.myapplication.Model.InventoryModel;
import com.example.myapplication.databinding.ActivityInventoryBinding;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.FirebaseDatabase;

public class InventoryActivity extends BaseActivity {

    RecyclerView recyclerView;
    MainAdapter mainAdapter;

    FloatingActionButton floatingActionButton;

    ActivityInventoryBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        binding = ActivityInventoryBinding.inflate(getLayoutInflater());
        setupDrawer(binding.getRoot());

        // Ensure ActionBar setup
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Inventory");
        } else {
            Log.e("Inventory", "ActionBar is null!");
        }



        recyclerView = (RecyclerView)findViewById(R.id.rv1);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        FirebaseRecyclerOptions<InventoryModel> options =
                new FirebaseRecyclerOptions.Builder<InventoryModel>()
                        .setQuery(FirebaseDatabase.getInstance().getReference().child("Products"), InventoryModel.class)
                        .build();

        mainAdapter = new MainAdapter(options);
        recyclerView.setAdapter(mainAdapter);

        floatingActionButton = (FloatingActionButton)findViewById(R.id.floatingActionButton);

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(InventoryActivity.this, AddActivity.class));
            }
        });


    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(InventoryActivity.this, MainActivity.class));
        finish();
    }

    /**
     *
     */
    @Override
    protected void onStart() {
        super.onStart();
        mainAdapter.startListening();
    }

    /**
     *
     */
    @Override
    protected void onStop() {
        super.onStop();
        mainAdapter.stopListening();
    }


}
