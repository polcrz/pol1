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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        paint.setTextSize(12);

        int pageWidth = 595;
        int pageHeight = 842;
        int margin = 50;
        int maxWidth = pageWidth - 2 * margin;
        int startY = margin;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int x = margin;
        int y = startY;

        // Draw Report Title
        y = drawTextWithPageWrapping(canvas, pdfDocument, "Report Title: " + reportTitle.getText().toString(), x, y, maxWidth, margin, pageHeight, paint, pageWidth);

        // Draw Sales Report
        y += 40;
        y = drawTextWithPageWrapping(canvas, pdfDocument, "Sales Report:", x, y, maxWidth, margin, pageHeight, paint, pageWidth);

        y += 20;
        y = drawMultilineTextWithPageWrapping(canvas, pdfDocument, textViewSalesReport.getText().toString(), x, y, maxWidth, margin, pageHeight, paint, pageWidth);

        // Draw Inventory Report
        y += 30;
        y = drawTextWithPageWrapping(canvas, pdfDocument, "Inventory Report:", x, y, maxWidth, margin, pageHeight, paint, pageWidth);

        y += 20;
        y = drawMultilineTextWithPageWrapping(canvas, pdfDocument, inventoryReport.getText().toString(), x, y, maxWidth, margin, pageHeight, paint, pageWidth);

        // Draw Sales Summary
        y += 30;
        y = drawTextWithPageWrapping(canvas, pdfDocument, "Sales Summary:", x, y, maxWidth, margin, pageHeight, paint, pageWidth);

        y += 20;
        y = drawMultilineTextWithPageWrapping(canvas, pdfDocument, salesSum.getText().toString(), x, y, maxWidth, margin, pageHeight, paint, pageWidth);

        // Finish the last page
        pdfDocument.finishPage(page);

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "Report_" + timestamp + ".pdf";
        File pdfFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            pdfDocument.writeTo(fos);
            Toast.makeText(this, "PDF saved to " + pdfFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("ReportActivity", "Error saving PDF", e);
            Toast.makeText(this, "Failed to save PDF.", Toast.LENGTH_SHORT).show();
        } finally {
            pdfDocument.close();
        }
    }

    private int drawTextWithPageWrapping(Canvas canvas, PdfDocument pdfDocument, String text, int x, int y, int maxWidth, int margin, int pageHeight, Paint paint, int pageWidth) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        PdfDocument.Page currentPage = null;

        for (String word : words) {
            if (paint.measureText(line + word) < maxWidth) {
                line.append(word).append(" ");
            } else {
                // Draw the current line
                canvas.drawText(line.toString(), x, y, paint);
                line = new StringBuilder(word + " ");
                y += (int) Math.ceil(paint.descent() - paint.ascent());

                // Check if the current `y` exceeds the page height
                if (y > pageHeight - margin) {
                    // Finish the current page
                    pdfDocument.finishPage(currentPage);

                    // Start a new page
                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.getPages().size() + 1).create();
                    currentPage = pdfDocument.startPage(pageInfo);
                    canvas = currentPage.getCanvas();

                    // Reset `y` to the top margin for the new page
                    y = margin;
                }
            }
        }

        // Draw the remaining line
        if (line.length() > 0) {
            canvas.drawText(line.toString(), x, y, paint);
            y += (int) Math.ceil(paint.descent() - paint.ascent());
        }

        return y;
    }


    private int drawMultilineTextWithPageWrapping(Canvas canvas, PdfDocument pdfDocument, String text, int x, int y, int maxWidth, int margin, int pageHeight, Paint paint, int pageWidth) {
        for (String line : text.split("\n")) {
            y = drawTextWithPageWrapping(canvas, pdfDocument, line, x, y, maxWidth, margin, pageHeight, paint, pageWidth);
        }
        return y;
    }




    private void fetchVendorSalesData(String reportType) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                StringBuilder salesReport = new StringBuilder();
                double totalSales = 0.0; // Track total sales across all vendors

                // Define the start and end times for the report based on the selected report type
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
                        double vendorTotalSales = 0.0; // Track total sales for this vendor

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
                                        // Extract the final price
                                        Double finalPrice = orderSnapshot.child("Invoice").child("finalPrice").getValue(Double.class);
                                        if (finalPrice != null) {
                                            vendorTotalSales += finalPrice; // Add to vendor's total sales
                                        }
                                    }
                                } catch (ParseException e) {
                                    Log.e("AdminsuperReports", "Error parsing order date: " + e.getMessage(), e);
                                }
                            }
                        }

                        // Append vendor's total sales to the report
                        salesReport.append("  Total Sales: ₱").append(String.format(Locale.getDefault(), "%.2f", vendorTotalSales)).append("\n\n");
                        totalSales += vendorTotalSales; // Add to overall total sales
                    }
                }

                // Display the sales report
                textViewSalesReport.setText(salesReport.toString());

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("AdminsuperReports", "Database error: " + databaseError.getMessage());
                Toast.makeText(AdminsuperReports.this, "Failed to fetch sales data.", Toast.LENGTH_SHORT).show();
            }
        });
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
                                        invoiceReport.append("Vendor: ").append(vendorName).append("\n");
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
                                            invoiceReport.append("  PWD Discount Applied:\n");
                                            invoiceReport.append("    PWD ID: ").append(pwdId).append("\n");
                                            invoiceReport.append("    PWD Name: ").append(pwdName).append("\n");
                                            invoiceReport.append("    Discount: ₱").append(String.format(Locale.getDefault(), "%.2f", discount)).append("\n");
                                        }

                                        invoiceReport.append("\n");
                                    }
                                } catch (ParseException e) {
                                    Log.e("AdminsuperReports", "Error parsing order date: " + e.getMessage(), e);
                                }
                            }
                        }

                        // If the vendor has no invoices in the selected range, add a message
                        if (!hasInvoices) {
                            invoiceReport.append("  No completed orders within the selected date range.\n\n");
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