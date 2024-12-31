package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.Model.InventoryModel;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainAdapter extends FirebaseRecyclerAdapter<InventoryModel, MainAdapter.myViewHolder> {

    public MainAdapter(@NonNull FirebaseRecyclerOptions<InventoryModel> options) {
        super(options);
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onBindViewHolder(@NonNull myViewHolder holder, @SuppressLint("RecyclerView") final int position, @NonNull InventoryModel model) {
        // Inventory Holder
        holder.Product.setText(model.getProduct());
        holder.Quantity.setText(String.format("Quantity: %d", model.getQuantity()));
        holder.Price.setText(String.format("Price: ₱%.2f", model.getPrice()));

        Glide.with(holder.Img.getContext())
                .load(model.getImage())
                .placeholder(com.firebase.ui.database.R.drawable.common_google_signin_btn_icon_dark)
                .circleCrop()
                .error(com.firebase.ui.database.R.drawable.common_google_signin_btn_icon_light)
                .into(holder.Img);

        // Check for low stock and update UI
        int lowStockThreshold = model.getProduct().equalsIgnoreCase("Siomai") ? 100 : 50;
        if (model.getQuantity() < lowStockThreshold) {
            holder.cardView.setBackgroundResource(R.drawable.border_red); // Set the red border
            showLowStockDialog(holder.itemView, model.getProduct());
        }
    }

    private void showLowStockDialog(View view, String productName) {
        new AlertDialog.Builder(view.getContext())
                .setTitle("Low Stock Warning")
                .setMessage("Low stock for product: " + productName)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.main_item, parent, false);
        return new myViewHolder(view);
    }

    class myViewHolder extends RecyclerView.ViewHolder {

        CircleImageView Img;
        TextView Product, Price, Quantity;
        CardView cardView;

        public myViewHolder(@NonNull View itemView) {
            super(itemView);
            // Inventory
            Img = itemView.findViewById(R.id.img1);
            Product = itemView.findViewById(R.id.product);
            Price = itemView.findViewById(R.id.Price);
            Quantity = itemView.findViewById(R.id.Quantity);
            cardView = itemView.findViewById(R.id.card_view); // Assuming card_view is the ID of your CardView in main_item.xml
        }
    }
}