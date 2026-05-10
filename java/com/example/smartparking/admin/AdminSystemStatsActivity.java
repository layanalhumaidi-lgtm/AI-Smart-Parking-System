package com.example.smartparking.admin;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.smartparking.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.Locale;

public class AdminSystemStatsActivity extends AppCompatActivity {

    private TextView tvActiveSessions, tvOccupancyRate, tvTotalRevenue;
    private LinearLayout layoutParkingStatusContainer;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_system_stats);

        tvActiveSessions = findViewById(R.id.tvActiveSessions);
        tvOccupancyRate = findViewById(R.id.tvOccupancyRate);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        layoutParkingStatusContainer = findViewById(R.id.layoutParkingStatusContainer);

        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView btnRefreshData = findViewById(R.id.btnRefreshData);

        btnBack.setOnClickListener(v -> finish());
        btnRefreshData.setOnClickListener(v -> {
            Toast.makeText(this, "جاري تحديث البيانات...", Toast.LENGTH_SHORT).show();
            loadAllData();
        });

        mDatabase = FirebaseDatabase.getInstance().getReference();

        loadAllData();
    }

    private void loadAllData() {
        calculateActiveSessions();
        calculateOverallOccupancyAndLoadStatus();
        calculateTotalRevenue();
    }

    private void calculateActiveSessions() {
        mDatabase.child("Reservations").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int activeCount = 0;
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    for (DataSnapshot resSnap : userSnap.getChildren()) {
                        String status = resSnap.child("status").getValue(String.class);
                        if ("نشط".equals(status)) {
                            activeCount++;
                        }
                    }
                }
                tvActiveSessions.setText(String.valueOf(activeCount));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    private void calculateOverallOccupancyAndLoadStatus() {
        mDatabase.child("ParkingLots").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                layoutParkingStatusContainer.removeAllViews();

                int overallTotalCapacity = 0;
                int overallTotalReserved = 0;

                for (DataSnapshot lotSnap : snapshot.getChildren()) {
                    String title = lotSnap.child("title").getValue(String.class);
                    if (title == null) continue;

                    int lotCapacity = 0;
                    int lotReserved = 0;

                    if (lotSnap.hasChild("regions")) {
                        for (DataSnapshot regionSnap : lotSnap.child("regions").getChildren()) {
                            for (DataSnapshot slotSnap : regionSnap.child("slots").getChildren()) {
                                lotCapacity++;
                                String status = slotSnap.child("status").getValue(String.class);
                                if ("reserved".equals(status)) {
                                    lotReserved++;
                                }
                            }
                        }
                    } else {

                        String capStr = lotSnap.child("availabilityText").getValue(String.class);
                        if (capStr != null) lotCapacity = Integer.parseInt(capStr);
                    }

                    int lotAvailable = lotCapacity - lotReserved;
                    if (lotAvailable < 0) lotAvailable = 0;

                    overallTotalCapacity += lotCapacity;
                    overallTotalReserved += lotReserved;


                    addParkingStatusCard(title, lotCapacity, lotAvailable, lotReserved);
                }


                int occupancyRate = 0;
                if (overallTotalCapacity > 0) {
                    occupancyRate = (int) (((float) overallTotalReserved / overallTotalCapacity) * 100);
                }
                tvOccupancyRate.setText(occupancyRate + "%");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    private void addParkingStatusCard(String name, int capacity, int available, int reserved) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_admin_parking_status, layoutParkingStatusContainer, false);

        TextView tvName = cardView.findViewById(R.id.tvStatusParkingName);
        TextView tvCapacity = cardView.findViewById(R.id.tvStatusCapacity);
        TextView tvAvailable = cardView.findViewById(R.id.tvStatusAvailable);
        TextView tvReserved = cardView.findViewById(R.id.tvStatusReserved);
        ProgressBar pbOccupancy = cardView.findViewById(R.id.pbStatusOccupancy);
        CardView cvIndicator = cardView.findViewById(R.id.cvStatusIndicator);

        tvName.setText(name);
        tvCapacity.setText(String.valueOf(capacity));
        tvAvailable.setText(String.valueOf(available));
        tvReserved.setText(String.valueOf(reserved));


        pbOccupancy.setMax(capacity > 0 ? capacity : 1);
        pbOccupancy.setProgress(reserved);

        int color;
        if (available == 0) {
            color = Color.parseColor("#D97A9A"); // ممتلئ (أحمر/وردي)
            tvAvailable.setTextColor(color);
        } else if ((float) available / capacity <= 0.2f) {
            color = Color.parseColor("#C5A36F"); // شبه ممتلئ (ذهبي)
            tvAvailable.setTextColor(color);
        } else {
            color = Color.parseColor("#8AB682"); // متاح بوفرة (أخضر)
            tvAvailable.setTextColor(color);
        }

        cvIndicator.setCardBackgroundColor(color);
        pbOccupancy.setProgressTintList(ColorStateList.valueOf(color));

        layoutParkingStatusContainer.addView(cardView);
    }

    private void calculateTotalRevenue() {
        mDatabase.child("payments").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalRevenue = 0;

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    for (DataSnapshot paymentSnap : userSnap.getChildren()) {
                        String amountStr = paymentSnap.child("amount").getValue(String.class);
                        if (amountStr != null) {

                            String numericAmount = amountStr.replaceAll("[^0-9]", "");
                            if (!numericAmount.isEmpty()) {
                                totalRevenue += Integer.parseInt(numericAmount);
                            }
                        }
                    }
                }


                String formattedRevenue = NumberFormat.getNumberInstance(Locale.US).format(totalRevenue);
                tvTotalRevenue.setText(formattedRevenue);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminSystemStatsActivity.this, "فشل جلب الأرباح", Toast.LENGTH_SHORT).show();
            }
        });
    }
}