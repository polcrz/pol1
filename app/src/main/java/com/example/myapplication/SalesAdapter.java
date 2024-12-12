package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.Model.SalesModel;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SalesAdapter extends FirebaseRecyclerAdapter<SalesModel, SalesAdapter.myViewHolder> {

    /**
     * Initialize a {@link RecyclerView.Adapter} that listens to a Firebase query. See
     * {@link FirebaseRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public SalesAdapter(@NonNull FirebaseRecyclerOptions<SalesModel> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull myViewHolder holder, int position, @NonNull SalesModel model) {

        // Set the product details in the UI
        holder.Product.setText(model.getProduct());
        holder.Quantity.setText(String.format("Stock: %d", model.getQuantity()));
        holder.Price.setText(String.format("Price: ₱%.2f", model.getPrice()));

        // Load the image using Glide
        Glide.with(holder.Image.getContext())
                .load(model.getImage())
                .placeholder(com.firebase.ui.database.R.drawable.common_google_signin_btn_icon_dark)
                .circleCrop()
                .error(com.firebase.ui.database.R.drawable.common_google_signin_btn_icon_light)
                .into(holder.Image);

        // Initialize counter value
        final int[] count = {0}; // Initialize counter for each item
        holder.count.setText(String.valueOf(count[0]));

        // Handle "plus" button click
        holder.plusBtn.setOnClickListener(v -> {
            if (count[0] < model.getQuantity()) { // Ensure counter does not exceed stock
                count[0]++;
                holder.count.setText(String.valueOf(count[0]));
            }
        });

        // Handle "minus" button click
        holder.minusBtn.setOnClickListener(v -> {
            if (count[0] > 0) { // Ensure counter does not go below zero
                count[0]--;
                holder.count.setText(String.valueOf(count[0]));
            }
        });




















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
        TextView Product, Price, Quantity, count;
        Button minusBtn, plusBtn, submitBtn;

        public myViewHolder(@NonNull View itemView) {
            super(itemView);

            Image = itemView.findViewById(R.id.imgS);
            Product = itemView.findViewById(R.id.sproduct);
            Price = itemView.findViewById(R.id.sPrice);
            Quantity = itemView.findViewById(R.id.stock);

            count = itemView.findViewById(R.id.counts);
            minusBtn = itemView.findViewById(R.id.minusBtn);
            plusBtn = itemView.findViewById(R.id.plusBtn);
            submitBtn = itemView.findViewById(R.id.submitBtn);
        }
    }
}
