package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.Model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<User> userList;
    private Context context;
    private DatabaseReference usersReference;
    private String currentUserRole; // Add a field to hold the role of the current user
    private boolean isSuperAdmin; // Add a field to hold the superadmin status

    public UserAdapter(List<User> userList, Context context, String currentUserRole) {
        this.userList = userList;
        this.context = context;
        this.usersReference = FirebaseDatabase.getInstance().getReference("users");
        this.currentUserRole = currentUserRole; // Initialize current user role
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.textViewName.setText(user.getName());
        holder.textViewEmail.setText(user.getEmail());
        holder.textViewRole.setText(user.getRole());

        // Load image using Glide
        Glide.with(context)
                .load(user.getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(holder.imageViewUser);

        // Show or hide the Delete button based on the role
        if (isSuperAdmin) {
            holder.buttonDelete.setVisibility(View.VISIBLE); // Show button for superAdmin
        } else {
            holder.buttonDelete.setVisibility(View.GONE); // Hide button for non-superAdmins
        }

        // Handle delete button click
        holder.buttonDelete.setOnClickListener(v -> {
            if (isSuperAdmin) { // Check if current user is a superAdmin
                showDeleteConfirmationDialog(user.getEmail());
            } else {
                Toast.makeText(context, "You do not have permission to delete an account.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void setSuperAdmin(boolean isSuperAdmin) {
        this.isSuperAdmin = isSuperAdmin;
        notifyDataSetChanged(); // Notify the adapter to refresh the list
    }

    private void deleteUser(String email) {
        usersReference.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            userSnapshot.getRef().removeValue();
                        }
                        Toast.makeText(context, "User deleted", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context, "Failed to delete user", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Show confirmation dialog before deleting
    private void showDeleteConfirmationDialog(final String email) {
        new AlertDialog.Builder(context)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete this account?")
                .setPositiveButton("Confirm", (dialog, which) -> deleteUser(email)) // If confirmed, delete the user
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()) // If cancelled, do nothing
                .create()
                .show();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewEmail, textViewRole;
        CircleImageView imageViewUser;
        Button buttonDelete;

        public UserViewHolder(View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
            textViewRole = itemView.findViewById(R.id.textViewRole);
            imageViewUser = itemView.findViewById(R.id.imageViewUser);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}