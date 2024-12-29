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

        holder.editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final DialogPlus dialogPlus = DialogPlus.newDialog(holder.Img.getContext())
                        .setContentHolder(new ViewHolder(R.layout.update_popup))
                        .setExpanded(true, 800)
                        .create();

                View view = dialogPlus.getHolderView();

                EditText Product = view.findViewById(R.id.productName);
                EditText Image = view.findViewById(R.id.imgUrl);
                EditText Price = view.findViewById(R.id.price);
                EditText Quantity = view.findViewById(R.id.quantity);




                Button btnUpdate = view.findViewById(R.id.btnUpdate);

                Product.setText(model.getProduct());
                Image.setText(model.getImage());
                Price.setText(String.format("%.2f", model.getPrice()));
                Quantity.setText(String.valueOf(model.getQuantity()));



                dialogPlus.show();

                btnUpdate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Retrieve input values
                        String productName = Product.getText().toString().trim();
                        String imageUrl = Image.getText().toString().trim();
                        String priceInput = Price.getText().toString().trim();
                        String quantityInput = Quantity.getText().toString().trim();

                        // Validate fields
                        if (productName.isEmpty() || imageUrl.isEmpty() || priceInput.isEmpty() || quantityInput.isEmpty()) {
                            Toast.makeText(holder.Product.getContext(), "All fields are required.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        boolean isPriceValid = true;
                        boolean isQuantityValid = true;

                        double price = 0;
                        int quantity = 0;

                        try {
                            price = Double.parseDouble(priceInput); // Validate and parse price
                        } catch (NumberFormatException e) {
                            isPriceValid = false;
                        }

                        try {
                            quantity = Integer.parseInt(quantityInput); // Validate and parse quantity
                        } catch (NumberFormatException e) {
                            isQuantityValid = false;
                        }

                        // Show appropriate error messages for invalid inputs
                        if (!isPriceValid && !isQuantityValid) {
                            Toast.makeText(holder.Product.getContext(), "Invalid price and quantity. Please enter numeric values.", Toast.LENGTH_SHORT).show();
                            return;
                        } else if (!isPriceValid) {
                            Toast.makeText(holder.Product.getContext(), "Invalid price. Please enter a numeric value.", Toast.LENGTH_SHORT).show();
                            return;
                        } else if (!isQuantityValid) {
                            Toast.makeText(holder.Product.getContext(), "Invalid quantity. Please enter a whole number.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Create a map to store updated values
                        Map<String, Object> map = new HashMap<>();
                        map.put("Product", productName);
                        map.put("Image", imageUrl);
                        map.put("Price", price);
                        map.put("Quantity", quantity);

                        String userUID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                        // Update Firebase database
                        FirebaseDatabase.getInstance().getReference().child("users").child(userUID).child("products")
                                .child(getRef(position).getKey()).updateChildren(map)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Toast.makeText(holder.Product.getContext(), "Data Updated Successfully.", Toast.LENGTH_SHORT).show();
                                        dialogPlus.dismiss();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(holder.Product.getContext(), "You don't have permission to edit this data", Toast.LENGTH_SHORT).show();
                                        dialogPlus.dismiss();
                                    }
                                });
                    }
                });


            }
        });


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
        Button editBtn;





        public myViewHolder(@NonNull View itemView) {
            super(itemView);
            // Inventory
            Img = itemView.findViewById(R.id.img1);
            Product = itemView.findViewById(R.id.product);
            Price = itemView.findViewById(R.id.Price);
            Quantity = itemView.findViewById(R.id.Quantity);

            editBtn = (Button) itemView.findViewById(R.id.edit_btn);



        }
    }
}
