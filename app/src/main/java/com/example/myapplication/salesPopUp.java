package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class salesPopUp extends AppCompatActivity {

    private TextView salesTextView; // TextView to display vendor names and their total sales
    private TextView generatedDateTextView; // TextView to display the report generation date
    private Button savePdfButton; // Button to save the report as a PDF

    private Button back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_pop_up);

        // Initialize the TextViews and Button
        salesTextView = findViewById(R.id.vName);
        generatedDateTextView = findViewById(R.id.generatedDateTextView);
        savePdfButton = findViewById(R.id.savepdf);
        back = findViewById(R.id.back);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(salesPopUp.this, adminSuperAdmin.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish(); // Ensure the current activity is removed from the stack
            }
        });

        // Check and request permissions
        checkAndRequestPermissions();

        // Fetch and display sales data for vendors
        fetchVendorsSales();

        savePdfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save the current sales report as PDF
                saveReportToPdf(salesTextView.getText().toString());
            }
        });
    }

    // Fetch vendor sales data from Firebase
    private void fetchVendorsSales() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                StringBuilder salesReport = new StringBuilder();

                // Get the current date when the report is generated
                String currentDate = getCurrentDate();
                // Set the date when the report was generated in the generatedDateTextView
                generatedDateTextView.setText("Report Generated on: " + currentDate);

                // Get the current week, month, and year
                String weekRange = getWeekRange();
                String currentMonth = getCurrentMonth();
                String currentYear = getCurrentYear();

                // Loop through the users and process sales data
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    try {
                        String role = userSnapshot.child("role").getValue(String.class);
                        if ("vendor".equalsIgnoreCase(role)) { // Process only vendors
                            String vendorName = userSnapshot.child("name").getValue(String.class);
                            if (vendorName == null) vendorName = "Unknown Vendor";

                            double dailySales = 0, weeklySales = 0, monthlySales = 0, yearlySales = 0;

                            // Iterate through orders
                            DataSnapshot ordersSnapshot = userSnapshot.child("orders");
                            if (ordersSnapshot.exists()) {
                                for (DataSnapshot orderSnapshot : ordersSnapshot.getChildren()) {
                                    String orderStatus = orderSnapshot.child("Status").getValue(String.class);
                                    if ("completed".equalsIgnoreCase(orderStatus)) {
                                        String orderDateStr = orderSnapshot.child("Date").getValue(String.class);
                                        Double finalPrice = orderSnapshot.child("Invoice").child("finalPrice").getValue(Double.class);

                                        if (orderDateStr != null && finalPrice != null) {
                                            // Update sales for different time periods
                                            if (isDateInFilter(orderDateStr, "daily")) {
                                                dailySales += finalPrice;
                                            }
                                            if (isDateInFilter(orderDateStr, "weekly")) {
                                                weeklySales += finalPrice;
                                            }
                                            if (isDateInFilter(orderDateStr, "monthly")) {
                                                monthlySales += finalPrice;
                                            }
                                            if (isDateInFilter(orderDateStr, "yearly")) {
                                                yearlySales += finalPrice;
                                            }
                                        }
                                    }
                                }
                            }

                            // Append vendor data to the report
                            salesReport.append(String.format("Name: %s\n", vendorName));
                            salesReport.append(String.format("Daily Sales: ₱%.2f\n", dailySales));
                            salesReport.append("\n");
                            salesReport.append(String.format("Weekly Sales: ₱%.2f\n", weeklySales));
                            salesReport.append(String.format("Week: %s\n", weekRange)); // Added week range
                            salesReport.append("\n");
                            salesReport.append(String.format("Monthly Sales: ₱%.2f\n", monthlySales));
                            salesReport.append(String.format("Month: %s\n", currentMonth)); // Added current month
                            salesReport.append("\n");
                            salesReport.append(String.format("Yearly Sales: ₱%.2f\n", yearlySales));
                            salesReport.append(String.format("Year: %s\n", currentYear)); // Added current year
                            salesReport.append("-----------------------------------------\n");
                            salesReport.append("\n");
                        }
                    } catch (Exception e) {
                        Log.e("salesPopUp", "Error processing user data: " + e.getMessage(), e);
                    }
                }

                // Update the main sales report TextView
                salesTextView.setText(salesReport.toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("salesPopUp", "Database error: " + databaseError.getMessage());
            }
        });
    }

    // Generate current date
    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    // Get the week range for the current week
    private String getWeekRange() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        Date weekStart = calendar.getTime();
        calendar.add(Calendar.DATE, 6); // Add 6 days for the end of the week
        Date weekEnd = calendar.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        return sdf.format(weekStart) + " - " + sdf.format(weekEnd);
    }

    // Get the current month
    private String getCurrentMonth() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
        return monthFormat.format(new Date());
    }

    // Get the current year
    private String getCurrentYear() {
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        return yearFormat.format(new Date());
    }

    // Check if a date is within the daily, weekly, monthly, or yearly filter
    private boolean isDateInFilter(String saleDateStr, String filter) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
            Date saleDate = sdf.parse(saleDateStr);

            Calendar saleCalendar = Calendar.getInstance();
            saleCalendar.setTime(saleDate);

            Calendar currentCalendar = Calendar.getInstance();

            switch (filter) {
                case "daily":
                    return saleCalendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                            saleCalendar.get(Calendar.DAY_OF_YEAR) == currentCalendar.get(Calendar.DAY_OF_YEAR);
                case "weekly":
                    return saleCalendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                            saleCalendar.get(Calendar.WEEK_OF_YEAR) == currentCalendar.get(Calendar.WEEK_OF_YEAR);
                case "monthly":
                    return saleCalendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                            saleCalendar.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH);
                case "yearly":
                    return saleCalendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR);
                default:
                    return false;
            }
        } catch (Exception e) {
            Log.e("salesPopUp", "Error checking date filter: " + e.getMessage());
            return false;
        }
    }

    // Check and request storage permissions
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    // Save report to PDF
    private void saveReportToPdf(String reportText) {
        // Get the text from the generatedDateTextView to include the report generation date
        String generatedDate = generatedDateTextView.getText().toString();

        // Create the file in the Downloads directory
        File pdfDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!pdfDir.exists()) {
            pdfDir.mkdirs(); // Create the directory if it doesn't exist
        }

        // Define the PDF file name with a timestamp for uniqueness
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File pdfFile = new File(pdfDir, "Sales_Report_" + timestamp + ".pdf");

        try {
            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
            PdfDocument.Page page = document.startPage(pageInfo);

            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setTextSize(12);

            float yPosition = 10;

            // First, add the generated date at the top of the PDF
            canvas.drawText(generatedDate, 10f, yPosition, paint);
            yPosition += 20; // Increase yPosition after adding the date

            // Then, add the report content (from salesTextView)
            String[] reportLines = reportText.split("\n");

            for (String line : reportLines) {
                // Check if there's enough space on the page
                if (yPosition > 800) {  // Page height minus some margin
                    document.finishPage(page);

                    // Start a new page if there's no more space
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();  // Reset the canvas for the new page
                    yPosition = 10;  // Reset the yPosition for the new page
                }

                // Draw the text on the canvas
                canvas.drawText(line, 10f, yPosition, paint);
                yPosition += 20; // Increase yPosition to print on the next line
            }

            document.finishPage(page);

            // Create the file output stream and write the PDF to storage
            FileOutputStream outputStream = new FileOutputStream(pdfFile);
            document.writeTo(outputStream);
            document.close();

            // Scan the file to make it visible
            MediaScannerConnection.scanFile(this, new String[]{pdfFile.getAbsolutePath()}, null, null);

            // Notify the user that the PDF was saved successfully
            Toast.makeText(this, "PDF saved to " + pdfFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("salesPopUp", "Error saving PDF: " + e.getMessage());
            Toast.makeText(this, "Error saving PDF", Toast.LENGTH_SHORT).show();
        }
    }
}