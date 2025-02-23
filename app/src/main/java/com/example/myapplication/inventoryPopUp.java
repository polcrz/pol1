package com.example.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class inventoryPopUp extends AppCompatActivity {

    private Button back, addProductBtn;
    private DatabaseReference usersRef;
    private LinearLayout inventoryLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_pop_up);

        back = findViewById(R.id.backBtn);
        addProductBtn = findViewById(R.id.addProductBtn);
        inventoryLayout = findViewById(R.id.inventoryLayout);

        back.setOnClickListener(v -> finish());

        addProductBtn.setOnClickListener(v -> showVendorSelectionDialog((vendorId, vendorName) -> showAddProductDialog(vendorId, vendorName)));

        usersRef = FirebaseDatabase.getInstance().getReference().child("users");

        fetchVendorsInventory();
    }

    private void fetchVendorsInventory() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                inventoryLayout.removeAllViews(); // Clear existing views

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String role = userSnapshot.child("role").getValue(String.class);
                    if ("vendor".equalsIgnoreCase(role)) {
                        String vendorName = userSnapshot.child("name").getValue(String.class);
                        if (vendorName == null) {
                            vendorName = "Unknown Vendor";
                        }

                        Map<String, Integer> productSales = new HashMap<>();
                        DataSnapshot productsSnapshot = userSnapshot.child("products");

                        if (productsSnapshot.exists()) {
                            TextView vendorTextView = new TextView(inventoryPopUp.this);
                            vendorTextView.setText(String.format("\n   Vendor: %s ", vendorName));
                            vendorTextView.setTextSize(24); // Increase text size
                            vendorTextView.setPadding(16, 16, 16, 16); // Add padding to the bottom
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                            params.setMargins(10, 10, 10, 10); // Set margin to 10
                            vendorTextView.setLayoutParams(params);
                            inventoryLayout.addView(vendorTextView);

                            DataSnapshot ordersSnapshot = userSnapshot.child("orders");
                            for (DataSnapshot orderSnapshot : ordersSnapshot.getChildren()) {
                                String orderStatus = orderSnapshot.child("Status").getValue(String.class);
                                if ("completed".equalsIgnoreCase(orderStatus)) {
                                    DataSnapshot productsSoldSnapshot = orderSnapshot.child("Products");
                                    for (DataSnapshot productSoldSnapshot : productsSoldSnapshot.getChildren()) {
                                        String productName = productSoldSnapshot.child("Product").getValue(String.class);
                                        Integer quantitySold = productSoldSnapshot.child("Quantity").getValue(Integer.class);

                                        if (productName != null && quantitySold != null) {
                                            int currentTotal = productSales.getOrDefault(productName, 0);
                                            productSales.put(productName, currentTotal + quantitySold);
                                        }
                                    }
                                }
                            }

                            for (DataSnapshot productSnapshot : productsSnapshot.getChildren()) {
                                String productName = productSnapshot.child("Product").getValue(String.class);
                                Integer quantity = productSnapshot.child("Quantity").getValue(Integer.class);
                                Double price = productSnapshot.child("Price").getValue(Double.class);
                                String image = productSnapshot.child("Image").getValue(String.class);
                                final String productId = productSnapshot.getKey(); // Get the unique product ID

                                if (productName != null && quantity != null && price != null) {
                                    // Create a CardView for each product
                                    View productCard = LayoutInflater.from(inventoryPopUp.this).inflate(R.layout.product_card, inventoryLayout, false);

                                    TextView productTextView = productCard.findViewById(R.id.productDetails);
                                    productTextView.setText(String.format("Product: %s\nQuantity: %d\nPrice: PHP %.2f\nTotal Sold: %d pcs",
                                            productName, quantity, price, productSales.getOrDefault(productName, 0)));

                                    ImageView productImageView = productCard.findViewById(R.id.productImageView);
                                    if (image != null && !image.isEmpty()) {
                                        // Load image using Glide
                                        Glide.with(inventoryPopUp.this).load(image).into(productImageView);
                                    }

                                    Button editButton = productCard.findViewById(R.id.editProductBtn);
                                    editButton.setOnClickListener(v -> handleEditButtonClick(userSnapshot.getKey(), productId, productName, quantity, price, image)); // Pass vendorId and productId

                                    Button deleteButton = productCard.findViewById(R.id.deleteProductBtn);
                                    deleteButton.setOnClickListener(v -> handleDeleteButtonClick(userSnapshot.getKey(), productId)); // Pass vendorId and productId

                                    // Check for low stock and update UI
                                    if ((productName.equalsIgnoreCase("Siomai") && quantity < 100) || (!productName.equalsIgnoreCase("Siomai") && quantity < 100)) {
                                        productCard.setBackgroundResource(R.drawable.border_red); // Set the red border
                                        showLowStockDialog(productName);
                                    }

                                    inventoryLayout.addView(productCard);
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("inventoryPopUp", "Database error: " + databaseError.getMessage());
            }
        });
    }

    private void showVendorSelectionDialog(OnVendorSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Vendor");

        // Create a layout to display the list of vendors
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Create the layout before the loop to ensure it's effectively final
                LinearLayout layout = new LinearLayout(inventoryPopUp.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(16, 16, 16, 16);

                AlertDialog.Builder builder = new AlertDialog.Builder(inventoryPopUp.this);

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String role = userSnapshot.child("role").getValue(String.class);
                    if ("vendor".equalsIgnoreCase(role)) {
                        final String vendorName = userSnapshot.child("name").getValue(String.class);
                        final String vendorId = userSnapshot.getKey();

                        // Create a button for each vendor
                        Button vendorButton = new Button(inventoryPopUp.this);
                        vendorButton.setText(vendorName != null ? vendorName : "Unknown Vendor");
                        vendorButton.setOnClickListener(v -> listener.onVendorSelected(vendorId, vendorName != null ? vendorName : "Unknown Vendor"));
                        layout.addView(vendorButton);
                    }
                }

                builder.setView(layout);
                builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
                builder.show();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("inventoryPopUp", "Database error: " + databaseError.getMessage());
            }
        });
    }

    interface OnVendorSelectedListener {
        void onVendorSelected(String vendorId, String vendorName);
    }

    private void showAddProductDialog(String vendorId, String vendorName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Product to " + vendorName);

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, (ViewGroup) findViewById(android.R.id.content), false);
        final TextInputEditText inputProductName = viewInflated.findViewById(R.id.inputProductName);
        final TextInputEditText inputQuantity = viewInflated.findViewById(R.id.inputQuantity);
        final TextInputEditText inputPrice = viewInflated.findViewById(R.id.inputPrice);
        final TextInputEditText inputImage = viewInflated.findViewById(R.id.inputImage);

        builder.setView(viewInflated);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                String productName = inputProductName.getText().toString();
                int quantity = Integer.parseInt(inputQuantity.getText().toString());
                double price = Double.parseDouble(inputPrice.getText().toString());
                String image = inputImage.getText().toString();
                addProductToVendor(vendorId, productName, quantity, price, image);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void addProductToVendor(String vendorId, String productName, int quantity, double price, String image) {
        DatabaseReference vendorProductsRef = usersRef.child(vendorId).child("products").push();
        Map<String, Object> productData = new HashMap<>();
        productData.put("Product", productName);
        productData.put("Quantity", quantity);
        productData.put("Price", price);
        if (image != null && !image.isEmpty()) {
            productData.put("Image", image);
        }
        vendorProductsRef.setValue(productData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d("Firebase", "Product added successfully to vendor: " + vendorId);
                } else {
                    Log.d("Firebase", "Failed to add product to vendor: " + vendorId);
                }
            }
        });
    }

    private void showEditProductDialog(String vendorId, String productId, String productName, int currentQuantity, double price, String image) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Product");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_edit_product, (ViewGroup) findViewById(android.R.id.content), false);
        final TextInputEditText inputProductName = viewInflated.findViewById(R.id.inputProductName);
        final TextInputEditText inputCurrentQuantity = viewInflated.findViewById(R.id.inputCurrentQuantity);
        final TextInputEditText inputAmount = viewInflated.findViewById(R.id.inputAmount);
        final TextInputEditText inputPrice = viewInflated.findViewById(R.id.inputPrice);
        final TextInputEditText inputImage = viewInflated.findViewById(R.id.inputImage);
        Button plusButton = viewInflated.findViewById(R.id.plusButton);
        Button minusButton = viewInflated.findViewById(R.id.minusButton);

        // Set current values
        inputProductName.setText(productName);
        inputCurrentQuantity.setText(String.valueOf(currentQuantity));
        inputPrice.setText(String.valueOf(price));
        inputImage.setText(String.valueOf(image));

        // Handle plus button click
        plusButton.setOnClickListener(v -> {
            int amount = Integer.parseInt(inputAmount.getText().toString());
            int newQuantity = currentQuantity + amount;
            inputCurrentQuantity.setText(String.valueOf(newQuantity));
        });

        // Handle minus button click
        minusButton.setOnClickListener(v -> {
            int amount = Integer.parseInt(inputAmount.getText().toString());
            int newQuantity = currentQuantity - amount;
            if (newQuantity >= 0) {
                inputCurrentQuantity.setText(String.valueOf(newQuantity));
            }
        });

        builder.setView(viewInflated);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                String newProductName = inputProductName.getText().toString();
                int newQuantity = Integer.parseInt(inputCurrentQuantity.getText().toString());
                double newPrice = Double.parseDouble(inputPrice.getText().toString());
                String newImage = inputImage.getText().toString();
                editProduct(vendorId, productId, newProductName, newQuantity, newPrice, newImage);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void editProduct(String vendorId, String productId, String productName, int quantity, double price, String image) {
        DatabaseReference productRef = usersRef.child(vendorId).child("products").child(productId);

        Map<String, Object> productUpdates = new HashMap<>();
        productUpdates.put("Product", productName);
        productUpdates.put("Quantity", quantity);
        productUpdates.put("Price", price);
        productUpdates.put("Image", image);

        productRef.updateChildren(productUpdates);
    }

    private void deleteProduct(String vendorId, String productId) {
        DatabaseReference productRef = usersRef.child(vendorId).child("products").child(productId);
        productRef.removeValue();
    }

    private void handleEditButtonClick(String vendorId, String productId, String productName, int quantity, double price, String image) {
        showEditProductDialog(vendorId, productId, productName, quantity, price, image);
    }

    private void handleDeleteButtonClick(String vendorId, String productId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete this product?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteProduct(vendorId, productId);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void showLowStockDialog(String productName) {
        new AlertDialog.Builder(this)
                .setTitle("Low Stock Warning")
                .setMessage("Low stock for product: " + productName)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }
}