package com.example.smartparking;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BookingSuccessFragment extends Fragment {

    private String parkingName, spotId, duration, totalPrice, receiptId;

    public static BookingSuccessFragment newInstance(String parkingName, String spotId, String duration, String totalPrice, String receiptId) {
        BookingSuccessFragment fragment = new BookingSuccessFragment();
        Bundle args = new Bundle();
        args.putString("parkingName", parkingName);
        args.putString("spotId", spotId);
        args.putString("duration", duration);
        args.putString("totalPrice", totalPrice);
        args.putString("receiptId", receiptId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_booking_success, container, false);

        if (getArguments() != null) {
            parkingName = getArguments().getString("parkingName");
            spotId = getArguments().getString("spotId");
            duration = getArguments().getString("duration");
            totalPrice = getArguments().getString("totalPrice");
            receiptId = getArguments().getString("receiptId");
        }

        TextView tvReceiptId = view.findViewById(R.id.tvReceiptId);
        TextView tvSuccessParkingName = view.findViewById(R.id.tvSuccessParkingName);
        TextView tvSuccessSpot = view.findViewById(R.id.tvSuccessSpot);
        TextView tvSuccessDate = view.findViewById(R.id.tvSuccessDate);
        TextView tvSuccessTime = view.findViewById(R.id.tvSuccessTime);
        TextView tvSuccessDuration = view.findViewById(R.id.tvSuccessDuration);
        TextView tvSuccessTotal = view.findViewById(R.id.tvSuccessTotal);
        ImageView ivQRCode = view.findViewById(R.id.ivQRCode); // ربط صورة الـ QR

        Button btnViewDetails = view.findViewById(R.id.btnViewDetails);
        Button btnNavigateToSpot = view.findViewById(R.id.btnNavigateToSpot);

        tvReceiptId.setText("#" + receiptId);
        tvSuccessParkingName.setText(parkingName);
        tvSuccessSpot.setText(spotId);
        tvSuccessDuration.setText(duration);
        tvSuccessTotal.setText(totalPrice);


        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ar"));
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", new Locale("ar"));

        tvSuccessDate.setText(dateFormat.format(now));
        tvSuccessTime.setText(timeFormat.format(now));


        String qrData = "ReceiptID: " + receiptId + "\nSpot: " + spotId + "\nDuration: " + duration;
        generateQRCode(qrData, ivQRCode);

        btnViewDetails.setOnClickListener(v -> {

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ReservationsFragment())
                    .commit();
        });

        btnNavigateToSpot.setOnClickListener(v -> {

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
                        Toast.makeText(getContext(), "إحداثيات الموقف غير متوفرة في قاعدة البيانات", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "لم يتم العثور على الموقف", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "حدث خطأ أثناء جلب الإحداثيات", Toast.LENGTH_SHORT).show();
                }
            });
        });

        return view;
    }

    private void generateQRCode(String data, ImageView imageView) {
        if (imageView == null) return;

        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 400, 400);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            imageView.setImageBitmap(bmp);

        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "فشل في توليد رمز الدخول", Toast.LENGTH_SHORT).show();
        }
    }
}