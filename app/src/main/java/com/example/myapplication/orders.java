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

    private TextView tvDiscount;

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
        tvDiscount = findViewById(R.id.tvDiscount);

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
            tvDiscount.setText(""); // Clear the discount TextView
            tvOrderPrice.setTag(0.0); // Clear the stored discount
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


    private void applyDiscount() {
        if (originalTotalPrice <= 0) {
            Toast.makeText(this, "Order price not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String orderId = tvOrderId.getText().toString().replace("Order ID: #", "").trim();
            DatabaseReference orderRef = databaseReference.child(userId).child("orders").child(orderId);

            orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot orderSnapshot) {
                    double highestDiscount = calculateHighestDiscount(orderSnapshot);
                    double discountedPrice = originalTotalPrice - highestDiscount;

                    // Update the price displayed in the TextView
                    tvOrderPrice.setText("Total Price: ₱" + String.format("%.2f", discountedPrice));
                    // Display the discount in the discount TextView
                    tvDiscount.setText(String.format("Discount: ₱%.2f", highestDiscount));
                    // Store the discount for later use in the invoice
                    tvOrderPrice.setTag(highestDiscount);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("Firebase", "Failed to fetch order data: " + databaseError.getMessage());
                }
            });
        } else {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
        }
    }

    private double calculateHighestDiscount(DataSnapshot orderSnapshot) {
        double highestDiscount = 0.0;

        // Iterate through the products in the order
        for (DataSnapshot productSnapshot : orderSnapshot.child("Products").getChildren()) {
            String productName = productSnapshot.child("Product").getValue(String.class);
            Integer unitPrice = productSnapshot.child("Price").getValue(Integer.class);
            Integer quantity = productSnapshot.child("Quantity").getValue(Integer.class);

            if (unitPrice != null && productName != null && quantity != null) {
                double discount = 0.0;

                // Calculate potential discount for siomai
                if (productName.equalsIgnoreCase("siomai") && quantity >= 4) {
                    discount = unitPrice * 4 * 0.20;
                } else {
                    // Calculate 20% discount for other products
                    discount = unitPrice * 0.20;
                }

                // Keep track of the highest discount
                if (discount > highestDiscount) {
                    highestDiscount = discount;
                }
            }
        }

        return highestDiscount;
    }

    private double[] calculateDiscounts(DataSnapshot orderSnapshot) {
        final double[] highestPrice = {0.0};
        final double[] siomaiDiscount = {0.0};

        // Iterate through the products in the order
        for (DataSnapshot productSnapshot : orderSnapshot.child("Products").getChildren()) {
            String productName = productSnapshot.child("Product").getValue(String.class);
            Integer unitPrice = productSnapshot.child("Price").getValue(Integer.class);
            Integer quantity = productSnapshot.child("Quantity").getValue(Integer.class);

            if (unitPrice != null && productName != null && quantity != null) {
                // Check for highest price product
                if (unitPrice > highestPrice[0]) {
                    highestPrice[0] = unitPrice;
                }

                // Check for special discount on siomai
                if (productName.equalsIgnoreCase("siomai") && quantity >= 4) {
                    siomaiDiscount[0] = unitPrice * 4 * 0.20;
                }
            }
        }

        // Apply 20% discount to the highest initial price
        double highestPriceDiscount = highestPrice[0] * 0.20;

        return new double[]{highestPriceDiscount, siomaiDiscount[0]};
    }

    private void generateAndSaveInvoice(String vendorName, String currentDateAndTime, String userId, boolean isPwdSenior,
                                        String pwdName, String pwdId, String idIssued, String expirationDate, boolean isSenior) {
        String invoiceNumber = "INV-" + System.currentTimeMillis();
        String orderId = tvOrderId.getText().toString().replace("Order ID: #", "").trim();
        String orderDetails = tvOrderDetails.getText().toString().replace("Order Details: ", "").trim();
        String itemPrices = tvItemPrices.getText().toString().replace("Prices: ", "").trim();
        String vatDetails = tvVatDetails.getText().toString().replace("VAT Details: ", "").trim();
        String totalPriceText = tvOrderPrice.getText().toString().replace("Total Price: ₱", "").trim();

        // Use a single-element array to hold the total price
        final double[] totalPrice = {0.0};
        try {
            totalPrice[0] = Double.parseDouble(totalPriceText); // This includes VAT and the applied discount
        } catch (NumberFormatException e) {
            Log.e("Invoice", "Invalid total price format: " + totalPriceText, e);
            Toast.makeText(orders.this, "Invalid total price format.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch the discount from the tag
        double discount = tvOrderPrice.getTag() != null ? (double) tvOrderPrice.getTag() : 0.0;

        // Update the discount TextView
        tvDiscount.setText(String.format("Discount: ₱%.2f", discount));

        // Fetch order data from Firebase
        DatabaseReference orderRef = databaseReference.child(userId).child("orders").child(orderId);

        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot orderSnapshot) {
                // Since the discount is already applied in totalPrice, we do not need to apply it again
                double finalPrice = totalPrice[0];

                double cashPayment = 0.0;
                try {
                    cashPayment = Double.parseDouble(etCashPayment.getText().toString());
                } catch (NumberFormatException e) {
                    Log.e("Invoice", "Invalid cash payment format: " + etCashPayment.getText().toString(), e);
                    Toast.makeText(orders.this, "Invalid cash payment format.", Toast.LENGTH_SHORT).show();
                    return;
                }
                double change = cashPayment - finalPrice;

                // Round values to 2 decimal places for precision
                finalPrice = Math.round(finalPrice * 100.0) / 100.0;
                cashPayment = Math.round(cashPayment * 100.0) / 100.0;
                change = Math.round(change * 100.0) / 100.0;

                if (cashPayment < finalPrice) {
                    Toast.makeText(orders.this, "Insufficient cash payment.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create an Invoice object
                Invoice invoice = new Invoice(invoiceNumber, vendorName, currentDateAndTime, orderId, orderDetails, itemPrices,
                        vatDetails, originalTotalPrice, discount, finalPrice, cashPayment, change, pwdName, pwdId, idIssued, expirationDate, isSenior);

                // Save the invoice under the order node in Firebase
                DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("orders").child(orderId);
                ordersRef.child("Status").setValue("completed"); // Update the order status to completed
                ordersRef.child("Invoice").setValue(invoice); // Save the invoice data

                // Launch InvoiceActivity with invoice details
                Intent intent = new Intent(orders.this, InvoiceActivity.class);
                intent.putExtra("invoice", invoice); // Pass the Invoice object
                startActivity(intent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Firebase", "Failed to fetch order data: " + databaseError.getMessage());
            }
        });
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