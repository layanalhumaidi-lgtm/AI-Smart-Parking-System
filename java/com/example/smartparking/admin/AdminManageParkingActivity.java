package com.example.smartparking.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartparking.R;
import com.example.smartparking.adapters.AdminParkingAdapter;
import com.example.smartparking.models.ParkingLot;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminManageParkingActivity extends AppCompatActivity {

    private RecyclerView rvAdminParkingLots;
    private AdminParkingAdapter adapter;
    private List<ParkingLot> parkingList;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_parking);

        rvAdminParkingLots = findViewById(R.id.rvAdminParkingLots);
        FloatingActionButton fabAddParking = findViewById(R.id.fabAddParking);
        ImageButton btnBack = findViewById(R.id.btnBack);

        rvAdminParkingLots.setLayoutManager(new LinearLayoutManager(this));
        parkingList = new ArrayList<>();

        adapter = new AdminParkingAdapter(parkingList, new AdminParkingAdapter.OnParkingClickListener() {
            @Override
            public void onEditClick(ParkingLot parking) {
                Intent intent = new Intent(AdminManageParkingActivity.this, AdminAddEditParkingActivity.class);
                intent.putExtra("parkingId", parking.getId());
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(ParkingLot parking) {
                new AlertDialog.Builder(AdminManageParkingActivity.this)
                        .setTitle("حذف الموقف")
                        .setMessage("هل أنت متأكد أنك تريد حذف هذا الموقف بالكامل؟")
                        .setPositiveButton("نعم", (dialog, which) -> {
                            mDatabase.child(parking.getId()).removeValue();
                            Toast.makeText(AdminManageParkingActivity.this, "تم الحذف", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("إلغاء", null)
                        .show();
            }
        });

        rvAdminParkingLots.setAdapter(adapter);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("ParkingLots");

        fabAddParking.setOnClickListener(v -> {
            startActivity(new Intent(AdminManageParkingActivity.this, AdminAddEditParkingActivity.class));
        });

        btnBack.setOnClickListener(v -> finish());

        loadParkingLots();
    }

    private void loadParkingLots() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                parkingList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String title = data.child("title").getValue(String.class);
                    String category = data.child("category").getValue(String.class); // قراءة التصنيف
                    String price = data.child("price").getValue(String.class);
                    String distance = data.child("distance").getValue(String.class);
                    String availabilityStr = data.child("availabilityText").getValue(String.class);
                    String imageUrl = data.child("imageUrl").getValue(String.class);

                    if (title != null) {
                        int availableCount = 0;
                        try {
                            if (availabilityStr != null) availableCount = Integer.parseInt(availabilityStr);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }

                        if (category == null) category = "الجامعة";


                        ParkingLot lot = new ParkingLot(
                                title,
                                category,
                                availableCount,
                                0,
                                price != null ? price : "0",
                                distance != null ? distance : "0",
                                R.drawable.parking_card_gradient_1,
                                imageUrl);

                        lot.setId(data.getKey());
                        parkingList.add(lot);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminManageParkingActivity.this, "فشل جلب المواقف", Toast.LENGTH_SHORT).show();
            }
        });
    }
}