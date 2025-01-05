package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.Model.ReportActivity;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminsuperReports extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private TextView textViewSalesReport;
    private TextView salesSum;
    private TextView generate;
    private Spinner mySpinner;
    private TextView reportTitle;
    private TextView inventoryReport;
    private Button saveAsPdfButton, backBTN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adminsuper_reports);

        // Initialize UI components
        textViewSalesReport = findViewById(R.id.salesReport);
        salesSum = findViewById(R.id.salesSum);
        generate = findViewById(R.id.generate);
        reportTitle = findViewById(R.id.reportTitle);
        mySpinner = findViewById(R.id.mySpinner);
        inventoryReport = findViewById(R.id.inventoryReport); // Initialize the new TextView
        saveAsPdfButton = findViewById(R.id.saveAsPdf);
        backBTN = findViewById(R.id.backBTN);

        backBTN.setOnClickListener(v -> finish());

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.spinner_items, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mySpinner.setAdapter(adapter);

        generate.setOnClickListener(v -> {
            String selectedOption = mySpinner.getSelectedItem().toString();

            if (selectedOption.equals("Select Report")) {
                Toast.makeText(AdminsuperReports.this, "Please select a valid report (Daily, Weekly, Monthly, or Yearly).", Toast.LENGTH_SHORT).show();
            } else {
                reportTitle.setText("Report for: \n" + getReportPeriod(selectedOption));
                fetchVendorSalesData(selectedOption);
                generateInventoryReport(selectedOption); // Pass selectedOption
                generateInvoiceReport(selectedOption);
            }
        });

        saveAsPdfButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                saveReportAsPDF();
            } else {
                Toast.makeText(AdminsuperReports.this, "PDF generation is not supported on this Android version.", Toast.LENGTH_SHORT).show();
            }
        });

        checkPermissions();
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
        String inventoryReportText = inventoryReport.getText().toString();
        String salesSummaryText = salesSum.getText().toString();

        // Create PDF using PdfCreator
        PdfCreator pdfCreator = new PdfCreator();
        pdfCreator.createPdf(filePath, reportTitleText, salesReportText, inventoryReportText, salesSummaryText);

        Toast.makeText(this, "PDF created at " + filePath, Toast.LENGTH_SHORT).show();
    }

    private List<Pair<Long, Long>> getWeeksInMonth(long startTime, long endTime) {
        List<Pair<Long, Long>> weeks = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);

        while (calendar.getTimeInMillis() < endTime) {
            long weekStartTime = calendar.getTimeInMillis();
            calendar.add(Calendar.DAY_OF_MONTH, 6);  // Move to the end of the week
            long weekEndTime = Math.min(calendar.getTimeInMillis(), endTime);
            weeks.add(new Pair<>(weekStartTime, weekEndTime));
            calendar.add(Calendar.DAY_OF_MONTH, 1);  // Move to the start of the next week
        }
        return weeks;
    }

    private void fetchVendorSalesData(String reportType) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                StringBuilder salesReport = new StringBuilder();

                long currentTimeMillis = System.currentTimeMillis();
                long startTime = getStartTimeForRange(reportType, currentTimeMillis);
                long endTime = getEndTimeForRange(reportType, startTime);

                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String startDateString = dateFormat.format(new Date(startTime));
                String endDateString = dateFormat.format(new Date(endTime));
                String currentDateString = dateFormat.format(new Date(currentTimeMillis));

                if (!reportType.equals("Yearly Report")) {
                    salesReport.append("Report Start Date: ").append(startDateString).append("\n");
                    salesReport.append("Report End Date: ").append(endDateString).append("\n");
                }
                salesReport.append("Report Generated On: ").append(currentDateString).append("\n\n");

                SimpleDateFormat periodFormat;
                switch (reportType) {
                    case "Daily Report":
                        periodFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                        generateDailyBreakdown(salesReport, dataSnapshot, startTime, endTime, periodFormat);
                        break;
                    case "Weekly Report":
                        periodFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                        generateWeeklyBreakdown(salesReport, dataSnapshot, startTime, endTime, periodFormat);
                        break;
                    case "Monthly Report":
                        periodFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                        generateMonthlyBreakdown(salesReport, dataSnapshot, startTime, endTime, periodFormat);
                        break;
                    case "Yearly Report":
                        periodFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
                        generateYearlyBreakdown(salesReport, dataSnapshot, startTime, endTime, periodFormat);
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid report type");
                }

                textViewSalesReport.setText(salesReport.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ReportActivity", "Database error: " + databaseError.getMessage());
                Toast.makeText(AdminsuperReports.this, "Failed to fetch sales data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateWeeklyBreakdown(StringBuilder salesReport, DataSnapshot dataSnapshot, long startTime, long endTime, SimpleDateFormat periodFormat) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);

        double totalSales = 0.0;
        double totalDiscountAmount = 0.0;
        int totalDiscountOrderCount = 0;

        while (calendar.getTimeInMillis() <= endTime) {
            // Get the start date of the week
            Date weekStartDate = calendar.getTime();
            String weekStartString = periodFormat.format(weekStartDate);

            // Move to the end of the week
            calendar.add(Calendar.DAY_OF_YEAR, 6);
            Date weekEndDate = calendar.getTime();
            String weekEndString = periodFormat.format(weekEndDate);

            // Prepare the week period string
            String weekPeriod = "Week: " + weekStartString + " - " + weekEndString;
            salesReport.append("\n--------------------------------------\n");
            salesReport.append(weekPeriod).append("\n");

            double weeklySales = 0.0;
            double weeklyDiscountAmount = 0.0;
            int weeklyDiscountOrderCount = 0;

            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                String role = userSnapshot.child("role").getValue(String.class);
                if ("vendor".equalsIgnoreCase(role)) {
                    String vendorName = userSnapshot.child("name").getValue(String.class);
                    salesReport.append("\nVendor: ").append(vendorName != null ? vendorName : "Unknown").append("\n");

                    DataSnapshot ordersSnapshot = userSnapshot.child("orders");
                    for (DataSnapshot orderSnapshot : ordersSnapshot.getChildren()) {
                        String orderStatus = orderSnapshot.child("Status").getValue(String.class);
                        String orderDateStr = orderSnapshot.child("Date").getValue(String.class);

                        if ("completed".equalsIgnoreCase(orderStatus) && orderDateStr != null) {
                            try {
                                SimpleDateFormat orderDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
                                Date orderDate = orderDateFormat.parse(orderDateStr);

                                if (orderDate != null && orderDate.getTime() >= weekStartDate.getTime() && orderDate.getTime() <= weekEndDate.getTime()) {
                                    Double finalPrice = orderSnapshot.child("Invoice").child("finalPrice").getValue(Double.class);
                                    Double discount = orderSnapshot.child("Invoice").child("discount").getValue(Double.class);

                                    if (finalPrice != null) {
                                        weeklySales += finalPrice;
                                        totalSales += finalPrice;
                                    }
                                    if (discount != null && discount > 0) {
                                        weeklyDiscountOrderCount++;
                                        weeklyDiscountAmount += discount;
                                        totalDiscountOrderCount++;
                                        totalDiscountAmount += discount;
                                    }
                                }
                            } catch (ParseException e) {
                                Log.e("ReportActivity", "Error parsing order date: " + e.getMessage(), e);
                            }
                        }
                    }
                    salesReport.append("Total Sales: ₱").append(String.format(Locale.getDefault(), "%.2f", weeklySales)).append("\n")
                            .append("Number of Orders with Discount: ").append(weeklyDiscountOrderCount).append("\n")
                            .append("Total Discount Amount: ₱").append(String.format(Locale.getDefault(), "%.2f", weeklyDiscountAmount)).append("\n\n");
                }
            }
            salesReport.append("--------------------------------------\n");
            // Move to the start of the next week
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Append total sales and discounts for the period
        salesReport.append("\nTotal Discount Information:\n")
                .append("Number of Orders with Discount: ").append(totalDiscountOrderCount).append("\n")
                .append("Total Discount Amount: ₱").append(String.format(Locale.getDefault(), "%.2f", totalDiscountAmount)).append("\n\n");
        salesReport.append("Total Sales: ₱").append(String.format(Locale.getDefault(), "%.2f", totalSales)).append("\n");
    }

    private void generateDailyBreakdown(StringBuilder salesReport, DataSnapshot dataSnapshot, long startTime, long endTime, SimpleDateFormat periodFormat) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);

        double totalSales = 0.0;
        double totalDiscountAmount = 0.0;
        int totalDiscountOrderCount = 0;

        while (calendar.getTimeInMillis() <= endTime) {
            String dayPeriod = periodFormat.format(calendar.getTime());
            salesReport.append("\n--------------------------------------\n");
            salesReport.append("Daily Breakdown for ").append(dayPeriod).append(":\n");

            double dailySales = 0.0;
            double dailyDiscountAmount = 0.0;
            int dailyDiscountOrderCount = 0;

            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                String role = userSnapshot.child("role").getValue(String.class);
                if ("vendor".equalsIgnoreCase(role)) {
                    String vendorName = userSnapshot.child("name").getValue(String.class);
                    salesReport.append("\nVendor: ").append(vendorName != null ? vendorName : "Unknown").append("\n");

                    DataSnapshot ordersSnapshot = userSnapshot.child("orders");
                    for (DataSnapshot orderSnapshot : ordersSnapshot.getChildren()) {
                        String orderStatus = orderSnapshot.child("Status").getValue(String.class);
                        String orderDateStr = orderSnapshot.child("Date").getValue(String.class);

                        if ("completed".equalsIgnoreCase(orderStatus) && orderDateStr != null) {
                            try {
                                SimpleDateFormat orderDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
                                Date orderDate = orderDateFormat.parse(orderDateStr);

                                if (orderDate != null && orderDate.getTime() >= calendar.getTimeInMillis() && orderDate.getTime() <= calendar.getTimeInMillis() + 86400000) { // 86400000ms = 1 day
                                    Double finalPrice = orderSnapshot.child("Invoice").child("finalPrice").getValue(Double.class);
                                    Double discount = orderSnapshot.child("Invoice").child("discount").getValue(Double.class);

                                    if (finalPrice != null) {
                                        dailySales += finalPrice;
                                        totalSales += finalPrice;
                                    }
                                    if (discount != null && discount > 0) {
                                        dailyDiscountOrderCount++;
                                        dailyDiscountAmount += discount;
                                        totalDiscountOrderCount++;
                                        totalDiscountAmount += discount;
                                    }
                                }
                            } catch (ParseException e) {
                                Log.e("ReportActivity", "Error parsing order date: " + e.getMessage(), e);
                            }
                        }
                    }
                    salesReport.append("Total Sales: ₱").append(String.format(Locale.getDefault(), "%.2f", dailySales)).append("\n")
                            .append("Number of Orders with Discount: ").append(dailyDiscountOrderCount).append("\n")
                            .append("Total Discount Amount: ₱").append(String.format(Locale.getDefault(), "%.2f", dailyDiscountAmount)).append("\n\n");
                }
            }
            salesReport.append("--------------------------------------\n");

            // Move to the next day
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Append total sales and discounts for the period
        salesReport.append("\nTotal Discount Information:\n")
                .append("Number of Orders with Discount: ").append(totalDiscountOrderCount).append("\n")
                .append("Total Discount Amount: ₱").append(String.format(Locale.getDefault(), "%.2f", totalDiscountAmount)).append("\n\n");
        salesReport.append("Total Sales: ₱").append(String.format(Locale.getDefault(), "%.2f", totalSales)).append("\n");
    }

    private void generateMonthlyBreakdown(StringBuilder salesReport, DataSnapshot dataSnapshot, long startTime, long endTime, SimpleDateFormat periodFormat) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);

        double totalSales = 0.0;
        double totalDiscountAmount = 0.0;
        int totalDiscountOrderCount = 0;

        salesReport.append("\nMonthly Breakdown:\n");
        while (calendar.getTimeInMillis() <= endTime) {
            String monthPeriod = periodFormat.format(calendar.getTime());
            salesReport.append("\n--------------------------------------\n");
            salesReport.append("Month: ").append(monthPeriod).append("\n");

            double monthlySales = 0.0;
            double monthlyDiscountAmount = 0.0;
            int monthlyDiscountOrderCount = 0;

            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                String role = userSnapshot.child("role").getValue(String.class);
                if ("vendor".equalsIgnoreCase(role)) {
                    String vendorName = userSnapshot.child("name").getValue(String.class);
                    salesReport.append("\nVendor: ").append(vendorName != null ? vendorName : "Unknown").append("\n");

                    DataSnapshot ordersSnapshot = userSnapshot.child("orders");
                    for (DataSnapshot orderSnapshot : ordersSnapshot.getChildren()) {
                        String orderStatus = orderSnapshot.child("Status").getValue(String.class);
                        String orderDateStr = orderSnapshot.child("Date").getValue(String.class);

                        if ("completed".equalsIgnoreCase(orderStatus) && orderDateStr != null) {
                            try {
                                SimpleDateFormat orderDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
                                Date orderDate = orderDateFormat.parse(orderDateStr);

                                if (orderDate != null && orderDate.getTime() >= calendar.getTimeInMillis() && orderDate.getTime() <= calendar.getTimeInMillis() + 2592000000L) { // 2592000000ms = 30 days
                                    Double finalPrice = orderSnapshot.child("Invoice").child("finalPrice").getValue(Double.class);
                                    Double discount = orderSnapshot.child("Invoice").child("discount").getValue(Double.class);

                                    if (finalPrice != null) {
                                        monthlySales += finalPrice;
                                        totalSales += finalPrice;
                                    }
                                    if (discount != null && discount > 0) {
                                        monthlyDiscountOrderCount++;
                                        monthlyDiscountAmount += discount;
                                        totalDiscountOrderCount++;
                                        totalDiscountAmount += discount;
                                    }
                                }
                            } catch (ParseException e) {
                                Log.e("ReportActivity", "Error parsing order date: " + e.getMessage(), e);
                            }
                        }
                    }
                    salesReport.append("Total Sales: ₱").append(String.format(Locale.getDefault(), "%.2f", monthlySales)).append("\n")
                            .append("Number of Orders with Discount: ").append(monthlyDiscountOrderCount).append("\n")
                            .append("Total Discount Amount: ₱").append(String.format(Locale.getDefault(), "%.2f", monthlyDiscountAmount)).append("\n\n");
                }
            }
            salesReport.append("--------------------------------------\n");

            // Move to the next month
            calendar.add(Calendar.MONTH, 1);
        }

        // Append total sales and discounts for the period
        salesReport.append("\nTotal Discount Information:\n")
                .append("Number of Orders with Discount: ").append(totalDiscountOrderCount).append("\n")
                .append("Total Discount Amount: ₱").append(String.format(Locale.getDefault(), "%.2f", totalDiscountAmount)).append("\n\n");
        salesReport.append("Total Sales: ₱").append(String.format(Locale.getDefault(), "%.2f", totalSales)).append("\n");
    }

    private void generateYearlyBreakdown(StringBuilder salesReport, DataSnapshot dataSnapshot, long startTime, long endTime, SimpleDateFormat periodFormat) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);

        // Variables to store totals for the entire period
        double totalSales = 0.0;
        double totalDiscountAmount = 0.0;
        int totalDiscountOrderCount = 0;

        salesReport.append("\nYearly Breakdown:\n");
        while (calendar.getTimeInMillis() <= endTime) {
            String yearPeriod = periodFormat.format(calendar.getTime());
            salesReport.append("\n--------------------------------------\n");
            salesReport.append("Year: ").append(yearPeriod).append("\n");

            // Variables to store totals for the current year
            double yearlySales = 0.0;
            double yearlyDiscountAmount = 0.0;
            int yearlyDiscountOrderCount = 0;

            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                String role = userSnapshot.child("role").getValue(String.class);
                if ("vendor".equalsIgnoreCase(role)) {
                    String vendorName = userSnapshot.child("name").getValue(String.class);
                    salesReport.append("\nVendor: ").append(vendorName != null ? vendorName : "Unknown").append("\n");

                    DataSnapshot ordersSnapshot = userSnapshot.child("orders");
                    for (DataSnapshot orderSnapshot : ordersSnapshot.getChildren()) {
                        String orderStatus = orderSnapshot.child("Status").getValue(String.class);
                        String orderDateStr = orderSnapshot.child("Date").getValue(String.class);

                        if ("completed".equalsIgnoreCase(orderStatus) && orderDateStr != null) {
                            try {
                                SimpleDateFormat orderDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
                                Date orderDate = orderDateFormat.parse(orderDateStr);

                                // Check if the order date falls within the current year
                                Calendar orderCalendar = Calendar.getInstance();
                                orderCalendar.setTime(orderDate);
                                int orderYear = orderCalendar.get(Calendar.YEAR);

                                Calendar currentYearCalendar = Calendar.getInstance();
                                currentYearCalendar.setTime(calendar.getTime());
                                int currentYear = currentYearCalendar.get(Calendar.YEAR);

                                if (orderYear == currentYear) {
                                    Double finalPrice = orderSnapshot.child("Invoice").child("finalPrice").getValue(Double.class);
                                    Double discount = orderSnapshot.child("Invoice").child("discount").getValue(Double.class);

                                    if (finalPrice != null) {
                                        yearlySales += finalPrice;
                                        totalSales += finalPrice;
                                    }
                                    if (discount != null && discount > 0) {
                                        yearlyDiscountOrderCount++;
                                        yearlyDiscountAmount += discount;
                                        totalDiscountOrderCount++;
                                        totalDiscountAmount += discount;
                                    }
                                }
                            } catch (ParseException e) {
                                Log.e("ReportActivity", "Error parsing order date: " + e.getMessage(), e);
                            }
                        }
                    }

                    // Append vendor sales for the current year
                    salesReport.append("Total Sales: ₱").append(String.format(Locale.getDefault(), "%.2f", yearlySales)).append("\n")
                            .append("Number of Orders with Discount: ").append(yearlyDiscountOrderCount).append("\n")
                            .append("Total Discount Amount: ₱").append(String.format(Locale.getDefault(), "%.2f", yearlyDiscountAmount)).append("\n\n");
                }
            }

            salesReport.append("--------------------------------------\n");
            // Move to the next year
            calendar.add(Calendar.YEAR, 1);
        }

        // Append total sales and discounts for the entire period
        salesReport.append("\nTotal Discount Information:\n")
                .append("Number of Orders with Discount: ").append(totalDiscountOrderCount).append("\n")
                .append("Total Discount Amount: ₱").append(String.format(Locale.getDefault(), "%.2f", totalDiscountAmount)).append("\n\n");
        salesReport.append("Total Sales: ₱").append(String.format(Locale.getDefault(), "%.2f", totalSales)).append("\n");
    }

    private void generateInvoiceReport(String reportType) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Get time range based on report type
        long currentTimeMillis = System.currentTimeMillis();
        long startTime = getStartTimeForRange(reportType, currentTimeMillis);
        long endTime = getEndTimeForRange(reportType, startTime);

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                StringBuilder invoiceReport = new StringBuilder("Invoice Report:\n\n");

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String role = userSnapshot.child("role").getValue(String.class);
                    if ("vendor".equalsIgnoreCase(role)) {
                        String vendorName = userSnapshot.child("name").getValue(String.class);
                        if (vendorName == null) {
                            vendorName = "Unknown";
                        }

                        // Traverse orders for this vendor
                        DataSnapshot ordersSnapshot = userSnapshot.child("orders");
                        List<DataSnapshot> validOrders = new ArrayList<>(); // List to hold valid orders

                        for (DataSnapshot orderSnapshot : ordersSnapshot.getChildren()) {
                            String orderStatus = orderSnapshot.child("Status").getValue(String.class);
                            String orderDateStr = orderSnapshot.child("Date").getValue(String.class);

                            if ("completed".equalsIgnoreCase(orderStatus) && orderDateStr != null) {
                                try {
                                    // Parse the order date
                                    SimpleDateFormat orderDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
                                    Date orderDate = orderDateFormat.parse(orderDateStr);

                                    // Check if the order date is within the selected time range
                                    if (orderDate != null && orderDate.getTime() >= startTime && orderDate.getTime() <= endTime) {
                                        // Add the valid order to the list
                                        validOrders.add(orderSnapshot);
                                    }
                                } catch (ParseException e) {
                                    Log.e("AdminsuperReports", "Error parsing order date: " + e.getMessage(), e);
                                }
                            }
                        }

                        // If the vendor has valid orders, generate the invoice report for them
                        if (!validOrders.isEmpty()) {
                            for (DataSnapshot orderSnapshot : validOrders) {
                                String orderDateStr = orderSnapshot.child("Date").getValue(String.class);
                                String invoiceNumber = orderSnapshot.child("Invoice").child("invoiceNumber").getValue(String.class);
                                if (invoiceNumber == null) {
                                    invoiceNumber = "N/A";
                                }
                                Double finalPrice = orderSnapshot.child("Invoice").child("finalPrice").getValue(Double.class);
                                if (finalPrice == null) {
                                    finalPrice = 0.0;
                                }
                                String orderId = orderSnapshot.getKey(); // Order ID is the key of the node

                                // Append order details for this invoice
                                invoiceReport.append("---------------------------------------------\nVendor: ").append(vendorName).append("\n");
                                invoiceReport.append("Invoice Number: ").append(invoiceNumber).append("\n");
                                invoiceReport.append("Order ID: ").append(orderId).append("\n");
                                invoiceReport.append("Order Date: ").append(orderDateStr).append("\n");
                                invoiceReport.append("Total Price: PHP ").append(String.format(Locale.getDefault(), "%.2f", finalPrice)).append("\n");
                                invoiceReport.append("Product/s:\n");

                                for (DataSnapshot productSnapshot : orderSnapshot.child("Products").getChildren()) {
                                    String productName = productSnapshot.child("Product").getValue(String.class);
                                    Integer quantity = productSnapshot.child("Quantity").getValue(Integer.class);
                                    if (productName != null && quantity != null) {
                                        invoiceReport.append("    - ").append(productName)
                                                .append(": ").append(quantity).append(" pcs\n");
                                    }
                                }

                                // Check if there is a discount (for PWD orders)
                                DataSnapshot invoiceSnapshot = orderSnapshot.child("Invoice");
                                String pwdId = invoiceSnapshot.child("pwdId").getValue(String.class);
                                String pwdName = invoiceSnapshot.child("pwdName").getValue(String.class);
                                Double discount = invoiceSnapshot.child("discount").getValue(Double.class);

                                if (pwdId != null && pwdName != null && discount != null) {
                                    invoiceReport.append("\nPWD/Senior Discount Applied:\n");
                                    invoiceReport.append("PWD/Senior ID Number: ").append(pwdId).append("\n");
                                    invoiceReport.append("PWD/Senior Name: ").append(pwdName).append("\n");
                                    invoiceReport.append("Discount: PHP ").append(String.format(Locale.getDefault(), "%.2f", discount)).append("\n");
                                }

                                invoiceReport.append("\n");
                            }
                        }
                    }
                }

                // Display the invoice report
                salesSum.setText(invoiceReport.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("AdminsuperReports", "Database error: " + databaseError.getMessage());
                Toast.makeText(AdminsuperReports.this, "Failed to fetch invoice data.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void generateInventoryReport(String reportType) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Get time range based on report type
        long currentTimeMillis = System.currentTimeMillis();
        long startTime = getStartTimeForRange(reportType, currentTimeMillis);
        long endTime = getEndTimeForRange(reportType, startTime);

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Map<String, ProductData>> vendorProductSales = new HashMap<>();

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String role = userSnapshot.child("role").getValue(String.class);
                    if ("vendor".equalsIgnoreCase(role)) {
                        String vendorName = userSnapshot.child("name").getValue(String.class);
                        if (vendorName == null) {
                            vendorName = "Unknown";
                        }
                        if (!vendorProductSales.containsKey(vendorName)) {
                            vendorProductSales.put(vendorName, new HashMap<>());
                        }

                        // Fetch current stock from the products node
                        DataSnapshot productsSnapshot = userSnapshot.child("products");
                        Map<String, Integer> currentStockMap = new HashMap<>();
                        for (DataSnapshot productSnapshot : productsSnapshot.getChildren()) {
                            String productName = productSnapshot.child("Product").getValue(String.class);
                            int quantity = productSnapshot.child("Quantity").getValue(Integer.class);
                            if (productName != null) {
                                currentStockMap.put(productName, quantity);
                            }
                        }

                        // Process completed orders within the selected time range
                        DataSnapshot ordersSnapshot = userSnapshot.child("orders");
                        for (DataSnapshot orderSnapshot : ordersSnapshot.getChildren()) {
                            String orderStatus = orderSnapshot.child("Status").getValue(String.class);
                            String orderDateStr = orderSnapshot.child("Date").getValue(String.class);

                            if ("completed".equalsIgnoreCase(orderStatus) && orderDateStr != null) {
                                try {
                                    // Parse the order date
                                    SimpleDateFormat orderDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
                                    Date orderDate = orderDateFormat.parse(orderDateStr);

                                    // Check if the order date is within the selected time range
                                    if (orderDate != null && orderDate.getTime() >= startTime && orderDate.getTime() <= endTime) {
                                        for (DataSnapshot productSnapshot : orderSnapshot.child("Products").getChildren()) {
                                            String productName = productSnapshot.child("Product").getValue(String.class);
                                            int soldQuantity = productSnapshot.child("Quantity").getValue(Integer.class);

                                            if (productName != null) {
                                                Map<String, ProductData> productSales = vendorProductSales.get(vendorName);

                                                // Get current stock from the map
                                                int currentStock = currentStockMap.getOrDefault(productName, 0);

                                                // Add or update product data
                                                ProductData productData = productSales.getOrDefault(productName, new ProductData(0, currentStock));
                                                productData.totalSold += soldQuantity;
                                                productSales.put(productName, productData);
                                            }
                                        }
                                    }
                                } catch (ParseException e) {
                                    Log.e("AdminsuperReports", "Error parsing order date: " + e.getMessage(), e);
                                }
                            }
                        }
                    }
                }

                displayInventoryReport(vendorProductSales);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("AdminsuperReports", "Database error: " + databaseError.getMessage());
                Toast.makeText(AdminsuperReports.this, "Failed to fetch inventory data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayInventoryReport(Map<String, Map<String, ProductData>> vendorProductSales) {
        StringBuilder report = new StringBuilder("Inventory Report:\n\n");

        for (Map.Entry<String, Map<String, ProductData>> vendorEntry : vendorProductSales.entrySet()) {
            report.append("Vendor: ").append(vendorEntry.getKey()).append("\n");
            for (Map.Entry<String, ProductData> productEntry : vendorEntry.getValue().entrySet()) {
                ProductData productData = productEntry.getValue();
                report.append("  Product: ").append(productEntry.getKey())
                        .append("\n  Total Sold Quantity: ").append(productData.totalSold).append(" pieces")
                        .append("\n  Current Stock: ").append(productData.currentStock).append(" pieces")
                        .append("\n\n");
            }
        }

        inventoryReport.setText(report.toString());
    }

    // Helper class to store product data
    private static class ProductData {
        int totalSold;
        int currentStock;

        ProductData(int totalSold, int currentStock) {
            this.totalSold = totalSold;
            this.currentStock = currentStock;
        }
    }


   
// Helper method to calculate start time for the selected time range (daily, weekly, etc.)
    private long getStartTimeForRange(String timeRange, long currentTimeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTimeMillis);

        switch (timeRange) {
            case "Daily Report":
                // Set start time to January 1, 2025
                calendar.set(2025, Calendar.JANUARY, 1, 0, 0, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            case "Weekly Report":
                calendar.set(2025, Calendar.JANUARY, 1, 0, 0, 0);  // Ensure the start date is Jan 1, 2025
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            case "Monthly Report":
                calendar.set(2025, Calendar.JANUARY, 1, 0, 0, 0);  // Set to the start of January 2025
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            case "Yearly Report":
                calendar.set(2025, Calendar.JANUARY, 1, 0, 0, 0);  // Set to the start of January 2025
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
                // Set end time to January 7, 2025
                calendar.set(2025, Calendar.JANUARY, 7, 23, 59, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                break;
            case "Weekly Report":
                // Set end time to the last day of the current month
                calendar.set(2025, Calendar.JANUARY, 31, 23, 59, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                break;
            case "Monthly Report":
                // Set end time to December 31, 2025
                calendar.set(2025, Calendar.DECEMBER, 31, 23, 59, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                break;
            case "Yearly Report":
                calendar.add(Calendar.YEAR, 1);
                calendar.add(Calendar.MILLISECOND, -1); // Subtract one millisecond to get the end time
                break;
            default:
                throw new IllegalArgumentException("Invalid report type");
        }
        return calendar.getTimeInMillis();
    }

    // Helper method to get the report period based on the selected range
    private String getReportPeriod(String timeRange) {
        Calendar calendar = Calendar.getInstance();
        String reportPeriod = "";

        switch (timeRange) {
            case "Daily Report":
                // Set report period to January 1, 2025 - January 7, 2025
                reportPeriod = "Jan 01, 2025 - Jan 07, 2025";
                break;
            case "Weekly Report":
                SimpleDateFormat weekFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                // Ensure the start date is Jan 1, 2025
                calendar.set(2025, Calendar.JANUARY, 1);
                String monthName = new SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.getTime());
                reportPeriod = "Weekly Report for " + monthName + " 2025";
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
}