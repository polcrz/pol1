package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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

public class adminSuperAdmin extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView salesTextView; // TextView to display total sales
    private TextView inventoryText; // For total inventory display
    private TextView accountsText, reports; // For displaying the total number of accounts


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_super_admin);

        mAuth = FirebaseAuth.getInstance();

        Button logoutButton = findViewById(R.id.logout);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Sign out the user
                mAuth.signOut();

                // Show logout Toast message
                Toast.makeText(adminSuperAdmin.this, "Logged out successfully", Toast.LENGTH_SHORT).show();

                // Clear the activity stack and go to LoginActivity
                Intent intent = new Intent(adminSuperAdmin.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears the current activity stack
                startActivity(intent);

                // Finish the current activity
                finish();
            }
        });



        CardView cardViewReports = findViewById(R.id.reportsCardView);
        reports = findViewById(R.id.reports);
        cardViewReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(adminSuperAdmin.this, AdminsuperReports.class));
            }
        });



        // Initialize CardView and its TextView for sales
        CardView cardViewSales = findViewById(R.id.salesCardView);
        salesTextView = findViewById(R.id.salesText);

        // Initialize the inventory TextView
        inventoryText = findViewById(R.id.inventoryText);
        CardView cardViewInventory = findViewById(R.id.inventoryCardView);

        // Initialize the accounts TextView
        accountsText = findViewById(R.id.accountsText); // Ensure this ID matches the TextView in your XML
        CardView cardViewAccounts = findViewById(R.id.accountsCardView);

        CardView cardViewProfile = findViewById(R.id.myProfile);
        cardViewProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(adminSuperAdmin.this, adminMyProfile.class));
            }
        });

        cardViewAccounts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(adminSuperAdmin.this, Accounts.class));
            }
        });

        cardViewInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(adminSuperAdmin.this, inventoryPopUp.class));
            }
        });

        // Set a click listener on the CardView
        cardViewSales.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle click event, e.g., navigate to another activity
                startActivity(new Intent(adminSuperAdmin.this, salesPopUp.class));
            }
        });

        // Fetch and display today's total sales
        fetchTodaysTotalSales();

        // Fetch the total inventory of products from vendor users
        fetchAllUsersTotalInventory();

        // Fetch and display the total number of accounts
        fetchTotalAccounts();
    }

    // Fetch today's total sales from all vendors
    private void fetchTodaysTotalSales() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                double totalSales = 0;

                // Get today's date
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String todayDate = sdf.format(new Date());

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String role = userSnapshot.child("role").getValue(String.class);
                    if ("vendor".equalsIgnoreCase(role)) { // Process only vendors
                        DataSnapshot ordersSnapshot = userSnapshot.child("orders");

                        for (DataSnapshot orderSnapshot : ordersSnapshot.getChildren()) {
                            String orderStatus = orderSnapshot.child("Status").getValue(String.class);
                            String orderDateStr = orderSnapshot.child("Date").getValue(String.class);

                            if ("completed".equalsIgnoreCase(orderStatus) && orderDateStr != null) {
                                try {
                                    // Parse the order date
                                    SimpleDateFormat orderDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
                                    Date orderDate = orderDateFormat.parse(orderDateStr);

                                    // Check if the order date is today
                                    if (todayDate.equals(sdf.format(orderDate))) {
                                        Double finalPrice = orderSnapshot.child("Invoice").child("finalPrice").getValue(Double.class);
                                        if (finalPrice != null) {
                                            totalSales += finalPrice;
                                        }
                                    }
                                } catch (ParseException e) {
                                    Log.e("adminSuperAdmin", "Error parsing order date: " + e.getMessage(), e);
                                }
                            }
                        }
                    }
                }

                // Update the text in the CardView's TextView for total sales
                salesTextView.setText(String.format("Today's Sales: ₱%.2f", totalSales));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("adminSuperAdmin", "Database error: " + databaseError.getMessage());
                Toast.makeText(adminSuperAdmin.this, "Failed to fetch sales data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Fetch and total the inventory of products from vendor users
    private void fetchAllUsersTotalInventory() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int totalInventory = 0;
                boolean lowInventoryAlert = false;
                StringBuilder alertBuilder = new StringBuilder("Alert! Low inventory:\n");

                // Loop through all users
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    // Check if the user has a 'vendor' role
                    String role = userSnapshot.child("role").getValue(String.class);
                    if ("vendor".equalsIgnoreCase(role)) {
                        String vendorName = userSnapshot.child("name").getValue(String.class);
                        boolean vendorLowInventory = false;
                        StringBuilder vendorAlertBuilder = new StringBuilder();

                        // Loop through the products for this vendor user
                        DataSnapshot productsSnapshot = userSnapshot.child("products");
                        for (DataSnapshot productSnapshot : productsSnapshot.getChildren()) {
                            Integer productQuantity = productSnapshot.child("Quantity").getValue(Integer.class);
                            String productName = productSnapshot.child("Product").getValue(String.class);

                            if (productQuantity != null && productName != null) {
                                totalInventory += productQuantity;

                                // Check for low inventory
                                if (productQuantity < 100) {
                                    vendorLowInventory = true;
                                    lowInventoryAlert = true;
                                    vendorAlertBuilder.append(productName).append(": ").append(productQuantity).append(" pcs\n");
                                }
                            }
                        }

                        if (vendorLowInventory) {
                            alertBuilder.append(vendorName).append(":\n").append(vendorAlertBuilder.toString());
                        }
                    }
                }

                // Update the inventory TextView with the alert message if there's a low inventory alert
                if (lowInventoryAlert) {
                    inventoryText.setText(alertBuilder.toString());
                    inventoryText.setTextColor(getResources().getColor(android.R.color.holo_red_dark)); // Set text color to red
                } else {
                    inventoryText.setText(String.format("Total Inventory: %d pcs", totalInventory));
                    inventoryText.setTextColor(getResources().getColor(android.R.color.black)); // Set text color to black
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("adminSuperAdmin", "Database error: " + databaseError.getMessage());
                Toast.makeText(adminSuperAdmin.this, "Failed to fetch inventory data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Fetch and display the total number of accounts
    private void fetchTotalAccounts() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int accountCount = 0;

                // Count the number of accounts (users)
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    accountCount++;
                }

                // Update the accounts TextView with the total number of accounts
                accountsText.setText(String.format("Total Accounts: %d", accountCount));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("adminSuperAdmin", "Database error: " + databaseError.getMessage());
                Toast.makeText(adminSuperAdmin.this, "Failed to fetch account data.", Toast.LENGTH_SHORT).show();
            }
        });
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