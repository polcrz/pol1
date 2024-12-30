package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
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
import com.example.myapplication.Model.InventoryModel;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.ViewHolder;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainAdapter extends FirebaseRecyclerAdapter<InventoryModel, MainAdapter.myViewHolder> {

    public MainAdapter(@NonNull FirebaseRecyclerOptions<InventoryModel> options) {
        super(options);
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onBindViewHolder(@NonNull myViewHolder holder, @SuppressLint("RecyclerView") final int position, @NonNull InventoryModel model) {
        //Inventory Holder
        holder.Product.setText(model.getProduct());
        holder.Quantity.setText(String.format("Quantity: %d", model.getQuantity()));
        holder.Price.setText(String.format("Price: ₱%.2f", model.getPrice()));

        Glide.with(holder.Img.getContext())
                .load(model.getImage())
                .placeholder(com.firebase.ui.database.R.drawable.common_google_signin_btn_icon_dark)
                .circleCrop()
                .error(com.firebase.ui.database.R.drawable.common_google_signin_btn_icon_light)
                .into(holder.Img);


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



        public myViewHolder(@NonNull View itemView) {
            super(itemView);
            // Inventory
            Img = itemView.findViewById(R.id.img1);
            Product = itemView.findViewById(R.id.product);
            Price = itemView.findViewById(R.id.Price);
            Quantity = itemView.findViewById(R.id.Quantity);




        }
    }
}
