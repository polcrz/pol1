package com.example.myapplication;// PdfCreator.java
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.TextAlignment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PdfCreator {

    public void createPdf(String dest, String reportTitle, String salesReport, String inventoryReport, String salesSummary) {
        try {
            // Initialize PDF writer
            PdfWriter writer = new PdfWriter(dest);

            // Initialize PDF document
            PdfDocument pdf = new PdfDocument(writer);

            // Initialize document
            Document document = new Document(pdf, PageSize.A4);

            // Add title
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            Paragraph title = new Paragraph(reportTitle)
                    .setFont(font)
                    .setFontSize(20)
                    .setFontColor(ColorConstants.BLUE)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            // Add sales report
            font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            Paragraph salesContent = new Paragraph(salesReport)
                    .setFont(font)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.LEFT);
            document.add(salesContent);

            // Add inventory report
            Paragraph inventoryContent = new Paragraph(inventoryReport)
                    .setFont(font)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.LEFT);
            document.add(inventoryContent);

            // Add sales summary
            Paragraph summaryContent = new Paragraph(salesSummary)
                    .setFont(font)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.LEFT);
            document.add(summaryContent);

            // Close document
            document.close();

            System.out.println("PDF created successfully");

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error creating PDF");
        }
    }
}