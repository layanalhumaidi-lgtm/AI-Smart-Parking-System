package com.example.smartparking;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class ActiveReservationFragment extends Fragment {

    private String resId, parkingName, spotId, startTime, totalPrice;
    private long endTimestamp;

    private TextView tvTimerHours, tvTimerMins, tvTimerSecs;
    private CountDownTimer countDownTimer;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    public static ActiveReservationFragment newInstance(String resId, String parkingName, String spotId, String startTime, String totalPrice, long endTimestamp) {
        ActiveReservationFragment fragment = new ActiveReservationFragment();
        Bundle args = new Bundle();
        args.putString("resId", resId);
        args.putString("parkingName", parkingName);
        args.putString("spotId", spotId);
        args.putString("startTime", startTime);
        args.putString("totalPrice", totalPrice);
        args.putLong("endTimestamp", endTimestamp);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_active_reservation, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (getArguments() != null) {
            resId = getArguments().getString("resId");
            parkingName = getArguments().getString("parkingName");
            spotId = getArguments().getString("spotId");
            startTime = getArguments().getString("startTime");
            totalPrice = getArguments().getString("totalPrice");
            endTimestamp = getArguments().getLong("endTimestamp", 0);
        }

        // ربط العناصر بالواجهة
        TextView tvActiveSpot = view.findViewById(R.id.tvActiveSpot);
        TextView tvActiveParkingName = view.findViewById(R.id.tvActiveParkingName);
        TextView tvActiveRegion = view.findViewById(R.id.tvActiveRegion);
        TextView tvActiveStartTime = view.findViewById(R.id.tvActiveStartTime);
        TextView tvActivePrice = view.findViewById(R.id.tvActivePrice);

        tvTimerHours = view.findViewById(R.id.tvTimerHours);
        tvTimerMins = view.findViewById(R.id.tvTimerMins);
        tvTimerSecs = view.findViewById(R.id.tvTimerSecs);

        tvActiveSpot.setText(spotId != null ? spotId : "--");
        tvActiveParkingName.setText(parkingName);
        tvActiveStartTime.setText(startTime);
        tvActivePrice.setText(totalPrice);

        if (spotId != null && !spotId.isEmpty()) {
            tvActiveRegion.setText("المنطقة " + spotId.substring(0, 1).toUpperCase());
        }

        startTimer();

        view.findViewById(R.id.btnBackActive).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        view.findViewById(R.id.btnNavigateMap).setOnClickListener(v -> openGoogleMaps());

        view.findViewById(R.id.btnEndSession).setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("إنهاء الجلسة")
                    .setMessage("هل أنت متأكد من إنهاء جلسة الوقوف الآن؟ لا يمكن التراجع عن هذا الإجراء.")
                    .setPositiveButton("نعم، إنهاء", (dialog, which) -> endSession())
                    .setNegativeButton("إلغاء", null)
                    .show();
        });

        return view;
    }

    private void startTimer() {
        long timeLeftInMillis = endTimestamp - System.currentTimeMillis();

        if (timeLeftInMillis <= 0) {
            updateTimerText(0, 0, 0);
            return;
        }

        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int hours = (int) (millisUntilFinished / (1000 * 60 * 60));
                int minutes = (int) ((millisUntilFinished / (1000 * 60)) % 60);
                int seconds = (int) ((millisUntilFinished / 1000) % 60);
                updateTimerText(hours, minutes, seconds);
            }

            @Override
            public void onFinish() {
                updateTimerText(0, 0, 0);
            }
        }.start();
    }

    private void updateTimerText(int hours, int minutes, int seconds) {
        if (tvTimerHours != null) tvTimerHours.setText(String.format(Locale.getDefault(), "%02d", hours));
        if (tvTimerMins != null) tvTimerMins.setText(String.format(Locale.getDefault(), "%02d", minutes));
        if (tvTimerSecs != null) tvTimerSecs.setText(String.format(Locale.getDefault(), "%02d", seconds));
    }

    private void endSession() {
        if (mAuth.getCurrentUser() != null && resId != null) {
            String userId = mAuth.getCurrentUser().getUid();


            mDatabase.child("Reservations").child(userId).child(resId).child("status").setValue("منتهي")
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {

                            freeUpParkingSlot(parkingName, spotId);

                            Toast.makeText(getContext(), "تم إنهاء الجلسة بنجاح", Toast.LENGTH_SHORT).show();
                            getParentFragmentManager().popBackStack();
                        } else {
                            Toast.makeText(getContext(), "فشل إنهاء الجلسة", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
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

    private void openGoogleMaps() {
        if (parkingName == null || parkingName.isEmpty()) return;

        mDatabase.child("ParkingLots").orderByChild("title").equalTo(parkingName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
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
                        }
                        Toast.makeText(getContext(), "إحداثيات الموقف غير متوفرة", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}