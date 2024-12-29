package com.example.myapplication;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

import com.example.myapplication.databinding.ActivityOrdersBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class orders extends BaseActivity {

    private ActivityOrdersBinding binding;
    private DatabaseReference databaseReference;
    private TextView tvOrderId, tvOrderDetails, tvItemPrices, tvOrderPrice, tvVatDetails;
    private Button btnOrderComplete, btnCancelOrder;
    private EditText etCashPayment;
    private static final double VAT_RATE = 12.0;

    // Declare this variable at the class level to store the original total price
    private double originalTotalPrice = 0.0;

    // Declare CheckBoxes
    private CheckBox cbPwd;
    private CheckBox cbSenior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Inflate layout using ViewBinding
        binding = ActivityOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupDrawer(binding.getRoot());

        // Set title for ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Orders");
        } else {
            Log.e("Orders", "ActionBar is null!");
        }

        // Initialize Firebase database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // Initialize UI elements
        tvOrderId = findViewById(R.id.tvOrderId);
        tvOrderDetails = findViewById(R.id.tvOrderDetails);
        tvItemPrices = findViewById(R.id.tvItemPrices);
        tvOrderPrice = findViewById(R.id.tvOrderPrice);
        tvVatDetails = findViewById(R.id.tvVatDetails);
        btnOrderComplete = findViewById(R.id.btnCompleteOrder);
        etCashPayment = findViewById(R.id.etCashPayment);
        btnCancelOrder = findViewById(R.id.btnCancelOrder);

        // Initialize CheckBoxes
        cbPwd = findViewById(R.id.cbPwd);
        cbSenior = findViewById(R.id.cbSenior);

        LinearLayout pwdSeniorLayout = findViewById(R.id.pwdSeniorLayout);
        EditText etPwdSeniorName = findViewById(R.id.etPwdSeniorName);
        EditText etPwdSeniorId = findViewById(R.id.etPwdSeniorId);
        EditText etIdIssued = findViewById(R.id.idIssued);
        EditText etExpirationDate = findViewById(R.id.expirationDate);

        etExpirationDate.setOnClickListener(v -> {
            // Get the current date
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            // Create a DatePickerDialog
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // Update the calendar with the selected date
                        calendar.set(selectedYear, selectedMonth, selectedDay);

                        // Format the selected date
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        String formattedDate = sdf.format(calendar.getTime());

                        // Display the formatted date in the EditText
                        etExpirationDate.setText(formattedDate);
                    },
                    year, month, day
            );

            // Show the DatePickerDialog
            datePickerDialog.show();
        });

        // Handle checkbox toggle for PWD discount
        cbPwd.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                cbSenior.setChecked(false);
                pwdSeniorLayout.setVisibility(View.VISIBLE);
                etExpirationDate.setVisibility(View.VISIBLE);
                applyDiscount();
            } else {
                pwdSeniorLayout.setVisibility(View.GONE);
                resetPrice();
            }
        });

        // Handle checkbox toggle for Senior discount
        cbSenior.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                cbPwd.setChecked(false);
                pwdSeniorLayout.setVisibility(View.VISIBLE);
                etExpirationDate.setVisibility(View.GONE);
                applyDiscount();
            } else {
                pwdSeniorLayout.setVisibility(View.GONE);
                resetPrice();
            }
        });

        // Fetch order data from Firebase
        fetchOrderData();

        // Set up the order complete button click listener
        btnOrderComplete.setOnClickListener(v -> {
            String cashPaymentText = etCashPayment.getText().toString().trim();
            if (cashPaymentText.isEmpty()) {
                Toast.makeText(orders.this, "Please enter the cash payment amount.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (cbPwd.isChecked() || cbSenior.isChecked()) {
                String pwdName = etPwdSeniorName.getText().toString().trim();
                String pwdId = etPwdSeniorId.getText().toString().trim();
                String idIssued = etIdIssued.getText().toString().trim();
                String expirationDate = etExpirationDate.getText().toString().trim();

                if (pwdName.isEmpty() || pwdId.isEmpty() || idIssued.isEmpty() || (cbPwd.isChecked() && expirationDate.isEmpty())) {
                    Toast.makeText(orders.this, "Please fill in all fields for PWD/Senior.", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean isSenior = cbSenior.isChecked();
                generateSalesInvoice(cbPwd.isChecked() || isSenior, pwdName, pwdId, idIssued, isSenior ? "" : expirationDate, isSenior);
            } else {
                generateSalesInvoice(false, "", "", "", "", false);
            }
        });

        // Set up the cancel order button click listener
        btnCancelOrder.setOnClickListener(v -> cancelOrder());
    }

    private double calculateVat(int priceWithVat) {
        return priceWithVat * (VAT_RATE / 100);
    }

    private void resetPrice() {
        if (!cbPwd.isChecked() && !cbSenior.isChecked()) {
            tvOrderPrice.setText("Total Price: ₱" + String.format("%.2f", originalTotalPrice));
        }
    }

    private void cancelOrder() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("CancelOrder", "No authenticated user found.");
            return;
        }

        String userId = currentUser.getUid();
        String orderId = tvOrderId.getText().toString().replace("Order ID: #", "").trim();

        DatabaseReference orderRef = databaseReference.child(userId).child("orders").child(orderId);
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("products");

        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot orderSnapshot) {
                if (orderSnapshot.exists()) {
                    // Iterate through the products in the order
                    for (DataSnapshot productSnapshot : orderSnapshot.child("Products").getChildren()) {
                        String productId = productSnapshot.getKey();
                        Integer quantityToReturn = productSnapshot.child("Quantity").getValue(Integer.class);

                        if (productId != null && quantityToReturn != null && quantityToReturn > 0) {
                            // Fetch the product data to update its quantity
                            productsRef.child(productId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot productSnapshot) {
                                    if (productSnapshot.exists()) {
                                        Integer currentQuantity = productSnapshot.child("Quantity").getValue(Integer.class);
                                        if (currentQuantity != null) {
                                            int updatedQuantity = currentQuantity + quantityToReturn;

                                            // Update the product quantity
                                            productsRef.child(productId).child("Quantity").setValue(updatedQuantity)
                                                    .addOnSuccessListener(aVoid -> Log.d("CancelOrder", "Product quantity updated successfully for: " + productId))
                                                    .addOnFailureListener(e -> Log.e("CancelOrder", "Failed to update product quantity.", e));
                                        } else {
                                            Log.e("CancelOrder", "Current quantity is null for product: " + productId);
                                        }
                                    } else {
                                        Log.e("CancelOrder", "Product not found in inventory: " + productId);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Log.e("CancelOrder", "Failed to fetch product data: " + databaseError.getMessage());
                                }
                            });
                        } else {
                            Log.e("CancelOrder", "Invalid product ID or quantity for product: " + productId);
                        }
                    }

                    // Update the order status to "cancelled"
                    orderRef.child("Status").setValue("cancelled")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(orders.this, "Order cancelled and products returned to inventory.", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(orders.this, SalesActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> Log.e("CancelOrder", "Failed to update order status.", e));
                } else {
                    Log.e("CancelOrder", "Order not found.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("CancelOrder", "Failed to fetch order data: " + databaseError.getMessage());
            }
        });
    }

    private void fetchOrderData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            databaseReference.child(userId).child("orders").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean hasPendingOrder = false;

                    for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                        String orderStatus = orderSnapshot.child("Status").getValue(String.class);
                        if (orderStatus != null && orderStatus.equals("pending")) {
                            hasPendingOrder = true;
                            String orderId = orderSnapshot.getKey();
                            Integer totalOrderPrice = orderSnapshot.child("TotalOrderPrice").getValue(Integer.class);

                            StringBuilder orderDetails = new StringBuilder();
                            StringBuilder itemPrices = new StringBuilder();
                            StringBuilder vatDetails = new StringBuilder();

                            for (DataSnapshot productSnapshot : orderSnapshot.child("Products").getChildren()) {
                                String productName = productSnapshot.child("Product").getValue(String.class);
                                Integer quantity = productSnapshot.child("Quantity").getValue(Integer.class);
                                Integer price = productSnapshot.child("TotalPrice").getValue(Integer.class);

                                // Perform null checks
                                if (productName != null && quantity != null && price != null) {
                                    double vatPerProduct = calculateVat(price); // Calculate VAT
                                    orderDetails.append(quantity).append("x ").append(productName).append(", ");
                                    itemPrices.append(productName).append(" - ₱").append(price).append(", "); // Full price
                                    vatDetails.append("VAT for ").append(productName).append(": ₱").append(String.format("%.2f", vatPerProduct)).append(", ");
                                }
                            }

                            tvOrderId.setText("Order ID: #" + orderId);
                            tvOrderDetails.setText("Order Details: " + orderDetails.toString());
                            tvItemPrices.setText("Prices: " + itemPrices.toString());
                            tvVatDetails.setText("VAT Details: " + vatDetails.toString());
                            // Save original price for discount logic
                            originalTotalPrice = totalOrderPrice != null ? totalOrderPrice : 0.0;
                            tvOrderPrice.setText("Total Price: ₱" + (originalTotalPrice != 0.0 ? originalTotalPrice : "N/A"));
                            break;
                        }
                    }

                    if (!hasPendingOrder) {
                        Intent intent = new Intent(orders.this, SalesActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("Firebase", "Failed to fetch data: " + databaseError.getMessage());
                }
            });
        } else {
            Log.e("Firebase", "No authenticated user found.");
        }
    }



    private double calculateDiscount(DataSnapshot orderSnapshot) {
        double discount = 0.0;

        // Iterate through the products in the order
        for (DataSnapshot productSnapshot : orderSnapshot.child("Products").getChildren()) {
            String productName = productSnapshot.child("Product").getValue(String.class);
            Integer quantity = productSnapshot.child("Quantity").getValue(Integer.class);
            Integer totalPrice = productSnapshot.child("TotalPrice").getValue(Integer.class); // Get TotalPrice for the product

            // Log values for debugging
            Log.d("Discount", "Product: " + productName + ", Quantity: " + quantity + ", TotalPrice: " + totalPrice);

            if (productName != null && quantity != null && totalPrice != null) {
                // Apply 20% discount on the TotalPrice for one unit
                discount += totalPrice * 0.20;  // Apply discount on the total price of the product
            }
        }

        Log.d("Discount", "Total Discount: " + discount); // Log the total discount calculated
        return discount;
    }

    private void applyDiscount() {
        if (originalTotalPrice <= 0) {
            Toast.makeText(this, "Order price not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch order data from Firebase to apply discount logic
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseReference.child("users").child(userId).child("orders").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // Assume we have a single order for simplicity
                    for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                        // Fetch TotalOrderPrice from the order level
                        Double totalOrderPrice = orderSnapshot.child("TotalOrderPrice").getValue(Double.class);

                        if (totalOrderPrice != null) {
                            // Log the total order price
                            Log.d("Discount", "Total Order Price before discount: " + totalOrderPrice);

                            // Apply discount to products
                            double discount = calculateDiscount(orderSnapshot);
                            double discountedPrice = totalOrderPrice - discount;

                            // Log the discounted price
                            Log.d("Discount", "Total Order Price after discount: " + discountedPrice);

                            // Update the price displayed in the TextView
                            tvOrderPrice.setText("Total Price: ₱" + String.format("%.2f", discountedPrice));
                        } else {
                            Log.e("Firebase", "TotalOrderPrice is missing in the database.");
                        }

                        break; // Assuming there's only one order
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("Firebase", "Failed to fetch data: " + databaseError.getMessage());
                }
            });
        }
    }

    private double calculateDiscountForInvoice(String orderId) {
        final double[] discount = {0.0}; // Using an array to modify the discount value inside the listener
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference orderRef = databaseReference.child(userId).child("orders").child(orderId);

            orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // Iterate through the products in the order
                    for (DataSnapshot productSnapshot : dataSnapshot.child("Products").getChildren()) {
                        String productName = productSnapshot.child("Product").getValue(String.class);
                        Integer quantity = productSnapshot.child("Quantity").getValue(Integer.class);
                        Integer unitPrice = productSnapshot.child("Price").getValue(Integer.class);  // Use unit price

                        if (productName != null && quantity != null && unitPrice != null) {
                            // Apply the discount for just one quantity (20% of unit price)
                            discount[0] += unitPrice * 0.20; // Apply 20% discount on unit price (one quantity only)
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("Firebase", "Failed to fetch data: " + databaseError.getMessage());
                }
            });
        }

        return discount[0];
    }




    private void generateAndSaveInvoice(String vendorName, String currentDateAndTime, String userId, boolean isPwdSenior,
                                        String pwdName, String pwdId, String idIssued, String expirationDate, boolean isSenior) {
        String invoiceNumber = "INV-" + System.currentTimeMillis();
        String orderId = tvOrderId.getText().toString().replace("Order ID: #", "").trim();
        String orderDetails = tvOrderDetails.getText().toString().replace("Order Details: ", "").trim();
        String itemPrices = tvItemPrices.getText().toString().replace("Prices: ", "").trim();
        String vatDetails = tvVatDetails.getText().toString().replace("VAT Details: ", "").trim();
        String totalPriceText = tvOrderPrice.getText().toString().replace("Total Price: ₱", "").trim();

        double totalPrice = Double.parseDouble(totalPriceText); // This includes VAT

        // Calculate the discount
        double discount = calculateDiscountForInvoice(orderId);
        double finalPrice = totalPrice - discount; // Subtract the discount

        double cashPayment = Double.parseDouble(etCashPayment.getText().toString());
        double change = cashPayment - finalPrice;

        // Round values to 2 decimal places for precision
        totalPrice = Math.round(totalPrice * 100.0) / 100.0;
        discount = Math.round(discount * 100.0) / 100.0;
        finalPrice = Math.round(finalPrice * 100.0) / 100.0;
        cashPayment = Math.round(cashPayment * 100.0) / 100.0;
        change = Math.round(change * 100.0) / 100.0;

        if (cashPayment < finalPrice) {
            Toast.makeText(orders.this, "Insufficient cash payment.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create an Invoice object
        Invoice invoice = new Invoice(invoiceNumber, vendorName, currentDateAndTime, orderId, orderDetails, itemPrices,
                vatDetails, totalPrice, discount, finalPrice, cashPayment, change, pwdName, pwdId, idIssued, expirationDate, isSenior);

        // Save the invoice under the order node in Firebase
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("orders").child(orderId);
        ordersRef.child("Status").setValue("completed"); // Update the order status to completed
        ordersRef.child("Invoice").setValue(invoice); // Save the invoice data

        // Launch InvoiceActivity with invoice details
        Intent intent = new Intent(orders.this, InvoiceActivity.class);
        intent.putExtra("invoice", invoice); // Pass the Invoice object
        startActivity(intent);
    }

    private void generateSalesInvoice(boolean isPwdSenior, String pwdName, String pwdId, String idIssued, String expirationDate, boolean isSenior) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String vendorName = dataSnapshot.child("name").getValue(String.class);
                generateAndSaveInvoice(vendorName, getCurrentDateAndTime(), userId, isPwdSenior, pwdName, pwdId, idIssued, expirationDate, isSenior);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Invoice", "Error fetching vendor details: " + databaseError.getMessage());
            }
        });
    }



    private String getCurrentDateAndTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(orders.this, SalesActivity.class));
        finish();
    }
}