package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class AddActivity extends AppCompatActivity {

    EditText Product, Image, Price, Quantity;
    Button Save, Back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add);

        Product = findViewById(R.id.productName);
        Image = findViewById(R.id.imgUrl);
        Price = findViewById(R.id.price);
        Quantity = findViewById(R.id.quantity);

        Save = findViewById(R.id.btnSave);
        Back = findViewById(R.id.btnBack);

        Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInputs()) { // Only proceed if inputs are valid
                    insertData();
                    clearAll();
                }
            }
        });

        Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddActivity.this, InventoryActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(AddActivity.this, InventoryActivity.class));
        finish();
    }

    private boolean validateInputs() {
        String product = Product.getText().toString().trim();
        String image = Image.getText().toString().trim();
        String priceStr = Price.getText().toString().trim();
        String quantityStr = Quantity.getText().toString().trim();

        boolean isValid = true;

        // Validate Product Name
        if (product.isEmpty()) {
            Product.setError("Product name is required");
            Product.requestFocus();
            isValid = false;
        }

        // Validate Image URL
        if (image.isEmpty()) {
            Image.setError("Image URL is required");
            Image.requestFocus();
            isValid = false;
        }

        // Validate Price
        if (priceStr.isEmpty()) {
            Price.setError("Price is required");
            Price.requestFocus();
            isValid = false;
        } else {
            try {
                double price = Double.parseDouble(priceStr); // Try parsing as double
                if (price < 0) {
                    Price.setError("Price cannot be negative");
                    Price.requestFocus();
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                Price.setError("Invalid price format. Must be a number.");
                Price.requestFocus();
                isValid = false;
            }
        }

        // Validate Quantity
        if (quantityStr.isEmpty()) {
            Quantity.setError("Quantity is required");
            Quantity.requestFocus();
            isValid = false;
        } else {
            try {
                int quantity = Integer.parseInt(quantityStr); // Try parsing as integer
                if (quantity < 0) {
                    Quantity.setError("Quantity cannot be negative");
                    Quantity.requestFocus();
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                Quantity.setError("Invalid quantity format. Must be a number.");
                Quantity.requestFocus();
                isValid = false;
            }
        }

        if (!isValid) {
            Toast.makeText(this, "Please correct the errors above", Toast.LENGTH_SHORT).show();
        }

        return isValid;
    }

    private void insertData() {
        Map<String, Object> map = new HashMap<>();
        map.put("Product", Product.getText().toString());
        map.put("Image", Image.getText().toString());
        map.put("Price", Double.parseDouble(Price.getText().toString()));
        map.put("Quantity", Integer.parseInt(Quantity.getText().toString()));

        FirebaseDatabase.getInstance().getReference().child("Products").push()
                .setValue(map)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(AddActivity.this, "Data Saved Successfully.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(AddActivity.this, "Error while Saving Data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void clearAll() {
        Product.setText("");
        Price.setText("");
        Quantity.setText("");
        Image.setText("");
    }
}
