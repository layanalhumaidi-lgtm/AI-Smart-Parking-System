package com.example.smartparking;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FindMyCarFragment extends Fragment {

    private TextView tvFragSlot, tvFragSection, tvFragPlate, tvFragParkingName;
    private TextView tvTipRegion, tvTipSlot;
    private CardView cardMapPlaceholder;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_find_my_car, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        tvFragSlot = view.findViewById(R.id.tvFragSlot);
        tvFragSection = view.findViewById(R.id.tvFragSection);
        tvFragPlate = view.findViewById(R.id.tvFragPlate);
        tvFragParkingName = view.findViewById(R.id.tvFragParkingName);

        tvTipRegion = view.findViewById(R.id.tvTipRegion);
        tvTipSlot = view.findViewById(R.id.tvTipSlot);

        cardMapPlaceholder = view.findViewById(R.id.cardMapPlaceholder);

        if (mAuth.getCurrentUser() != null) {
            loadUserCarLocation(mAuth.getCurrentUser().getUid());
        }

        if (cardMapPlaceholder != null) {
            cardMapPlaceholder.setOnClickListener(v -> openGoogleMaps());
        }


        return view;
    }

    private void loadUserCarLocation(String userId) {
        mDatabase.child("Reservations").child(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && isAdded()) {
                            long now = System.currentTimeMillis();
                            boolean foundActive = false;

                            for (DataSnapshot resSnap : snapshot.getChildren()) {
                                String status = resSnap.child("status").getValue(String.class);
                                Long startTimestamp = resSnap.child("startTimestamp").getValue(Long.class);
                                Long endTimestamp = resSnap.child("endTimestamp").getValue(Long.class);

                                boolean isActive = false;

                                if ("منتهي".equals(status)) {
                                    isActive = false;
                                } else if ("نشط".equals(status)) {
                                    isActive = true;
                                } else if (startTimestamp != null && endTimestamp != null) {
                                    if (now >= startTimestamp && now <= endTimestamp) {
                                        isActive = true;
                                    }
                                }

                                if (isActive) {
                                    foundActive = true;
                                    String slot = resSnap.child("spotId").getValue(String.class);
                                    String plate = resSnap.child("plateNumber").getValue(String.class);
                                    String building = resSnap.child("parkingName").getValue(String.class);

                                    String region = "غير محدد";
                                    if (slot != null && !slot.isEmpty()) {
                                        region = "المنطقة " + slot.substring(0, 1).toUpperCase();
                                    }

                                    if (tvFragSlot != null) tvFragSlot.setText(slot);
                                    if (tvFragSection != null) tvFragSection.setText(region);
                                    if (tvFragPlate != null) tvFragPlate.setText(plate != null ? plate : "غير متوفر");
                                    if (tvFragParkingName != null) tvFragParkingName.setText(building);

                                    if (tvTipRegion != null && region != null) {
                                        tvTipRegion.setText("تابع اللافتات الإرشادية لـ " + region);
                                    }
                                    if (tvTipSlot != null && slot != null) {
                                        tvTipSlot.setText("ابحث عن رقم الموقف " + slot);
                                    }

                                    break;
                                }
                            }

                            if (!foundActive) {
                                setNoActiveParkingData();
                            }

                        } else {
                            setNoActiveParkingData();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "فشل تحميل موقع السيارة", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void setNoActiveParkingData() {
        if (tvFragSlot != null) tvFragSlot.setText("--");
        if (tvFragSection != null) tvFragSection.setText("لا يوجد حجز نشط");
        if (tvFragPlate != null) tvFragPlate.setText("--");
        if (tvFragParkingName != null) tvFragParkingName.setText("لا يوجد موقف مسجل");

        if (tvTipRegion != null) tvTipRegion.setText("قم بحجز موقف لترى الإرشادات هنا");
        if (tvTipSlot != null) tvTipSlot.setText("--");
    }

    private void openGoogleMaps() {
        if (tvFragParkingName == null) return;

        String parkingName = tvFragParkingName.getText().toString();

        if (parkingName.isEmpty() || parkingName.equals("لا يوجد موقف مسجل") || parkingName.equals("موقف غير محدد")) {
            Toast.makeText(getContext(), "لا يوجد حجز نشط لفتح الخريطة", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("ParkingLots");

        dbRef.orderByChild("title").equalTo(parkingName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot parkingSnap : snapshot.getChildren()) {
                        if (parkingSnap.hasChild("latitude") && parkingSnap.hasChild("longitude")) {
                            String lat = parkingSnap.child("latitude").getValue(String.class);
                            String lng = parkingSnap.child("longitude").getValue(String.class);

                            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                            mapIntent.setPackage("com.google.android.apps.maps");

                            if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                                startActivity(mapIntent);
                            } else {
                                Uri browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng);
                                startActivity(new Intent(Intent.ACTION_VIEW, browserUri));
                            }
                            return;
                        }
                    }
                    Toast.makeText(getContext(), "إحداثيات الموقف غير متوفرة في النظام", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "لم يتم العثور على الموقف", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "حدث خطأ أثناء الاتصال بالخريطة", Toast.LENGTH_SHORT).show();
            }
        });
    }
}