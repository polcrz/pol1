package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.Model.SalesModel;
import com.example.myapplication.databinding.ActivitySalesBinding;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class SalesActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private SalesAdapter adapter;
    private ImageButton historyBtn;
    private ActivitySalesBinding binding;
    private Button invoice, orderBtn, submitBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySalesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup navigation drawer
        setupDrawer(binding.getRoot());

        // Set ActionBar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Sales");
        } else {
            Log.e("Sales", "ActionBar is null!");
        }

        // Initialize UI components
        orderBtn = findViewById(R.id.orderBtn);
        historyBtn = findViewById(R.id.historyBtn);
        submitBtn = findViewById(R.id.submitBtn);

        // Navigate to Orders screen
        orderBtn.setOnClickListener(v -> startActivity(new Intent(SalesActivity.this, orders.class)));

        // Navigate to Sales Record screen
        historyBtn.setOnClickListener(v -> startActivity(new Intent(SalesActivity.this, SalesRecord.class)));

        // Initialize RecyclerView and set layout manager
        recyclerView = findViewById(R.id.salesRecyclerV);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Declare userUID here only once
        String userUID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Firebase query for user's sales
        DatabaseReference salesRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("users")
                .child(userUID)
                .child("products");

        // Setup FirebaseRecyclerOptions
        FirebaseRecyclerOptions<SalesModel> options = new FirebaseRecyclerOptions.Builder<SalesModel>()
                .setQuery(salesRef, SalesModel.class)
                .build();

        // Initialize adapter
        adapter = new SalesAdapter(options);
        recyclerView.setAdapter(adapter);

        submitBtn.setOnClickListener(v -> {
            HashMap<String, Integer> selectedSales = adapter.getSelectedSales();

            if (selectedSales.isEmpty()) {
                Toast.makeText(this, "Invalid Order.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check for invalid quantities before proceeding
            if (adapter.hasInvalidQuantities()) {
                Toast.makeText(this, "Cannot submit sale with invalid quantities", Toast.LENGTH_SHORT).show();
                return;
            }

            // Do not redefine userUID here, use the one declared earlier
            DatabaseReference userOrdersRef = FirebaseDatabase.getInstance()
                    .getReference()
                    .child("users")
                    .child(userUID)
                    .child("orders") // Saving to the orders node
                    .push(); // Generate unique order ID

            String date = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());
            HashMap<String, Object> orderData = new HashMap<>();
            orderData.put("Date", date);
            orderData.put("Status", "pending"); // Order status as 'pending'

            // To store all products in this order
            HashMap<String, Object> productsOrdered = new HashMap<>();
            final double[] totalOrderPrice = {0}; // Use an array to allow modification within lambda

            for (String key : selectedSales.keySet()) {
                int selectedQuantity = selectedSales.get(key);

                DatabaseReference itemRef = FirebaseDatabase.getInstance()
                        .getReference()
                        .child("users")
                        .child(userUID)
                        .child("products")
                        .child(key);

                itemRef.get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        int currentStock = snapshot.child("Quantity").getValue(Integer.class);

                        if (currentStock >= selectedQuantity) {
                            int newStock = currentStock - selectedQuantity;

                            // Update stock in Firebase
                            HashMap<String, Object> updateMap = new HashMap<>();
                            updateMap.put("Quantity", newStock);
                            itemRef.updateChildren(updateMap);

                            // Add product details to the order
                            double price = snapshot.child("Price").getValue(Double.class);
                            double totalPrice = price * selectedQuantity;
                            totalOrderPrice[0] += totalPrice; // Accumulate the total price

                            HashMap<String, Object> productData = new HashMap<>();
                            productData.put("Product", snapshot.child("Product").getValue(String.class));
                            productData.put("Quantity", selectedQuantity);
                            productData.put("Price", price); // Include the unit price
                            productData.put("TotalPrice", totalPrice);

                            productsOrdered.put(key, productData);

                            // After adding all items, save order data
                            if (productsOrdered.size() == selectedSales.size()) {
                                orderData.put("Products", productsOrdered);
                                orderData.put("TotalOrderPrice", totalOrderPrice[0]); // Add total price
                                userOrdersRef.setValue(orderData); // Save to orders node

                                Toast.makeText(this, "Order submitted successfully.", Toast.LENGTH_SHORT).show();

                                // Navigate to the Orders screen after successful submission
                                Intent intent = new Intent(SalesActivity.this, orders.class);
                                startActivity(intent);
                            }
                        } else {
                            Toast.makeText(this, "Not enough stock for " + snapshot.child("Product").getValue(String.class), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
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