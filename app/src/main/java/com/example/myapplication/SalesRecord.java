package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivitySalesRecordBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SalesRecord extends BaseActivity {

    private ActivitySalesRecordBinding binding;

    private TextView salesHistoryTextView;
    private TextView dailySalesTextView;
    private TextView weeklySalesTextView;
    private TextView monthlySalesTextView;
    private TextView yearlySalesTextView;

    private double todaySales = 0;
    private double weeklySales = 0;
    private double monthlySales = 0;
    private double yearlySales = 0;

    private List<String> salesRecords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_record);

        // Inflate layout using ViewBinding
        binding = ActivitySalesRecordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupDrawer(binding.getRoot());

        // Set title for ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Sales Record");
        } else {
            Log.e("Records", "ActionBar is null!");
        }

        // Initialize the TextViews
        salesHistoryTextView = findViewById(R.id.salesHistoryTextView);
        dailySalesTextView = findViewById(R.id.dailySalesTextView);
        weeklySalesTextView = findViewById(R.id.weeklySalesTextView);
        monthlySalesTextView = findViewById(R.id.monthlySalesTextView);
        yearlySalesTextView = findViewById(R.id.yearlySalesTextView);

        // Initialize the sales records list
        salesRecords = new ArrayList<>();

        // Fetch and display the invoice details
        fetchCompletedOrders();
    }

    private void fetchCompletedOrders() {
        String userUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(userUID)
                .child("orders");

        // Query for completed orders ordered by timestamp in descending order
        Query completedOrdersQuery = ordersRef.orderByChild("timestamp").limitToLast(100);

        completedOrdersQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Clear the current sales records list
                salesRecords.clear();

                // Iterate through the data from the last to the first to get the latest orders first
                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    String orderId = orderSnapshot.getKey();
                    if (orderId != null) {
                        fetchInvoiceDetails(orderId);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(SalesRecord.this, "Failed to fetch sales history.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchInvoiceDetails(String orderId) {
        String userUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference invoiceRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(userUID)
                .child("orders")
                .child(orderId)
                .child("Invoice");

        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(userUID)
                .child("orders")
                .child(orderId);

        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot orderSnapshot) {
                String date = orderSnapshot.child("Date").getValue(String.class);
                Double finalPriceValue = orderSnapshot.child("Invoice").child("finalPrice").getValue(Double.class);
                double finalPrice = (finalPriceValue != null) ? finalPriceValue : 0.0;

                if (date != null) {
                    String formattedDate = formatDate(date);

                    // Compare and calculate sales
                    if (isToday(date)) todaySales += finalPrice;
                    if (isThisWeek(date)) weeklySales += finalPrice;
                    if (isThisMonth(date)) monthlySales += finalPrice;
                    if (isThisYear(date)) yearlySales += finalPrice;

                    // Now fetch invoice details
                    invoiceRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot invoiceSnapshot) {
                            if (invoiceSnapshot.exists()) {
                                String invoiceDetails = getInvoiceDetails(invoiceSnapshot, formattedDate, orderId);
                                salesRecords.add(invoiceDetails);
                                updateSalesHistory();
                            } else {
                                Log.e("InvoiceDetails", "Invoice not found for orderId: " + orderId);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.e("SalesRecord", "Failed to fetch invoice details.", databaseError.toException());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("SalesRecord", "Failed to fetch order details.", databaseError.toException());
            }
        });
    }

    private String getInvoiceDetails(DataSnapshot invoiceSnapshot, String orderDate, String orderId) {
        // Retrieve invoice details or set default values if missing
        String invoiceNumber = invoiceSnapshot.child("invoiceNumber").getValue(String.class);
        String itemPrices = invoiceSnapshot.child("itemPrices").getValue(String.class);
        String orderDetails = invoiceSnapshot.child("orderDetails").getValue(String.class);
        String vatDetails = invoiceSnapshot.child("vatDetails").getValue(String.class);
        double cashPayment = invoiceSnapshot.child("cashPayment").getValue(Double.class) != null
                ? invoiceSnapshot.child("cashPayment").getValue(Double.class)
                : 0.0;
        double change = invoiceSnapshot.child("change").getValue(Double.class) != null
                ? invoiceSnapshot.child("change").getValue(Double.class)
                : 0.0;
        double finalPrice = invoiceSnapshot.child("finalPrice").getValue(Double.class) != null
                ? invoiceSnapshot.child("finalPrice").getValue(Double.class)
                : 0.0;
        String vendorName = invoiceSnapshot.child("vendorName").getValue(String.class);

        // Retrieve or default PWD/Senior details
        String pwdName = invoiceSnapshot.child("pwdName").getValue(String.class);
        String pwdId = invoiceSnapshot.child("pwdId").getValue(String.class);
        pwdName = (pwdName != null && !pwdName.trim().isEmpty()) ? pwdName : "Not Provided";
        pwdId = (pwdId != null && !pwdId.trim().isEmpty()) ? pwdId : "Not Provided";

        // Retrieve or default discount
        Double discount = invoiceSnapshot.child("discount").getValue(Double.class);
        String discountDetails = (discount != null) ? "Discount: PHP " + String.format("%.2f", discount) : "Discount: PHP 0.00";

        // Build invoice details
        StringBuilder invoiceDetails = new StringBuilder();
        invoiceDetails.append("Date and Time: ").append(orderDate).append("\n")
                .append("Order ID: ").append(orderId).append("\n")
                .append("Invoice ID: ").append(invoiceNumber != null ? invoiceNumber : "Not Available").append("\n")
                .append("Vendor: ").append(vendorName != null ? vendorName : "Not Available").append("\n\n")
                .append("Order Details:\n").append(orderDetails != null ? orderDetails : "No Details").append("\n\n")
                .append("Items:\n").append(itemPrices != null ? itemPrices : "No Items").append("\n\n")
                .append("VAT Details:\n").append(vatDetails != null ? vatDetails : "No VAT Details").append("\n\n")
                .append("Payment Details:\n")
                .append("Cash Payment: PHP ").append(String.format("%.2f", cashPayment)).append("\n")
                .append("Change: PHP ").append(String.format("%.2f", change)).append("\n\n")
                .append("PWD/Senior Details:\n")
                .append("Name: ").append(pwdName).append("\n")
                .append("ID: ").append(pwdId).append("\n\n")
                .append(discountDetails).append("\n")
                .append("Final Price: PHP ").append(String.format("%.2f", finalPrice)).append("\n")
                .append("-----------------------------------------------------------\n\n");

        return invoiceDetails.toString();
    }

    private void updateSalesHistory() {
        // Clear the current text view content
        salesHistoryTextView.setText("");

        // Reverse the sales records list to display from latest to oldest
        Collections.reverse(salesRecords);

        // Append each record to the text view
        for (String record : salesRecords) {
            salesHistoryTextView.append(record);
        }

        // Update sales display
        updateSalesDisplay();
    }

    private void updateSalesDisplay() {
        // Update the TextViews with the calculated sales totals
        dailySalesTextView.setText("Daily Sales: PHP " + String.format("%.2f", todaySales));
        weeklySalesTextView.setText("Weekly Sales: PHP " + String.format("%.2f", weeklySales));
        monthlySalesTextView.setText("Monthly Sales: PHP " + String.format("%.2f", monthlySales));
        yearlySalesTextView.setText("Yearly Sales: PHP " + String.format("%.2f", yearlySales));
    }

    private String formatDate(String date) {
        try {
            // Parse the date stored in Firebase (assuming it is in a suitable format)
            SimpleDateFormat firebaseDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
            Date parsedDate = firebaseDateFormat.parse(date);

            if (parsedDate != null) {
                // Format the date to 12-hour format with AM/PM
                SimpleDateFormat formattedDate = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
                return formattedDate.format(parsedDate);
            }
        } catch (Exception e) {
            Log.e("SalesRecord", "Error parsing date: " + date, e);
        }
        return "Invalid Date";
    }

    private boolean isToday(String date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String orderDate = dateFormat.format(new Date(date));
        String currentDate = dateFormat.format(Calendar.getInstance().getTime());
        return orderDate.equals(currentDate);
    }

    private boolean isThisWeek(String date) {
        // You can implement this logic based on your need
        return true; // Simplified for this example
    }

    private boolean isThisMonth(String date) {
        // You can implement this logic based on your need
        return true; // Simplified for this example
    }

    private boolean isThisYear(String date) {
        // You can implement this logic based on your need
        return true; // Simplified for this example
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(SalesRecord.this, SalesActivity.class));
        finish();
    }
}