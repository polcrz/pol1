package com.example.myapplication;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.io.IOException;
import java.io.OutputStream;

public class InvoiceActivity extends AppCompatActivity {

    private TextView tvInvoiceDetails;
    private Button back, savePDF;
    private CardView cardViewInvoice; // Reference to the CardView containing invoice details

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.invoice); // Ensure your layout file is named invoice.xml

        // Initialize UI elements
        tvInvoiceDetails = findViewById(R.id.tvInvoiceDetails);
        back = findViewById(R.id.back);
        savePDF = findViewById(R.id.savePDF);
        cardViewInvoice = findViewById(R.id.cardViewInvoice); // Reference to the CardView

        // Set up the back button
        back.setOnClickListener(v -> {
            startActivity(new Intent(InvoiceActivity.this, orders.class));
            finish(); // Optional: Close the current activity
        });

        // Retrieve the Invoice object from the Intent
        Invoice invoice = (Invoice) getIntent().getSerializableExtra("invoice");

        if (invoice != null) {
            // Format the invoice details using the Invoice object
            StringBuilder invoiceDetails = new StringBuilder();
            invoiceDetails.append(String.format("Vendor: %s\n", invoice.getVendorName()));
            invoiceDetails.append(String.format("Invoice Number: %s\n", invoice.getInvoiceNumber()));
            invoiceDetails.append(String.format("Date: %s\n", invoice.getDateAndTime()));
            invoiceDetails.append(String.format("Order ID: %s\n", invoice.getOrderId()));
            invoiceDetails.append(String.format("Order Details: %s\n", invoice.getOrderDetails()));
            invoiceDetails.append(String.format("Item Prices: %s\n", invoice.getItemPrices()));
            invoiceDetails.append(String.format("VAT Details: %s\n", invoice.getVatDetails()));
            invoiceDetails.append(String.format("Total Price: ₱%.2f\n", invoice.getTotalPrice()));

            // Add discount details if applicable
            if (invoice.getDiscount() > 0) {
                invoiceDetails.append(String.format("Discount: ₱%.2f\n", invoice.getDiscount()));
                invoiceDetails.append(String.format("Final Price: ₱%.2f\n", invoice.getFinalPrice()));
            }

            invoiceDetails.append(String.format("Cash Payment: ₱%.2f\n", invoice.getCashPayment()));
            invoiceDetails.append(String.format("Change: ₱%.2f\n", invoice.getChange()));

            // Conditionally add PWD/Senior details
            if (invoice.getPwdName() != null && !invoice.getPwdName().isEmpty() && invoice.getPwdId() != null && !invoice.getPwdId().isEmpty()) {
                invoiceDetails.append(String.format("Name: %s\n", invoice.getPwdName()));
                if (invoice.isSenior()) {
                    invoiceDetails.append(String.format("Senior ID Number: %s\n", invoice.getPwdId()));
                } else {
                    invoiceDetails.append(String.format("PWD ID Number: %s\n", invoice.getPwdId()));
                    invoiceDetails.append(String.format("ID Expiration Date: %s\n", invoice.getExpirationDate()));
                }
                invoiceDetails.append(String.format("Place Where the ID is issued: %s\n", invoice.getIdIssued()));
            }

            // Display the invoice details in the TextView
            tvInvoiceDetails.setText(invoiceDetails.toString());
        } else {
            // Handle case where the invoice is not passed correctly
            tvInvoiceDetails.setText("Invoice details are not available.");
        }

        // Set up the savePDF button
        savePDF.setOnClickListener(v -> {
            saveCardViewAsPDF(invoice);
        });
    }

    // Method to capture the content inside the CardView and save it as a PDF
    private void saveCardViewAsPDF(Invoice invoice) {
        if (invoice == null) {
            Toast.makeText(this, "Invoice is not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a bitmap with the same size as the CardView
        Bitmap bitmap = Bitmap.createBitmap(cardViewInvoice.getWidth(), cardViewInvoice.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        cardViewInvoice.draw(canvas);

        // Create a PdfDocument to save the content
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        // Draw the bitmap into the PDF page
        page.getCanvas().drawBitmap(bitmap, 0, 0, null);

        // Finish the page and write the PDF content
        pdfDocument.finishPage(page);

        // Saving PDF to the Downloads directory
        String fileName = "Invoice_" + invoice.getInvoiceNumber() + ".pdf";
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        try {
            // Insert the PDF into external storage
            OutputStream outputStream = getContentResolver().openOutputStream(
                    getContentResolver().insert(MediaStore.Files.getContentUri("external"), contentValues));

            if (outputStream != null) {
                pdfDocument.writeTo(outputStream);
                Toast.makeText(this, "Invoice saved as PDF", Toast.LENGTH_SHORT).show();
                outputStream.close();
            } else {
                Toast.makeText(this, "Error saving PDF", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving PDF", Toast.LENGTH_SHORT).show();
        } finally {
            pdfDocument.close();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(InvoiceActivity.this, orders.class));
        finish();
    }
}