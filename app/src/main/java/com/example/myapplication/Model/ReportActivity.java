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
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        paint.setTextSize(12);

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int x = 10;
        int y = 50;
        int maxWidth = 570; // Leave margins on both sides

        drawTextWithWrapping(canvas, "Report Title: " + reportTitle.getText().toString(), x, y, maxWidth, paint);
        y += 40;

        drawTextWithWrapping(canvas, "Sales Report:", x, y, maxWidth, paint);
        y += 20;
        y = drawMultilineText(canvas, textViewSalesReport.getText().toString(), x, y, maxWidth, paint);

        y += 30;
        drawTextWithWrapping(canvas, "Inventory Report:", x, y, maxWidth, paint);
        y += 20;
        y = drawMultilineText(canvas, textViewInventoryReport.getText().toString(), x, y, maxWidth, paint);

        y += 30;
        drawTextWithWrapping(canvas, "Sales Summary:", x, y, maxWidth, paint);
        y += 20;
        y = drawMultilineText(canvas, salesSum.getText().toString(), x, y, maxWidth, paint);

        pdfDocument.finishPage(page);

        // Add a timestamp to the filename
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "Report_" + timestamp + ".pdf";

        // Save the PDF to the Downloads folder
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

    private void drawTextWithWrapping(Canvas canvas, String text, int x, int y, int maxWidth, Paint paint) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (paint.measureText(line + word) < maxWidth) {
                line.append(word).append(" ");
            } else {
                canvas.drawText(line.toString(), x, y, paint);
                line = new StringBuilder(word + " ");
                y += paint.descent() - paint.ascent();
            }
        }
        canvas.drawText(line.toString(), x, y, paint);
    }

    private int drawMultilineText(Canvas canvas, String text, int x, int y, int maxWidth, Paint paint) {
        for (String line : text.split("\n")) {
            drawTextWithWrapping(canvas, line, x, y, maxWidth, paint);
            y += paint.descent() - paint.ascent();
        }
        return y;
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
                                data.append("Product: ").append(inventory.getProduct())
                                        .append("\n");

                                int totalSold = productSales.getOrDefault(inventory.getProduct(), 0);
                                data.append("Quantity Sold: ").append(totalSold).append("\n");

                                data.append("Quantity in Stock: ").append(inventory.getQuantity())
                                        .append("\nPrice: ").append(inventory.getPrice())
                                        .append("\n---------------------------------")
                                        .append("\n\n");
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

    // Process each sale to extract the required details
    private void processSaleForDetails(DataSnapshot sale, long startTime, long endTime, String reportType, StringBuilder salesData) {
        String date = sale.child("Date").getValue(String.class);
        if (date == null) {
            Log.w("ReportActivity", "Date is null for sale: " + sale.getKey());
            return;
        }

        try {
            SimpleDateFormat firebaseDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.US);  // Ensure the correct locale
            Date saleDateParsed = firebaseDateFormat.parse(date);
            if (saleDateParsed == null || saleDateParsed.getTime() < startTime || saleDateParsed.getTime() > endTime) {
                Log.w("ReportActivity", "Sale date is out of range or could not be parsed for sale: " + sale.getKey());
                return;
            }

            // Extract details from the Invoice node
            String invoiceNumber = sale.child("Invoice").child("orderId").getValue(String.class);
            String orderDetails = sale.child("Invoice").child("orderDetails").getValue(String.class);
            Double discount = sale.child("Invoice").child("discount").getValue(Double.class);
            Double finalPrice = sale.child("Invoice").child("finalPrice").getValue(Double.class);
            String vendorName = sale.child("Invoice").child("vendorName").getValue(String.class);

            // Add details to summary
            salesData.append("Order ID: ").append(invoiceNumber != null ? invoiceNumber : "N/A").append("\n");
            salesData.append("Order Details: ").append(orderDetails != null ? orderDetails : "N/A").append("\n");
            salesData.append("Discount: ₱").append(discount != null ? String.format(Locale.getDefault(), "%.2f", discount) : "0.00").append("\n");
            salesData.append("Final Price: ₱").append(finalPrice != null ? String.format(Locale.getDefault(), "%.2f", finalPrice) : "0.00").append("\n");
            salesData.append("Vendor Name: ").append(vendorName != null ? vendorName : "N/A").append("\n");
            salesData.append("--------------------------------------\n\n");

        } catch (ParseException e) {
            Log.e("ReportActivity", "Error parsing date for sale: " + sale.getKey(), e);
        } catch (Exception e) {
            Log.e("ReportActivity", "Error processing sale for details: " + sale.getKey(), e);
        }
    }

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
                HashMap<String, ProductSalesData> productSales = new HashMap<>();
                double totalSalesAmount = 0;    // Track total sales amount

                // Initialize a map to hold sales amounts for each period
                Map<String, Double> periodSales = new HashMap<>();
                SimpleDateFormat periodFormat;

                long currentTimeMillis = System.currentTimeMillis();
                long startTime = getStartTimeForRange(timeRange, currentTimeMillis);
                long endTime = getEndTimeForRange(timeRange, startTime);

                switch (timeRange) {
                    case "Daily Report":
                        periodFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()); // e.g., "Dec 26, 2024"
                        break;
                    case "Weekly Report":
                        periodFormat = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()); // e.g., "Mon, Dec 26, 2024"
                        break;
                    case "Monthly Report":
                        periodFormat = new SimpleDateFormat("MMMM", Locale.getDefault()); // e.g., "January"
                        break;
                    case "Yearly Report":
                        periodFormat = new SimpleDateFormat("yyyy", Locale.getDefault()); // e.g., "2024"
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid report type");
                }

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String date = snapshot.child("Date").getValue(String.class);
                    Double finalPrice = null;

                    // Handle Invoice as a HashMap
                    DataSnapshot invoiceSnapshot = snapshot.child("Invoice");
                    if (invoiceSnapshot.exists()) {
                        finalPrice = invoiceSnapshot.child("finalPrice").getValue(Double.class);
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
                            String period = periodFormat.format(saleDate);

                            // Update period sales amount
                            double currentSalesAmount = periodSales.getOrDefault(period, 0.0);
                            periodSales.put(period, currentSalesAmount + finalPrice);

                            // Update total sales amount
                            totalSalesAmount += finalPrice;
                        }
                    } catch (Exception e) {
                        Log.e("ReportActivity", "Error parsing date for entry: " + snapshot.getKey(), e);
                    }
                }

                // Generate period sales report and include the exact date before the day of sales
                for (Map.Entry<String, Double> entry : periodSales.entrySet()) {
                    salesReport.append("Date: ").append(entry.getKey()).append("\n");
                    salesReport.append("Sales: ₱").append(String.format("%.2f", entry.getValue()))
                            .append("\n---------------------------------\n\n");
                }

                // Include the overall total at the end of the report
                salesReport.append("\n\n")
                        .append("Total Sales: ₱").append(String.format("%.2f", totalSalesAmount))
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