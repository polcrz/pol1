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
    private Button saveAsPdfButton;

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

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.spinner_items, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mySpinner.setAdapter(adapter);

        generate.setOnClickListener(v -> {
            String selectedOption = mySpinner.getSelectedItem().toString();

            if (selectedOption.equals("Select Report")) {
                Toast.makeText(AdminsuperReports.this, "Please select a valid report (Daily, Weekly, Monthly, or Yearly).", Toast.LENGTH_SHORT).show();
            } else {
                reportTitle.setText("Report for: " + getReportPeriod(selectedOption));
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
                        salesReport.append("\n--------------------------------------\nVendor: ").append(vendorName != null ? vendorName : "Unknown").append("\n");

                        DataSnapshot ordersSnapshot = userSnapshot.child("orders");
                        double vendorTotalSales = 0.0;
                        double vendorDiscountAmount = 0.0;
                        int vendorDiscountOrderCount = 0;
                        Map<String, Integer> overallProductSales = new HashMap<>();
                        Map<String, Double> periodSales = new LinkedHashMap<>();
                        Map<String, Map<String, Integer>> periodProductSales = new LinkedHashMap<>();
                        Map<String, Integer> periodDiscountOrderCount = new LinkedHashMap<>();
                        Map<String, Double> periodDiscountAmount = new LinkedHashMap<>();

                        SimpleDateFormat periodFormat;
                        switch (reportType) {
                            case "Daily Report":
                                periodFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()); // e.g., "Dec 29, 2024"
                                break;
                            case "Weekly Report":
                                periodFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()); // e.g., "Dec 23, 2024 - Dec 29, 2024"
                                break;
                            case "Monthly Report":
                                periodFormat = new SimpleDateFormat("MMM dd", Locale.getDefault()); // e.g., "Dec 01 - Dec 07"
                                break;
                            case "Yearly Report":
                                periodFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()); // e.g., "December 2024"
                                break;
                            default:
                                throw new IllegalArgumentException("Invalid report type");
                        }

                        if (reportType.equals("Monthly Report")) {
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

                        for (DataSnapshot orderSnapshot : ordersSnapshot.getChildren()) {
                            String orderStatus = orderSnapshot.child("Status").getValue(String.class);
                            String orderDateStr = orderSnapshot.child("Date").getValue(String.class);

                            Double finalPrice = null;
                            Double discount = null;

                            // Handle Invoice as a HashMap
                            DataSnapshot invoiceSnapshot = orderSnapshot.child("Invoice");
                            if (invoiceSnapshot.exists()) {
                                finalPrice = invoiceSnapshot.child("finalPrice").getValue(Double.class);
                                discount = invoiceSnapshot.child("discount").getValue(Double.class);
                            }

                            if ("completed".equalsIgnoreCase(orderStatus) && orderDateStr != null && finalPrice != null) {
                                try {
                                    SimpleDateFormat orderDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
                                    Date orderDate = orderDateFormat.parse(orderDateStr);

                                    if (orderDate != null && orderDate.getTime() >= startTime && orderDate.getTime() <= endTime) {
                                        String period = "";
                                        if (reportType.equals("Monthly Report")) {
                                            Calendar saleCalendar = Calendar.getInstance();
                                            saleCalendar.setTime(orderDate);
                                            for (String weekPeriod : periodSales.keySet()) {
                                                if (weekPeriod.contains(periodFormat.format(saleCalendar.getTime()))) {
                                                    period = weekPeriod;
                                                    break;
                                                }
                                            }
                                        } else {
                                            period = periodFormat.format(orderDate);
                                        }

                                        if (!period.isEmpty()) {
                                            double currentSalesAmount = periodSales.getOrDefault(period, 0.0);
                                            periodSales.put(period, currentSalesAmount + finalPrice);

                                            if (!periodProductSales.containsKey(period)) {
                                                periodProductSales.put(period, new HashMap<>());
                                            }
                                            Map<String, Integer> periodProductSalesMap = periodProductSales.get(period);
                                            for (DataSnapshot productSnapshot : orderSnapshot.child("Products").getChildren()) {
                                                String productName = productSnapshot.child("Product").getValue(String.class);
                                                Integer quantitySold = productSnapshot.child("Quantity").getValue(Integer.class);
                                                if (productName != null && quantitySold != null) {
                                                    int currentQuantity = periodProductSalesMap.getOrDefault(productName, 0);
                                                    periodProductSalesMap.put(productName, currentQuantity + quantitySold);

                                                    // Update overall product sales
                                                    int overallQuantity = overallProductSales.getOrDefault(productName, 0);
                                                    overallProductSales.put(productName, overallQuantity + quantitySold);
                                                }
                                            }

                                            if (discount != null && discount > 0) {
                                                int currentDiscountOrderCount = periodDiscountOrderCount.getOrDefault(period, 0);
                                                double currentDiscountAmount = periodDiscountAmount.getOrDefault(period, 0.0);
                                                periodDiscountOrderCount.put(period, currentDiscountOrderCount + 1);
                                                periodDiscountAmount.put(period, currentDiscountAmount + discount);

                                                vendorDiscountOrderCount++;
                                                vendorDiscountAmount += discount;
                                            }
                                        }

                                        vendorTotalSales += finalPrice;
                                    }
                                } catch (ParseException e) {
                                    Log.e("ReportActivity", "Error parsing order date: " + e.getMessage(), e);
                                }
                            }
                        }

                        salesReport.append("  Total Sales: ₱").append(String.format(Locale.getDefault(), "%.2f", vendorTotalSales)).append("\n");

                        if (!overallProductSales.isEmpty()) {
                            salesReport.append("  Products Sold:\n");
                            for (Map.Entry<String, Integer> productEntry : overallProductSales.entrySet()) {
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

                        salesReport.append("\nVendor Sales Breakdown:\n");
                        for (Map.Entry<String, Double> entry : periodSales.entrySet()) {
                            salesReport.append("Period: ").append(entry.getKey()).append("\n");
                            salesReport.append("Sales: ₱").append(String.format("%.2f", entry.getValue())).append("\n\n");

                            Map<String, Integer> periodProductSalesMap = periodProductSales.get(entry.getKey());
                            if (periodProductSalesMap != null && !periodProductSalesMap.isEmpty()) {
                                salesReport.append("Products Sold:\n");
                                for (Map.Entry<String, Integer> productEntry : periodProductSalesMap.entrySet()) {
                                    salesReport.append("  Product: ").append(productEntry.getKey()).append("\n");
                                    salesReport.append("  Quantity Sold: ").append(productEntry.getValue()).append(" pcs\n");
                                }
                            }

                            salesReport.append("Discount Information:\n");
                            salesReport.append("  Number of Orders with Discount: ").append(periodDiscountOrderCount.get(entry.getKey())).append("\n");
                            salesReport.append("  Total Discount Amount: ₱").append(String.format("%.2f", periodDiscountAmount.get(entry.getKey()))).append("\n");
                            salesReport.append("---------------------------------\n\n");
                        }
                    }
                }

                salesReport.append("Total Discount Information:\n")
                        .append("  Number of Orders with Discount: ").append(totalDiscountOrderCount).append("\n")
                        .append("  Total Discount Amount: ₱").append(String.format(Locale.getDefault(), "%.2f", totalDiscountAmount)).append("\n\n");
                salesReport.append("Total Sales: ₱").append(String.format(Locale.getDefault(), "%.2f", totalSales)).append("\n");

                textViewSalesReport.setText(salesReport.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ReportActivity", "Database error: " + databaseError.getMessage());
                Toast.makeText(AdminsuperReports.this, "Failed to fetch sales data.", Toast.LENGTH_SHORT).show();
            }
        });
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

            Map<String, Double> dailySales = new HashMap<>();

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
                                Log.e("AdminsuperReports", "Error parsing order date: " + e.getMessage(), e);
                            }
                        }
                    }

                    salesReport.append("  Daily Sales: ₱").append(String.format(Locale.getDefault(), "%.2f", vendorDailySales)).append("\n");
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

            Map<String, Double> dailySales = new HashMap<>();

            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                String role = userSnapshot.child("role").getValue(String.class);
                if ("vendor".equalsIgnoreCase(role)) {
                    String vendorName = userSnapshot.child("name").getValue(String.class);
                    salesReport.append("Vendor: ").append(vendorName != null ? vendorName : "Unknown").append("\n");

                    DataSnapshot ordersSnapshot = userSnapshot.child("orders");

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
                                Log.e("AdminsuperReports", "Error parsing order date: " + e.getMessage(), e);
                            }
                        }
                    }
                }
            }

            for (Map.Entry<String, Double> entry : dailySales.entrySet()) {
                salesReport.append("  ").append(entry.getKey()).append(": ₱")
                        .append(String.format(Locale.getDefault(), "%.2f", entry.getValue())).append("\n");
            }
        }
    }

    private void generateMonthlyBreakdown(StringBuilder salesReport, DataSnapshot dataSnapshot, long startTime, long endTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        SimpleDateFormat weekFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        List<Pair<Long, Long>> weeksInMonth = getWeeksInMonth(startTime, endTime);

        salesReport.append("\nMonthly Breakdown:\n");
        for (Pair<Long, Long> week : weeksInMonth) {
            String weekRange = weekFormat.format(new Date(week.first)) + " - " + weekFormat.format(new Date(week.second));
            salesReport.append("  Week: ").append(weekRange).append("\n");

            Map<String, Double> weeklySales = new HashMap<>();

            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                String role = userSnapshot.child("role").getValue(String.class);
                if ("vendor".equalsIgnoreCase(role)) {
                    String vendorName = userSnapshot.child("name").getValue(String.class);
                    salesReport.append("Vendor: ").append(vendorName != null ? vendorName : "Unknown").append("\n");

                    DataSnapshot ordersSnapshot = userSnapshot.child("orders");

                    for (DataSnapshot orderSnapshot : ordersSnapshot.getChildren()) {
                        String orderStatus = orderSnapshot.child("Status").getValue(String.class);
                        String orderDateStr = orderSnapshot.child("Date").getValue(String.class);

                        if ("completed".equalsIgnoreCase(orderStatus) && orderDateStr != null) {
                            try {
                                SimpleDateFormat orderDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
                                Date orderDate = orderDateFormat.parse(orderDateStr);

                                if (orderDate != null && orderDate.getTime() >= week.first && orderDate.getTime() <= week.second) {
                                    Double finalPrice = orderSnapshot.child("Invoice").child("finalPrice").getValue(Double.class);

                                    if (finalPrice != null) {
                                        weeklySales.put(vendorName, weeklySales.getOrDefault(vendorName, 0.0) + finalPrice);
                                    }
                                }
                            } catch (ParseException e) {
                                Log.e("AdminsuperReports", "Error parsing order date: " + e.getMessage(), e);
                            }
                        }
                    }
                }
            }

            for (Map.Entry<String, Double> entry : weeklySales.entrySet()) {
                salesReport.append("  ").append(entry.getKey()).append(" Weekly Sales: ₱")
                        .append(String.format(Locale.getDefault(), "%.2f", entry.getValue())).append("\n");
            }
        }
    }

    private void generateYearlyBreakdown(StringBuilder salesReport, DataSnapshot dataSnapshot, long startTime, long endTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        salesReport.append("\nYearly Breakdown:\n");
        while (calendar.getTimeInMillis() <= endTime) {
            long monthStartTime = calendar.getTimeInMillis();
            calendar.add(Calendar.MONTH, 1);
            long monthEndTime = calendar.getTimeInMillis() - 1;

            String monthRange = monthFormat.format(new Date(monthStartTime));
            salesReport.append("  Month: ").append(monthRange).append("\n");

            Map<String, Double> monthlySales = new HashMap<>();

            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                String role = userSnapshot.child("role").getValue(String.class);
                if ("vendor".equalsIgnoreCase(role)) {
                    String vendorName = userSnapshot.child("name").getValue(String.class);
                    salesReport.append("Vendor: ").append(vendorName != null ? vendorName : "Unknown").append("\n");

                    DataSnapshot ordersSnapshot = userSnapshot.child("orders");

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
                                        monthlySales.put(vendorName, monthlySales.getOrDefault(vendorName, 0.0) + finalPrice);
                                    }
                                }
                            } catch (ParseException e) {
                                Log.e("AdminsuperReports", "Error parsing order date: " + e.getMessage(), e);
                            }
                        }
                    }
                }
            }

            for (Map.Entry<String, Double> entry : monthlySales.entrySet()) {
                salesReport.append("  ").append(entry.getKey()).append(" Monthly Sales: ₱")
                        .append(String.format(Locale.getDefault(), "%.2f", entry.getValue())).append("\n");
            }
        }
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
                StringBuilder noSalesVendors = new StringBuilder("Vendors with No Sales:\n\n");

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String role = userSnapshot.child("role").getValue(String.class);
                    if ("vendor".equalsIgnoreCase(role)) {
                        String vendorName = userSnapshot.child("name").getValue(String.class);
                        if (vendorName == null) {
                            vendorName = "Unknown";
                        }

                        // Traverse orders for this vendor
                        DataSnapshot ordersSnapshot = userSnapshot.child("orders");
                        boolean hasInvoices = false; // Track if this vendor has any valid invoices

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
                                        hasInvoices = true; // Mark that this vendor has invoices in this range
                                        // Get invoice details
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
                                        invoiceReport.append("  Invoice Number: ").append(invoiceNumber).append("\n");
                                        invoiceReport.append("  Order ID: ").append(orderId).append("\n");
                                        invoiceReport.append("  Order Date: ").append(orderDateStr).append("\n");
                                        invoiceReport.append("  Total Price: ₱").append(String.format(Locale.getDefault(), "%.2f", finalPrice)).append("\n");
                                        invoiceReport.append("  Products:\n");

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
                                            invoiceReport.append("  PWD/Senior Discount Applied:\n");
                                            invoiceReport.append("    PWD/Senior ID Number: ").append(pwdId).append("\n");
                                            invoiceReport.append("    PWD/Senior Name: ").append(pwdName).append("\n");
                                            invoiceReport.append("    Discount: ₱").append(String.format(Locale.getDefault(), "%.2f", discount)).append("\n");
                                        }

                                        invoiceReport.append("\n");
                                    }
                                } catch (ParseException e) {
                                    Log.e("AdminsuperReports", "Error parsing order date: " + e.getMessage(), e);
                                }
                            }
                        }

                        // If the vendor has no invoices in the selected range, add their name to the no sales list
                        if (!hasInvoices) {
                            noSalesVendors.append(vendorName).append("\n");
                        }
                    }
                }

                // Append vendors with no sales to the report
                invoiceReport.append(noSalesVendors).append("\n");

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
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
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
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
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
}