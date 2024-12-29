package com.example.myapplication;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.Model.SalesModel;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SalesAdapter extends FirebaseRecyclerAdapter<SalesModel, SalesAdapter.myViewHolder> {

    // HashMap to track selected quantities
    private HashMap<String, Integer> selectedQuantities = new HashMap<>();

    // Constructor
    public SalesAdapter(@NonNull FirebaseRecyclerOptions<SalesModel> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull myViewHolder holder, int position, @NonNull SalesModel model) {
        // Populate the views
        holder.Product.setText(model.getProduct());
        holder.Quantity.setText(String.format("Stock: %d", model.getQuantity()));

        // Show the price based on product and quantity
        if ("Siomai".equalsIgnoreCase(model.getProduct())) {
            // For Siomai, base the price on the unit price and the quantity
            double unitPrice = model.getPrice(); // e.g., ₱15 per piece for Siomai
            double priceFor4Pieces = unitPrice * 4; // Default price for 4 pieces
            holder.Price.setText(String.format("Price: ₱%.2f", priceFor4Pieces));
        } else {
            // For other products, show price as unit price * quantity
            holder.Price.setText(String.format("Price: ₱%.2f", model.getPrice()));
        }

        // Load image with Glide
        Glide.with(holder.Image.getContext())
                .load(model.getImage())
                .placeholder(com.firebase.ui.database.R.drawable.common_google_signin_btn_icon_dark)
                .circleCrop()
                .error(com.firebase.ui.database.R.drawable.common_google_signin_btn_icon_light)
                .into(holder.Image);

        // Initialize count
        final int[] count = {0};  // Start at 0 for all products
        holder.count.setText(String.valueOf(count[0]));

        // Disable buttons if quantity is zero
        if (model.getQuantity() == 0) {
            holder.plusBtn.setEnabled(false);
            holder.minusBtn.setEnabled(false);
            holder.Quantity.setText("Out of Stock");
            holder.Quantity.setTextColor(holder.Quantity.getContext().getResources().getColor(android.R.color.holo_red_dark));
        } else {
            holder.plusBtn.setEnabled(true);
            holder.minusBtn.setEnabled(true);

            // Handle "plus" button click - Only for Siomai
            holder.plusBtn.setOnClickListener(v -> {
                if ("Siomai".equalsIgnoreCase(model.getProduct())) {
                    if (count[0] == 0) {
                        // For Siomai, the first press sets the quantity to 4
                        count[0] = 4;
                    } else if (count[0] < model.getQuantity()) {
                        // After the first press, increase by 1 for Siomai
                        count[0]++;
                    }

                    holder.count.setText(String.valueOf(count[0]));

                    // Price logic for Siomai
                    double unitPrice = model.getPrice(); // e.g., ₱15 per piece for Siomai
                    double totalPrice = count[0] > 4 ? (unitPrice * 4) + ((count[0] - 4) * unitPrice) : (unitPrice * count[0]);
                    holder.Price.setText(String.format("Price: ₱%.2f", totalPrice));

                } else {
                    // For other products, increase by 1 freely without any special logic
                    if (count[0] < model.getQuantity()) {
                        count[0]++;
                        holder.count.setText(String.valueOf(count[0]));

                        // Price logic for other products (unit price * quantity)
                        double totalPrice = model.getPrice() * count[0];
                        holder.Price.setText(String.format("Price: ₱%.2f", totalPrice));
                    }
                }

                selectedQuantities.put(getRef(holder.getAdapterPosition()).getKey(), count[0]);
            });

            // Handle "minus" button click - Applies to all products
            holder.minusBtn.setOnClickListener(v -> {
                if ("Siomai".equalsIgnoreCase(model.getProduct())) {
                    if (count[0] == 4) {
                        // For Siomai, if quantity is 4, pressing minus sets it to 0
                        count[0] = 0;
                    } else if (count[0] > 4) {
                        // For Siomai, if quantity > 4, reduce by 1
                        count[0]--;
                    }

                    holder.count.setText(String.valueOf(count[0]));

                    // Price logic for Siomai
                    double unitPrice = model.getPrice(); // e.g., ₱15 per piece for Siomai
                    double totalPrice = count[0] > 4 ? (unitPrice * 4) + ((count[0] - 4) * unitPrice) : (unitPrice * 4);
                    holder.Price.setText(String.format("Price: ₱%.2f", totalPrice));

                } else {
                    // For other products, reduce by 1 freely and show unit price even when quantity is 0
                    if (count[0] > 0) {
                        count[0]--;
                        holder.count.setText(String.valueOf(count[0]));
                    }

                    // Price for other products (unit price * quantity), even if quantity is 0
                    holder.Price.setText(String.format("Price: ₱%.2f", model.getPrice()));
                }

                if (count[0] == 0) {
                    selectedQuantities.remove(getRef(holder.getAdapterPosition()).getKey());
                } else {
                    selectedQuantities.put(getRef(holder.getAdapterPosition()).getKey(), count[0]);
                }
            });
        }
    }










    // Method to retrieve selected sales and their quantities
    public HashMap<String, Integer> getSelectedSales() {
        return selectedQuantities;
    }

    // Method to check for invalid quantities before submission
    public boolean hasInvalidQuantities() {
        for (String key : selectedQuantities.keySet()) {
            int position = getItemPosition(key);
            if (position != -1) {
                SalesModel item = getItem(position);
                int quantity = selectedQuantities.get(key);
                if ("Siomai".equalsIgnoreCase(item.getProduct()) && quantity > 0 && quantity < 4) {
                    return true; // Invalid quantity found for Siomai
                }
            }
        }
        return false; // All quantities are valid
    }

    // Helper method to get item position by key
    private int getItemPosition(String key) {
        for (int i = 0; i < getItemCount(); i++) {
            if (getRef(i).getKey().equals(key)) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sales_item, parent, false);
        return new myViewHolder(view);
    }

    // ViewHolder class
    class myViewHolder extends RecyclerView.ViewHolder {
        CircleImageView Image;
        TextView Product, Price, Quantity;
        Button minusBtn, plusBtn;
        EditText count;

        public myViewHolder(@NonNull View itemView) {
            super(itemView);
            Image = itemView.findViewById(R.id.imgS);
            Product = itemView.findViewById(R.id.sproduct);
            Price = itemView.findViewById(R.id.sPrice);
            Quantity = itemView.findViewById(R.id.stock);
            count = itemView.findViewById(R.id.counts);
            minusBtn = itemView.findViewById(R.id.minusBtn);
            plusBtn = itemView.findViewById(R.id.plusBtn);
        }
    }
}