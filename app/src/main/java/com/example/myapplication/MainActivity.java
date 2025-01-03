package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.example.myapplication.Model.ReportActivity;
import com.example.myapplication.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends BaseActivity {

    ActivityMainBinding binding;
    FirebaseAuth auth;
    DatabaseReference databaseReference;
    TextView userTextView; // TextView to display user welcome message
    TextView currentSales; // TextView to display today's total sales
    TextView currentInventory; // TextView to display today's inventory

    CardView cardview, cardview2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        // Set up the root view with the navigation drawer
        setupDrawer(binding.getRoot());

        // Ensure ActionBar setup
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Home");
        } else {
            Log.e("Home", "ActionBar is null!");
        }

        // Initialize Firebase references
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // Find TextViews and CardView for warning
        CardView lowStockCardView = findViewById(R.id.cardwarning);
        TextView warningTextView = findViewById(R.id.warningTextView);

        // Find TextViews for user, current sales, and current inventory
        userTextView = findViewById(R.id.user);
        currentSales = findViewById(R.id.currentSales);
        currentInventory = findViewById(R.id.currentInventory);
        cardview = findViewById(R.id.cardView);
        cardview2 = findViewById(R.id.cardView2);

        cardview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             startActivity(new Intent(MainActivity.this, InventoryActivity.class ));
            }
        });

        cardview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SalesRecord.class ));
            }
        });

        // Fetch and display the current user's name
        fetchCurrentUserDetails();

        // Fetch and display today's total sales
        fetchTodaysTotalSales();

        // Fetch and display today's inventory with low-stock check for Siomai
        fetchTodaysInventory(lowStockCardView, warningTextView);

        // Check the role of the signed-in user
        checkUserRole();

        // Set click listeners for buttons
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

    // Method to fetch and display the current user's details
    private void fetchCurrentUserDetails() {
        String userId = auth.getCurrentUser().getUid();

        databaseReference.child(userId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String userName = snapshot.getValue(String.class);
                    userTextView.setText("Welcome, " + userName); // Set the welcome message
                } else {
                    userTextView.setText("Welcome, User"); // Default message if no name found
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Failed to fetch user name: " + error.getMessage());
            }
        });
    }

    // Method to fetch and display today's total sales from final price (after discounts) for only "completed" orders
    // Method to fetch and display today's total sales from final price (after discounts) for only "completed" orders
    private void fetchTodaysTotalSales() {
        String userId = auth.getCurrentUser().getUid(); // Get current user ID from Firebase Auth

        // Reference to the user's orders in the database
        DatabaseReference ordersRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("orders"); // Referring to the orders node for the user

        // Get today's date in the format that matches the order date format in Firebase
        String todayDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
        Log.d("MainActivity", "Today's date: " + todayDate); // Log today's date

        ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalSales = 0.0;

                // Loop through the orders
                for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                    // Log the entire order snapshot for debugging
                    Log.d("MainActivity", "Order Snapshot: " + orderSnapshot.toString());

                    String orderDate = orderSnapshot.child("Date").getValue(String.class); // Order date
                    String orderStatus = orderSnapshot.child("Status").getValue(String.class); // Order status

                    // Log order details
                    Log.d("MainActivity", "Order Date: " + orderDate + ", Status: " + orderStatus);

                    // Check if the order's date matches today and the status is "completed"
                    if (orderDate != null && orderStatus != null && "completed".equals(orderStatus)) {
                        if (isSameDay(orderDate, todayDate)) {
                            // Access the invoice for the order and get the final price (after any discounts)
                            DataSnapshot invoiceSnapshot = orderSnapshot.child("Invoice");
                            if (invoiceSnapshot.exists()) {
                                Double finalPrice = invoiceSnapshot.child("finalPrice").getValue(Double.class); // Final price after discounts
                                if (finalPrice != null) {
                                    totalSales += finalPrice; // Add the final price to today's total sales
                                    Log.d("MainActivity", "Added final price: PHP " + finalPrice);
                                } else {
                                    Log.w("MainActivity", "No finalPrice found in invoice for order: " + orderSnapshot.getKey());
                                }
                            } else {
                                Log.w("MainActivity", "No invoice found for order: " + orderSnapshot.getKey());
                            }
                        } else {
                            Log.d("MainActivity", "Skipping order: Date does not match today's date.");
                        }
                    }
                }

                // Log the total sales to check if it's computed correctly
                Log.d("MainActivity", "Today's Total Sales: PHP " + totalSales);

                // Update the UI with the total sales value
                currentSales.setText("Today's Sales: PHP " + String.format(Locale.getDefault(), "%.2f", totalSales));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Failed to fetch sales: " + error.getMessage());
                currentSales.setText("Error fetching today's sales");
            }
        });
    }



    private boolean isSameDay(String orderDate, String todayDate) {
        try {
            // Define a date format that ignores time and only compares the date part
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

            // Parse the order date and today's date
            Date orderDateParsed = dateFormat.parse(orderDate);
            Date todayParsed = dateFormat.parse(todayDate);

            if (orderDateParsed != null && todayParsed != null) {
                // Normalize both dates to the same format (date only, without time)
                SimpleDateFormat onlyDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                String orderDateStr = onlyDateFormat.format(orderDateParsed);
                String todayDateStr = onlyDateFormat.format(todayParsed);

                // Compare the date part only
                return orderDateStr.equals(todayDateStr);
            }
        } catch (ParseException e) {
            Log.e("MainActivity", "Date parsing error: " + e.getMessage());
        }
        return false;
    }





    // Method to fetch and display today's inventory
    // Method to fetch and display today's inventory
    private void fetchTodaysInventory(CardView lowStockCardView, TextView warningTextView) {
        String userId = auth.getCurrentUser().getUid();

        DatabaseReference inventoryRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("products");

        inventoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder inventoryBuilder = new StringBuilder();
                StringBuilder warningsBuilder = new StringBuilder(); // To hold all warning messages
                inventoryBuilder.append("Today's Inventory:\n");

                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                    String productName = productSnapshot.child("Product").getValue(String.class);
                    Long productQuantity = productSnapshot.child("Quantity").getValue(Long.class);

                    if (productName != null && productQuantity != null) {
                        inventoryBuilder.append("\n").append("Product: ").append(productName)
                                .append("\nQuantity: ")
                                .append(productQuantity)
                                .append("\n\n");

                        // Check for out of stock products
                        if (productQuantity == 0) {
                            warningsBuilder.append("Out of Stock!\n")
                                    .append("Product: ").append(productName).append(" is out of stock.\n");
                        } else {
                            // Check for Siomai low stock
                            if ("Siomai".equalsIgnoreCase(productName) && productQuantity < 100) {
                                warningsBuilder.append("Warning!\n")
                                        .append("Siomai stock is low (Quantity: ")
                                        .append(productQuantity).append(")\n");
                            }

                            // Check for Gulaman low stock
                            if ("Gulaman".equalsIgnoreCase(productName) && productQuantity < 100) {
                                warningsBuilder.append("Warning!\n")
                                        .append("Gulaman stock is low (Quantity: ")
                                        .append(productQuantity).append(")\n");
                            }
                        }
                    }
                }

                currentInventory.setText(inventoryBuilder.toString());

                // Display warnings if any exist
                if (warningsBuilder.length() > 0) {
                    warningTextView.setText(warningsBuilder.toString().trim());
                    lowStockCardView.setVisibility(View.VISIBLE);
                } else {
                    lowStockCardView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Failed to fetch inventory: " + error.getMessage());
                currentInventory.setText("Error fetching inventory");
            }
        });
    }



    // Method to check the role of the signed-in user
    private void checkUserRole() {
        String userId = auth.getCurrentUser().getUid();

        DatabaseReference userRef = databaseReference.child(userId);
        userRef.child("role").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String role = dataSnapshot.getValue(String.class);
                if ("admin".equals(role) || "superadmin".equals(role)) {
                    Intent intent = new Intent(MainActivity.this, adminSuperAdmin.class);
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("MainActivity", "Failed to fetch user role: " + databaseError.getMessage());
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
        finish();
    }
}
