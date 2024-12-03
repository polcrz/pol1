package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class AddActivity extends AppCompatActivity {



    EditText Product, Image;
    Button Save, Back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add);

        Product= (EditText) findViewById(R.id.productName);
        Image= (EditText) findViewById(R.id.imgUrl);


        Save= (Button) findViewById(R.id.btnSave);
        Back= (Button) findViewById(R.id.btnBack);

        Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertData();
                clearAll();
            }
        });

        Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AddActivity.this, InventoryActivity.class));

            }

        });
        }
        private void insertData(){

        Map<String, Object> map = new HashMap<>();
        map.put("Product", Product.getText().toString());
        map.put("Image", Image.getText().toString());


            FirebaseDatabase.getInstance().getReference().child("Products").push()
                    .setValue(map)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(AddActivity.this, "Data Save Successfully.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(AddActivity.this, "Error while Saving Data. ", Toast.LENGTH_SHORT).show();
                        }
                    });





        }

        private void clearAll(){

        Product.setText("");
        /*Price.setText("");
        Quantity.setText("");*/
        Image.setText("");

        }

    }
