package com.example.myapplication.Model;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.myapplication.BaseActivity;
import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityReportBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ReportActivity extends BaseActivity {

    private ActivityReportBinding binding;
    private DatabaseReference databaseReference; // For Realtime Database
    private TextView textViewSource;            // TextView to display fetched data
    private TextView generate;                 // "Generate" button
    private Spinner mySpinner;                 // Spinner for selecting items

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize binding and setup drawer
        binding = ActivityReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupDrawer(binding.getRoot());

        // Ensure ActionBar setup
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Report");
        } else {
            Log.e("ReportActivity", "ActionBar is null!");
        }

        // Initialize Firebase Realtime Database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("Products");

        // Initialize TextView and Button
        textViewSource = findViewById(R.id.inventoryReport); // Ensure this matches your XML ID
        generate = findViewById(R.id.generate);             // Button to trigger data fetch

        // Initialize Spinner
        mySpinner = findViewById(R.id.mySpinner); // Ensure ID matches XML

        // Set up an adapter for the Spinner (e.g., ArrayAdapter)
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.spinner_items, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mySpinner.setAdapter(adapter);

        // Set onClickListener for the "Generate" button
        generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchAllData(); // Fetch data when button is clicked
            }
        });
    }

    private void fetchAllData() {
        // Attach a listener to read data from the "Products" node
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                StringBuilder data = new StringBuilder(); // To store the fetched data

                // Iterate through all child nodes under "Products"
                for (DataSnapshot productSnapshot : dataSnapshot.getChildren()) {
                    // Extract the "Product" and "Quantity" fields
                    String product = productSnapshot.child("Product").getValue(String.class);
                    Long quantity = productSnapshot.child("Quantity").getValue(Long.class);

                    // Append data to the StringBuilder
                    data.append("Product: ").append(product)
                            .append("\nQuantity: ").append(quantity)
                            .append("\n\n");
                }

                // Display the result in the TextView
                if (data.length() > 0) {
                    textViewSource.setText(data.toString());
                } else {
                    textViewSource.setText("No products found.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle possible errors
                Log.e("ReportActivity", "Error fetching data", databaseError.toException());
                textViewSource.setText("Error: " + databaseError.getMessage());
                Toast.makeText(ReportActivity.this, "Failed to fetch data!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
