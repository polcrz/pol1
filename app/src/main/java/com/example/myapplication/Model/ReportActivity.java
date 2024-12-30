package com.example.myapplication.Model;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.BaseActivity;
import com.example.myapplication.PdfCreator;
import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityReportBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportActivity extends BaseActivity {

    private ActivityReportBinding binding;
    private DatabaseReference databaseReference;
    private DatabaseReference salesDatabaseReference;
    private TextView textViewSalesReport;
    private TextView textViewInventoryReport;
    private TextView generate;
    private TextView saveAsPdfButton;
    private Spinner mySpinner;
    private TextView reportTitle;
    private TextView userNameTextView;

    private TextView salesSum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupDrawer(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Report");
        } else {
            Log.e("Report", "ActionBar is null!");
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("products");
        salesDatabaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("orders");

        // Initialize UI components
        userNameTextView = findViewById(R.id.user);
        textViewSalesReport = findViewById(R.id.salesReport);
        textViewInventoryReport = findViewById(R.id.inventoryReport);
        generate = findViewById(R.id.generate);
        saveAsPdfButton = findViewById(R.id.saveAsPdf);
        reportTitle = findViewById(R.id.reportTitle);
        mySpinner = findViewById(R.id.mySpinner);
        salesSum = findViewById(R.id.salesSum);

        // Fetch the current user's name and display it in the TextView
        fetchUserName();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.spinner_items, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mySpinner.setAdapter(adapter);

        generate.setOnClickListener(v -> {
            String selectedOption = mySpinner.getSelectedItem().toString();

            if (selectedOption.equals("Select Report")) {
                Toast.makeText(ReportActivity.this, "Please select a valid report (Daily, Weekly, Monthly, or Yearly).", Toast.LENGTH_SHORT).show();
            } else {
                reportTitle.setText("Report for: " + getReportPeriod(selectedOption));
                fetchReportData(selectedOption);
                fetchSalesDetails(selectedOption);
            }
        });

        saveAsPdfButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                saveReportAsPDF();
            } else {
                Toast.makeText(ReportActivity.this, "PDF generation is not supported on this Android version.", Toast.LENGTH_SHORT).show();
            }
        });

        checkPermissions();
    }

    // Fetch the user's name from Firebase and display it in the TextView
    private void fetchUserName() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userDatabaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId);

        userDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Fetch the user's name from Firebase
                String userName = dataSnapshot.child("name").getValue(String.class);  // Assuming the user's name is under "name" field

                // Set the user's name to the TextView
                if (userName != null) {
                    userNameTextView.setText(userName);
                } else {
                    userNameTextView.setText("Name not available");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ReportActivity", "Failed to fetch user data", databaseError.toException());
                userNameTextView.setText("Error fetching name");
            }
        });
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void saveReportAsPDF() {
        // Define the file path
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "Report_" + timestamp + ".pdf";
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + fileName;

        // Gather report data
        String reportTitleText = reportTitle.getText().toString();
        String salesReportText = textViewSalesReport.getText().toString();
        String inventoryReportText = textViewInventoryReport.getText().toString();
        String salesSummaryText = salesSum.getText().toString();

        // Create PDF using PdfCreator
        PdfCreator pdfCreator = new PdfCreator();
        pdfCreator.createPdf(filePath, reportTitleText, salesReportText, inventoryReportText, salesSummaryText);

        Toast.makeText(this, "PDF created at " + filePath, Toast.LENGTH_SHORT).show();
    }






    private void fetchReportData(String timeRange) {
        // Fetch inventory data (Product and Quantity) based on the selected time range
        fetchInventoryData(timeRange);

        // Fetch sales data based on the selected option (daily, weekly, monthly, yearly)
        fetchSalesData(timeRange);
    }

    private void fetchInventoryData(String reportType) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                StringBuilder data = new StringBuilder(); // To store the fetched data
                Map<String, Integer> productSales = new HashMap<>(); // To store quantity sold for each product

                // Define the start and end times for the report based on the selected report type
                long currentTimeMillis = System.currentTimeMillis();
                long startTime = getStartTimeForRange(reportType, currentTimeMillis);
                long endTime = getEndTimeForRange(reportType, startTime);

                // Format the dates
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String startDateString = dateFormat.format(new Date(startTime));
                String currentDateString = dateFormat.format(new Date(currentTimeMillis));

                // Add the dates to the report
                data.append("Report Start Date: ").append(startDateString).append("\n");
                data.append("Report Generated On: ").append(currentDateString).append("\n\n");

                // First, fetch all sales data within the specified time range
                salesDatabaseReference.orderByChild("Status").equalTo("completed").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot salesSnapshot) {
                        for (DataSnapshot sale : salesSnapshot.getChildren()) {
                            String date = sale.child("Date").getValue(String.class);
                            if (date == null) continue;

                            try {
                                // Parse the sale date
                                SimpleDateFormat firebaseDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
                                Date saleDateParsed = firebaseDateFormat.parse(date);
                                if (saleDateParsed == null || saleDateParsed.getTime() < startTime || saleDateParsed.getTime() > endTime) {
                                    continue;
                                }

                                DataSnapshot productsSnapshot = sale.child("Products");
                                for (DataSnapshot productSnapshot : productsSnapshot.getChildren()) {
                                    String productName = productSnapshot.child("Product").getValue(String.class);
                                    Long quantitySoldLong = productSnapshot.child("Quantity").getValue(Long.class); // Extract Quantity as Long
                                    int quantitySold = (quantitySoldLong != null) ? quantitySoldLong.intValue() : 0;

                                    if (productName != null) {
                                        int currentQuantitySold = productSales.getOrDefault(productName, 0);
                                        productSales.put(productName, currentQuantitySold + quantitySold);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("ReportActivity", "Error parsing date for entry: " + sale.getKey(), e);
                            }
                        }

                        // Now, fetch inventory data and join with sales data
                        for (DataSnapshot productSnapshot : dataSnapshot.getChildren()) {
                            InventoryModel inventory = productSnapshot.getValue(InventoryModel.class);

                            if (inventory != null) {
                                data.append("Product: ").append(inventory.getProduct()).append("\n");
                                data.append("Price: ").append(inventory.getPrice()).append("\n");
                                data.append("Quantity in Stock: ").append(inventory.getQuantity()).append(" pc/s").append("\n");
                                int totalSold = productSales.getOrDefault(inventory.getProduct(), 0);
                                data.append("Quantity Sold: ").append(totalSold).append(" pc/s").append("\n");
                                data.append("---------------------------------\n\n");
                            } else {
                                data.append("Error fetching product data\n\n");
                            }
                        }

                        if (data.length() > 0) {
                            textViewInventoryReport.setText(data.toString());
                        } else {
                            textViewInventoryReport.setText("No inventory data found.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("ReportActivity", "Error fetching sales data", databaseError.toException());
                        Toast.makeText(ReportActivity.this, "Failed to fetch sales data!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ReportActivity", "Error fetching inventory data", databaseError.toException());
                textViewInventoryReport.setText("Error: " + databaseError.getMessage());
                Toast.makeText(ReportActivity.this, "Failed to fetch inventory data!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper method to determine the period for the sale (day, week, month, or year)
    private String getPeriodForSale(String reportType, Date saleDate) {
        SimpleDateFormat periodFormat;

        switch (reportType) {
            case "Daily Report":
                periodFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()); // e.g., "Dec 26, 2024"
                return periodFormat.format(saleDate);
            case "Weekly Report":
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(saleDate);
                int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
                int year = calendar.get(Calendar.YEAR);
                return "Week " + weekOfYear + ", " + year;
            case "Monthly Report":
                periodFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()); // e.g., "December 2024"
                return periodFormat.format(saleDate);
            case "Yearly Report":
                periodFormat = new SimpleDateFormat("yyyy", Locale.getDefault()); // e.g., "2024"
                return periodFormat.format(saleDate);
            default:
                throw new IllegalArgumentException("Invalid report type");
        }
    }


    // Fetch sales details based on the selected report type
    private void fetchVendorSalesData(String reportType) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                StringBuilder salesReport = new StringBuilder();
                double totalSales = 0.0;
                double totalDiscountAmount = 0.0;
                int totalDiscountOrderCount = 0;

                long currentTimeMillis = System.currentTimeMillis();
                long startTime = getStartTimeForRange(reportType, currentTimeMillis);
                long endTime = getEndTimeForRange(reportType, startTime);

                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String startDateString = dateFormat.format(new Date(startTime));
                String endDateString = dateFormat.format(new Date(endTime));
                String currentDateString = dateFormat.format(new Date(currentTimeMillis));

                salesReport.append("Report Start Date: ").append(startDateString).append("\n");
                if (!reportType.equals("Daily Report")) {
                    salesReport.append("Report End Date: ").append(endDateString).append("\n");
                }
                salesReport.append("Report Generated On: ").append(currentDateString).append("\n\n");

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String role = userSnapshot.child("role").getValue(String.class);
                    if ("vendor".equalsIgnoreCase(role)) {
                        String vendorName = userSnapshot.child("name").getValue(String.class);
                        salesReport.append("Vendor: ").append(vendorName != null ? vendorName : "Unknown").append("\n");

                        DataSnapshot ordersSnapshot = userSnapshot.child("orders");
                        double vendorTotalSales = 0.0;
                        double vendorDiscountAmount = 0.0;
                        int vendorDiscountOrderCount = 0;
                        Map<String, Integer> productSales = new HashMap<>();

                        for (DataSnapshot orderSnapshot : ordersSnapshot.getChildren()) {
                            String orderStatus = orderSnapshot.child("Status").getValue(String.class);
                            String orderDateStr = orderSnapshot.child("Date").getValue(String.class);

                            if ("completed".equalsIgnoreCase(orderStatus) && orderDateStr != null) {
                                try {
                                    SimpleDateFormat orderDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
                                    Date orderDate = orderDateFormat.parse(orderDateStr);

                                    if (orderDate != null && orderDate.getTime() >= startTime && orderDate.getTime() <= endTime) {
                                        Double finalPrice = orderSnapshot.child("Invoice").child("finalPrice").getValue(Double.class);
                                        Double discount = orderSnapshot.child("Invoice").child("discount").getValue(Double.class);

                                        if (finalPrice != null) {
                                            vendorTotalSales += finalPrice;
                                        }
                                        if (discount != null && discount > 0) {
                                            vendorDiscountAmount += discount;
                                            vendorDiscountOrderCount++;
                                        }

                                        for (DataSnapshot productSnapshot : orderSnapshot.child("Products").getChildren()) {
                                            String productName = productSnapshot.child("Product").getValue(String.class);
                                            Integer quantitySold = productSnapshot.child("Quantity").getValue(Integer.class);

                                            if (productName != null && quantitySold != null) {
                                                int currentQuantity = productSales.getOrDefault(productName, 0);
                                                productSales.put(productName, currentQuantity + quantitySold);
                                            }
                                        }
                                    }
                                } catch (ParseException e) {
                                    Log.e("ReportActivity", "Error parsing order date: " + e.getMessage(), e);
                                }
                            }
                        }

                        salesReport.append("  Total Sales: ₱").append(String.format(Locale.getDefault(), "%.2f", vendorTotalSales)).append("\n");

                        if (!productSales.isEmpty()) {
                            salesReport.append("  Products Sold:\n");
                            for (Map.Entry<String, Integer> productEntry : productSales.entrySet()) {
                                salesReport.append("    - ").append(productEntry.getKey())
                                        .append(": ").append(productEntry.getValue()).append(" pcs\n");
                            }
                        }

                        salesReport.append("  Discount Information:\n")
                                .append("    Number of Orders with Discount: ").append(vendorDiscountOrderCount).append("\n")
                                .append("    Total Discount Amount: ₱").append(String.format(Locale.getDefault(), "%.2f", vendorDiscountAmount)).append("\n\n");

                        totalSales += vendorTotalSales;
                        totalDiscountAmount += vendorDiscountAmount;
                        totalDiscountOrderCount += vendorDiscountOrderCount;
                    }
                }

                salesReport.append("Total Discount Information:\n")
                        .append("  Number of Orders with Discount: ").append(totalDiscountOrderCount).append("\n")
                        .append("  Total Discount Amount: ₱").append(String.format(Locale.getDefault(), "%.2f", totalDiscountAmount)).append("\n\n");
                salesReport.append("Total Sales: ₱").append(String.format(Locale.getDefault(), "%.2f", totalSales)).append("\n");

                // Display the sales report
                textViewSalesReport.setText(salesReport.toString());

                // Generate detailed breakdowns for other report types
                if (reportType.equals("Daily Report")) {
                    generateDailyBreakdown(salesReport, dataSnapshot, startTime, endTime);
                } else if (reportType.equals("Weekly Report")) {
                    generateWeeklyBreakdown(salesReport, dataSnapshot, startTime, endTime);
                } else if (reportType.equals("Monthly Report")) {
                    generateMonthlyBreakdown(salesReport, dataSnapshot, startTime, endTime);
                } else if (reportType.equals("Yearly Report")) {
                    generateYearlyBreakdown(salesReport, dataSnapshot, startTime, endTime);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ReportActivity", "Database error: " + databaseError.getMessage());
                Toast.makeText(ReportActivity.this, "Failed to fetch sales data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchSalesDetails(String reportType) {
        long currentTimeMillis = System.currentTimeMillis();
        long startTime = getStartTimeForRange(reportType, currentTimeMillis);
        long endTime = getEndTimeForRange(reportType, startTime);

        salesDatabaseReference.orderByChild("Status").equalTo("completed")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot salesSnapshot) {
                        StringBuilder salesData = new StringBuilder();

                        for (DataSnapshot sale : salesSnapshot.getChildren()) {
                            processSaleForDetails(sale, startTime, endTime, reportType, salesData);
                        }

                        displaySalesDetails(salesData, reportType);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("ReportActivity", "Error fetching sales data", databaseError.toException());
                        salesSum.setText("Failed to fetch sales details!");
                    }
                });
    }

    private void processSaleForDetails(DataSnapshot sale, long startTime, long endTime, String reportType, StringBuilder salesData) {
        String date = sale.child("Date").getValue(String.class);
        if (date == null) {
            Log.w("ReportActivity", "Date is null for sale: " + sale.getKey());
            return;
        }

        try {
            SimpleDateFormat firebaseDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.US);
            Date saleDateParsed = firebaseDateFormat.parse(date);
            if (saleDateParsed == null || saleDateParsed.getTime() < startTime || saleDateParsed.getTime() > endTime) {
                Log.w("ReportActivity", "Sale date is out of range or could not be parsed for sale: " + sale.getKey());
                return;
            }

            // Extract details from the Invoice node
            String orderId = sale.child("Invoice").child("orderId").getValue(String.class);
            String invoiceNumber = sale.child("Invoice").child("invoiceNumber").getValue(String.class);
            String vendorName = sale.child("Invoice").child("vendorName").getValue(String.class);
            String orderDetails = sale.child("Invoice").child("orderDetails").getValue(String.class);
            Double totalPrice = sale.child("Invoice").child("totalPrice").getValue(Double.class);
            Double discount = sale.child("Invoice").child("discount").getValue(Double.class);
            String pwdName = sale.child("Invoice").child("pwdName").getValue(String.class);
            String pwdId = sale.child("Invoice").child("pwdId").getValue(String.class);
            Double finalPrice = sale.child("Invoice").child("finalPrice").getValue(Double.class);

            // Add details to summary
            salesData.append("Order ID: ").append(orderId != null ? orderId : "N/A").append("\n");
            salesData.append("Invoice Number: ").append(invoiceNumber != null ? invoiceNumber : "N/A").append("\n");
            salesData.append("Date: ").append(date).append("\n");
            salesData.append("Vendor Name: ").append(vendorName != null ? vendorName : "N/A").append("\n");
            salesData.append("Order Details: ").append(orderDetails != null ? orderDetails : "N/A").append("\n");
            salesData.append("Total Price: ₱").append(totalPrice != null ? String.format(Locale.getDefault(), "%.2f", totalPrice) : "0.00").append("\n");

            // Check and add discount details if available
            if (discount != null && discount > 0) {
                salesData.append("Discount: ₱").append(String.format(Locale.getDefault(), "%.2f", discount)).append("\n");
                salesData.append("PWD/Senior Name: ").append(pwdName != null ? pwdName : "N/A").append("\n");
                salesData.append("PWD/Senior ID Number: ").append(pwdId != null ? pwdId : "N/A").append("\n");
                salesData.append("Final Price: ₱").append(finalPrice != null ? String.format(Locale.getDefault(), "%.2f", finalPrice) : "0.00").append("\n");
            }

            salesData.append("--------------------------------------\n\n");

        } catch (ParseException e) {
            Log.e("ReportActivity", "Error parsing date for sale: " + sale.getKey(), e);
        } catch (Exception e) {
            Log.e("ReportActivity", "Error processing sale for details: " + sale.getKey(), e);
        }
    }

    private void generateDailyBreakdown(StringBuilder salesReport, DataSnapshot dataSnapshot, long startTime, long endTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE MMM dd, yyyy", Locale.getDefault());

        while (calendar.getTimeInMillis() <= endTime) {
            long dayStartTime = calendar.getTimeInMillis();
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            long dayEndTime = calendar.getTimeInMillis() - 1;

            salesReport.append("\nDaily Breakdown (").append(dayFormat.format(new Date(dayStartTime))).append("):\n");

            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                String role = userSnapshot.child("role").getValue(String.class);
                if ("vendor".equalsIgnoreCase(role)) {
                    String vendorName = userSnapshot.child("name").getValue(String.class);
                    salesReport.append("Vendor: ").append(vendorName != null ? vendorName : "Unknown").append("\n");

                    DataSnapshot ordersSnapshot = userSnapshot.child("orders");
                    double vendorDailySales = 0.0;

                    for (DataSnapshot orderSnapshot : ordersSnapshot.getChildren()) {
                        String orderStatus = orderSnapshot.child("Status").getValue(String.class);
                        String orderDateStr = orderSnapshot.child("Date").getValue(String.class);

                        if ("completed".equalsIgnoreCase(orderStatus) && orderDateStr != null) {
                            try {
                                SimpleDateFormat orderDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
                                Date orderDate = orderDateFormat.parse(orderDateStr);

                                if (orderDate != null && orderDate.getTime() >= dayStartTime && orderDate.getTime() <= dayEndTime) {
                                    Double finalPrice = orderSnapshot.child("Invoice").child("finalPrice").getValue(Double.class);

                                    if (finalPrice != null) {
                                        vendorDailySales += finalPrice;
                                    }
                                }
                            } catch (ParseException e) {
                                Log.e("ReportActivity", "Error parsing order date: " + e.getMessage(), e);
                            }
                        }
                    }

                    salesReport.append("  Daily Sales: ₱").append(String.format(Locale.getDefault(), "%.2f", vendorDailySales)).append("\n");
                }
            }
        }
    }
    private void generateMonthlyBreakdown(StringBuilder salesReport, DataSnapshot dataSnapshot, long startTime, long endTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        while (calendar.getTimeInMillis() <= endTime) {
            long monthStartTime = calendar.getTimeInMillis();
            calendar.add(Calendar.MONTH, 1);
            long monthEndTime = calendar.getTimeInMillis() - 1;

            salesReport.append("\nMonthly Breakdown (").append(monthFormat.format(new Date(monthStartTime))).append("):\n");

            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                String role = userSnapshot.child("role").getValue(String.class);
                if ("vendor".equalsIgnoreCase(role)) {
                    String vendorName = userSnapshot.child("name").getValue(String.class);
                    salesReport.append("Vendor: ").append(vendorName != null ? vendorName : "Unknown").append("\n");

                    DataSnapshot ordersSnapshot = userSnapshot.child("orders");
                    double vendorMonthlySales = 0.0;

                    for (DataSnapshot orderSnapshot : ordersSnapshot.getChildren()) {
                        String orderStatus = orderSnapshot.child("Status").getValue(String.class);
                        String orderDateStr = orderSnapshot.child("Date").getValue(String.class);

                        if ("completed".equalsIgnoreCase(orderStatus) && orderDateStr != null) {
                            try {
                                SimpleDateFormat orderDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
                                Date orderDate = orderDateFormat.parse(orderDateStr);

                                if (orderDate != null && orderDate.getTime() >= monthStartTime && orderDate.getTime() <= monthEndTime) {
                                    Double finalPrice = orderSnapshot.child("Invoice").child("finalPrice").getValue(Double.class);

                                    if (finalPrice != null) {
                                        vendorMonthlySales += finalPrice;
                                    }
                                }
                            } catch (ParseException e) {
                                Log.e("ReportActivity", "Error parsing order date: " + e.getMessage(), e);
                            }
                        }
                    }

                    salesReport.append("  Monthly Sales: ₱").append(String.format(Locale.getDefault(), "%.2f", vendorMonthlySales)).append("\n");
                }
            }
        }
    }
    private void generateWeeklyBreakdown(StringBuilder salesReport, DataSnapshot dataSnapshot, long startTime, long endTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        SimpleDateFormat weekFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE MMM dd, yyyy", Locale.getDefault());

        while (calendar.getTimeInMillis() <= endTime) {
            long weekStartTime = calendar.getTimeInMillis();
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
            long weekEndTime = calendar.getTimeInMillis() - 1;

            salesReport.append("\nWeekly Breakdown (").append(weekFormat.format(new Date(weekStartTime)))
                    .append(" - ").append(weekFormat.format(new Date(weekEndTime))).append("):\n");

            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                String role = userSnapshot.child("role").getValue(String.class);
                if ("vendor".equalsIgnoreCase(role)) {
                    String vendorName = userSnapshot.child("name").getValue(String.class);
                    salesReport.append("Vendor: ").append(vendorName != null ? vendorName : "Unknown").append("\n");

                    DataSnapshot ordersSnapshot = userSnapshot.child("orders");
                    Map<String, Double> dailySales = new HashMap<>();

                    for (DataSnapshot orderSnapshot : ordersSnapshot.getChildren()) {
                        String orderStatus = orderSnapshot.child("Status").getValue(String.class);
                        String orderDateStr = orderSnapshot.child("Date").getValue(String.class);

                        if ("completed".equalsIgnoreCase(orderStatus) && orderDateStr != null) {
                            try {
                                SimpleDateFormat orderDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
                                Date orderDate = orderDateFormat.parse(orderDateStr);

                                if (orderDate != null && orderDate.getTime() >= weekStartTime && orderDate.getTime() <= weekEndTime) {
                                    Double finalPrice = orderSnapshot.child("Invoice").child("finalPrice").getValue(Double.class);

                                    if (finalPrice != null) {
                                        String dayOfWeek = dayFormat.format(orderDate);
                                        dailySales.put(dayOfWeek, dailySales.getOrDefault(dayOfWeek, 0.0) + finalPrice);
                                    }
                                }
                            } catch (ParseException e) {
                                Log.e("ReportActivity", "Error parsing order date: " + e.getMessage(), e);
                            }
                        }
                    }

                    for (Map.Entry<String, Double> entry : dailySales.entrySet()) {
                        salesReport.append("  ").append(entry.getKey()).append(": ₱")
                                .append(String.format(Locale.getDefault(), "%.2f", entry.getValue())).append("\n");
                    }
                }
            }
        }
    }

    private void generateYearlyBreakdown(StringBuilder salesReport, DataSnapshot dataSnapshot, long startTime, long endTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        while (calendar.getTimeInMillis() <= endTime) {
            long monthStartTime = calendar.getTimeInMillis();
            calendar.add(Calendar.MONTH, 1);
            long monthEndTime = calendar.getTimeInMillis() - 1;

            salesReport.append("\nMonthly Breakdown (").append(monthFormat.format(new Date(monthStartTime))).append("):\n");

            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                String role = userSnapshot.child("role").getValue(String.class);
                if ("vendor".equalsIgnoreCase(role)) {
                    String vendorName = userSnapshot.child("name").getValue(String.class);
                    salesReport.append("Vendor: ").append(vendorName != null ? vendorName : "Unknown").append("\n");

                    DataSnapshot ordersSnapshot = userSnapshot.child("orders");
                    double vendorMonthlySales = 0.0;

                    for (DataSnapshot orderSnapshot : ordersSnapshot.getChildren()) {
                        String orderStatus = orderSnapshot.child("Status").getValue(String.class);
                        String orderDateStr = orderSnapshot.child("Date").getValue(String.class);

                        if ("completed".equalsIgnoreCase(orderStatus) && orderDateStr != null) {
                            try {
                                SimpleDateFormat orderDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
                                Date orderDate = orderDateFormat.parse(orderDateStr);

                                if (orderDate != null && orderDate.getTime() >= monthStartTime && orderDate.getTime() <= monthEndTime) {
                                    Double finalPrice = orderSnapshot.child("Invoice").child("finalPrice").getValue(Double.class);

                                    if (finalPrice != null) {
                                        vendorMonthlySales += finalPrice;
                                    }
                                }
                            } catch (ParseException e) {
                                Log.e("ReportActivity", "Error parsing order date: " + e.getMessage(), e);
                            }
                        }
                    }

                    salesReport.append("  Monthly Sales: ₱").append(String.format(Locale.getDefault(), "%.2f", vendorMonthlySales)).append("\n");
                }
            }
        }
    }

    // Process each sale to extract the required details


    // Display the sales details in the TextView
    private void displaySalesDetails(StringBuilder salesData, String reportType) {
        if (salesData.length() == 0) {
            salesSum.setText("No completed sales found for " + reportType + " report.");
        } else {
            salesSum.setText(reportType + " Sales Details:\n\n" + salesData.toString());
        }
    }




    private void fetchSalesData(String timeRange) {
        salesDatabaseReference.orderByChild("Status").equalTo("completed").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                StringBuilder salesReport = new StringBuilder();
                double totalSalesAmount = 0;    // Track total sales amount
                int totalDiscountOrderCount = 0;   // Track number of orders with discounts
                double totalDiscountAmount = 0; // Track total discount amount

                // Initialize a map to hold sales amounts for each period
                Map<String, Double> periodSales = new LinkedHashMap<>();
                // Initialize a map to hold product sales data for each period
                Map<String, Map<String, Integer>> periodProductSales = new LinkedHashMap<>();
                // Initialize a map to hold discount information for each period
                Map<String, Integer> periodDiscountOrderCount = new LinkedHashMap<>();
                Map<String, Double> periodDiscountAmount = new LinkedHashMap<>();
                // Initialize a map to hold total product sales data
                Map<String, Integer> totalProductSales = new LinkedHashMap<>();
                SimpleDateFormat periodFormat; // For formatting periods based on time range
                String reportTitle = "";

                long currentTimeMillis = System.currentTimeMillis();
                long startTime = getStartTimeForRange(timeRange, currentTimeMillis);
                long endTime = getEndTimeForRange(timeRange, startTime);

                switch (timeRange) {
                    case "Daily Report":
                        periodFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()); // e.g., "Dec 29, 2024"
                        reportTitle = "Daily Sales Report for " + periodFormat.format(new Date(startTime));
                        break;
                    case "Weekly Report":
                        periodFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()); // e.g., "Dec 23, 2024 - Dec 29, 2024"
                        reportTitle = "Weekly Sales Report";
                        break;
                    case "Monthly Report":
                        periodFormat = new SimpleDateFormat("MMM dd", Locale.getDefault()); // e.g., "Dec 01 - Dec 07"
                        reportTitle = "Monthly Sales Report for December 2024";
                        break;
                    case "Yearly Report":
                        periodFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()); // e.g., "December 2024"
                        reportTitle = "Yearly Sales Report for 2024";
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid report type");
                }

                if (timeRange.equals("Monthly Report")) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(startTime);
                    int weekNumber = 1;

                    while (calendar.getTimeInMillis() <= endTime) {
                        Calendar weekStart = (Calendar) calendar.clone();
                        Calendar weekEnd = (Calendar) calendar.clone();
                        weekEnd.add(Calendar.DAY_OF_YEAR, 6); // End of the week

                        if (weekEnd.getTimeInMillis() > endTime) {
                            weekEnd.setTimeInMillis(endTime); // Adjust the end to the last day of the month
                        }

                        String weekPeriod = "Week " + weekNumber + " (" + periodFormat.format(weekStart.getTime()) + " - " + periodFormat.format(weekEnd.getTime()) + ")";
                        periodSales.put(weekPeriod, 0.0);
                        periodProductSales.put(weekPeriod, new HashMap<>());
                        periodDiscountOrderCount.put(weekPeriod, 0);
                        periodDiscountAmount.put(weekPeriod, 0.0);

                        // Move to the next week
                        calendar.add(Calendar.WEEK_OF_YEAR, 1);
                        weekNumber++;
                    }
                }

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String date = snapshot.child("Date").getValue(String.class);
                    Double finalPrice = null;
                    Double discount = null;

                    // Handle Invoice as a HashMap
                    DataSnapshot invoiceSnapshot = snapshot.child("Invoice");
                    if (invoiceSnapshot.exists()) {
                        finalPrice = invoiceSnapshot.child("finalPrice").getValue(Double.class);
                        discount = invoiceSnapshot.child("discount").getValue(Double.class);
                    }

                    if (date == null || finalPrice == null) {
                        Log.e("ReportActivity", "Invalid data entry: " + snapshot.getKey());
                        continue;
                    }

                    try {
                        // Parse the sale date
                        SimpleDateFormat firebaseDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
                        Date saleDate = firebaseDateFormat.parse(date);

                        if (saleDate != null && saleDate.getTime() >= startTime && saleDate.getTime() <= endTime) {
                            // Get the period for the sale date
                            String period = "";
                            if (timeRange.equals("Monthly Report")) {
                                Calendar saleCalendar = Calendar.getInstance();
                                saleCalendar.setTime(saleDate);
                                for (String weekPeriod : periodSales.keySet()) {
                                    if (weekPeriod.contains(periodFormat.format(saleCalendar.getTime()))) {
                                        period = weekPeriod;
                                        break;
                                    }
                                }
                            } else {
                                period = periodFormat.format(saleDate);
                            }

                            if (!period.isEmpty()) {
                                // Update period sales amount
                                double currentSalesAmount = periodSales.getOrDefault(period, 0.0);
                                periodSales.put(period, currentSalesAmount + finalPrice);

                                // Update product sales data for the period
                                if (!periodProductSales.containsKey(period)) {
                                    periodProductSales.put(period, new HashMap<>());
                                }
                                Map<String, Integer> productSales = periodProductSales.get(period);
                                for (DataSnapshot productSnapshot : snapshot.child("Products").getChildren()) {
                                    String productName = productSnapshot.child("Product").getValue(String.class);
                                    Integer quantitySold = productSnapshot.child("Quantity").getValue(Integer.class);
                                    if (productName != null && quantitySold != null) {
                                        // Update period product sales
                                        int currentQuantity = productSales.getOrDefault(productName, 0);
                                        productSales.put(productName, currentQuantity + quantitySold);

                                        // Update total product sales
                                        int totalQuantity = totalProductSales.getOrDefault(productName, 0);
                                        totalProductSales.put(productName, totalQuantity + quantitySold);
                                    }
                                }

                                // Update period discount information
                                if (discount != null && discount > 0) {
                                    int currentDiscountOrderCount = periodDiscountOrderCount.getOrDefault(period, 0);
                                    double currentDiscountAmount = periodDiscountAmount.getOrDefault(period, 0.0);
                                    periodDiscountOrderCount.put(period, currentDiscountOrderCount + 1);
                                    periodDiscountAmount.put(period, currentDiscountAmount + discount);

                                    // Update total discount information
                                    totalDiscountOrderCount++;
                                    totalDiscountAmount += discount;
                                }
                            }

                            // Update total sales amount
                            totalSalesAmount += finalPrice;
                        }
                    } catch (Exception e) {
                        Log.e("ReportActivity", "Error parsing date for entry: " + snapshot.getKey(), e);
                    }
                }

                // Generate period sales report and include the exact date before the day of sales
                salesReport.append(reportTitle).append("\n\n");
                for (Map.Entry<String, Double> entry : periodSales.entrySet()) {
                    salesReport.append("Period: ").append(entry.getKey()).append("\n");
                    salesReport.append("Sales: ₱").append(String.format("%.2f", entry.getValue()))
                            .append("\n\n");

                    // Include product sales data for the period
                    Map<String, Integer> productSales = periodProductSales.get(entry.getKey());
                    if (productSales != null && !productSales.isEmpty()) {
                        salesReport.append("Products Sold:\n\n");
                        for (Map.Entry<String, Integer> productEntry : productSales.entrySet()) {
                            salesReport.append("Product: ").append(productEntry.getKey()).append("\nQuantity Sold: ").append(productEntry.getValue()).append(" pc/s").append("\n\n");
                        }
                    }

                    // Include discount information for the period
                    salesReport.append("Discount Information:\n")
                            .append("Number of Orders with Discount: ").append(periodDiscountOrderCount.get(entry.getKey())).append("\n")
                            .append("Total Discount Amount: ₱").append(String.format("%.2f", periodDiscountAmount.get(entry.getKey()))).append("\n")
                            .append("---------------------------------\n\n");
                }

                // Include total product sales before the overall total sales
                salesReport.append("Total Products Sold:\n");
                for (Map.Entry<String, Integer> entry : totalProductSales.entrySet()) {
                    salesReport.append("Product: ").append(entry.getKey()).append("\nQuantity Sold: ").append(entry.getValue()).append(" pc/s").append("\n\n");
                }
                salesReport.append("---------------------------------\n\n");

                // Include the discount information before the total sales
                salesReport.append("Total Discount Information:\n")
                        .append("Number of Orders with Discount: ").append(totalDiscountOrderCount).append("\n")
                        .append("Total Discount Amount: ₱").append(String.format("%.2f", totalDiscountAmount)).append("\n")
                        .append("---------------------------------\n\n");

                // Include the overall total at the end of the report
                salesReport.append("Total Sales: ₱").append(String.format("%.2f", totalSalesAmount))
                        .append("\n---------------------------------");

                // Display the report
                textViewSalesReport.setText(salesReport.length() > 0 ? salesReport.toString() : "No sales data found for this period.");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ReportActivity", "Failed to fetch sales data", databaseError.toException());
                Toast.makeText(ReportActivity.this, "Failed to fetch sales data!", Toast.LENGTH_SHORT).show();
            }
        });
    }



    // Helper method to calculate start time for the selected time range (daily, weekly, etc.)
    private long getStartTimeForRange(String timeRange, long currentTimeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTimeMillis);

        switch (timeRange) {
            case "Daily Report":
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            case "Weekly Report":
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            case "Monthly Report":
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            case "Yearly Report":
                calendar.set(Calendar.DAY_OF_YEAR, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            default:
                throw new IllegalArgumentException("Invalid report type");
        }
        return calendar.getTimeInMillis();
    }

    // Helper method to calculate end time for the selected time range (daily, weekly, etc.)
    private long getEndTimeForRange(String timeRange, long startTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);

        switch (timeRange) {
            case "Daily Report":
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                break;
            case "Weekly Report":
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case "Monthly Report":
                calendar.add(Calendar.MONTH, 1);
                break;
            case "Yearly Report":
                calendar.add(Calendar.YEAR, 1);
                break;
            default:
                throw new IllegalArgumentException("Invalid report type");
        }
        calendar.add(Calendar.MILLISECOND, -1); // Subtract one millisecond to get the end time
        return calendar.getTimeInMillis();
    }

    // Helper method to get the report period based on the selected range
    private String getReportPeriod(String timeRange) {
        Calendar calendar = Calendar.getInstance();
        String reportPeriod = "";

        switch (timeRange) {
            case "Daily Report":
                SimpleDateFormat dailyFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                reportPeriod = dailyFormat.format(calendar.getTime());
                break;
            case "Weekly Report":
                // Get the start of the week (Sunday)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                SimpleDateFormat weekFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String startOfWeek = weekFormat.format(calendar.getTime());

                // Get the end of the week (Saturday)
                calendar.add(Calendar.DAY_OF_WEEK, 6);  // Move 6 days ahead to get Saturday
                String endOfWeek = weekFormat.format(calendar.getTime());

                // Combine both start and end dates
                reportPeriod = "Week of " + startOfWeek + " - " + endOfWeek;
                break;
            case "Monthly Report":
                SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                reportPeriod = monthFormat.format(calendar.getTime());
                break;
            case "Yearly Report":
                SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
                reportPeriod = "Year " + yearFormat.format(calendar.getTime());
                break;
            default:
                throw new IllegalArgumentException("Invalid report type");
        }
        return reportPeriod;
    }

    // Product sales data structure
    public static class ProductSalesData {
        double totalQuantitySold;
        double totalSalesAmount;

        public ProductSalesData(double totalQuantitySold, double totalSalesAmount) {
            this.totalQuantitySold = totalQuantitySold;
            this.totalSalesAmount = totalSalesAmount;
        }
    }
}