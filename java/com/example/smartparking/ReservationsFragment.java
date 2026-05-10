package com.example.smartparking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartparking.adapters.ReservationAdapter;
import com.example.smartparking.models.Reservation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReservationsFragment extends Fragment {

    private RecyclerView rvReservations;
    private ReservationAdapter adapter;
    private List<Reservation> reservationList;
    private LinearLayout layoutEmpty;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reservations, container, false);

        rvReservations = view.findViewById(R.id.rvReservations);
        layoutEmpty = view.findViewById(R.id.layoutEmptyReservations);

        rvReservations.setLayoutManager(new LinearLayoutManager(getContext()));
        reservationList = new ArrayList<>();

        adapter = new ReservationAdapter(reservationList, reservation -> {
            if ("نشط".equals(reservation.getStatus())) {
                Fragment activeFragment = ActiveReservationFragment.newInstance(
                        reservation.getId(),
                        reservation.getParkingName(),
                        reservation.getSpotId(),
                        reservation.getStartTime(),
                        reservation.getTotalPrice(),
                        reservation.getEndTimestamp()
                );
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, activeFragment)
                        .addToBackStack(null)
                        .commit();
            } else if ("محجوز".equals(reservation.getStatus())) {
                Toast.makeText(getContext(), "الحجز لم يبدأ بعد", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "هذا الحجز منتهي", Toast.LENGTH_SHORT).show();
            }
        });

        rvReservations.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference()
                    .child("Reservations").child(uid);
            loadReservations();
        }

        return view;
    }

    private void loadReservations() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reservationList.clear();
                long now = System.currentTimeMillis();

                if (snapshot.exists()) {
                    layoutEmpty.setVisibility(View.GONE);
                    for (DataSnapshot data : snapshot.getChildren()) {
                        Reservation res = data.getValue(Reservation.class);
                        if (res != null) {
                            res.setId(data.getKey());
                            String currentStatus = res.getStatus();

                            if (!"منتهي".equals(currentStatus) && res.getStartTimestamp() > 0 && res.getEndTimestamp() > 0) {
                                String newStatus = currentStatus;

                                if (now > res.getEndTimestamp()) {
                                    newStatus = "منتهي";
                                } else if (now >= res.getStartTimestamp() && now <= res.getEndTimestamp()) {
                                    newStatus = "نشط";
                                } else {
                                    newStatus = "محجوز";
                                }

                                if (!newStatus.equals(currentStatus)) {
                                    data.getRef().child("status").setValue(newStatus);
                                    res.setStatus(newStatus);

                                    if ("منتهي".equals(newStatus)) {
                                        freeUpParkingSlot(res.getParkingName(), res.getSpotId());
                                    }
                                }
                            }
                            reservationList.add(res);
                        }
                    }
                    Collections.reverse(reservationList);
                } else {
                    layoutEmpty.setVisibility(View.VISIBLE);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "خطأ في الاتصال", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void freeUpParkingSlot(String parkingName, String spotId) {
        if (parkingName == null || spotId == null || spotId.isEmpty()) return;

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("ParkingLots");
        dbRef.orderByChild("title").equalTo(parkingName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot pSnap : snapshot.getChildren()) {
                        String realParkingId = pSnap.getKey();
                        String rStr = spotId.substring(0, 1).toUpperCase();

                        dbRef.child(realParkingId).child("regions").child(rStr).child("slots").child(spotId).child("status").setValue("available");
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}