package com.example.smartparking.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartparking.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView tvCoordinates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        ImageButton btnBackMap = findViewById(R.id.btnBackMap);
        Button btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        tvCoordinates = findViewById(R.id.tvCoordinates);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnBackMap.setOnClickListener(v -> finish());

        btnConfirmLocation.setOnClickListener(v -> {
            if (mMap != null) {

                LatLng center = mMap.getCameraPosition().target;

                Intent resultIntent = new Intent();
                resultIntent.putExtra("lat", String.valueOf(center.latitude));
                resultIntent.putExtra("lng", String.valueOf(center.longitude));
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;


        LatLng taifLocation = new LatLng(21.2622, 40.4167);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(taifLocation, 14f));

        mMap.setOnCameraMoveListener(() -> {
            LatLng center = mMap.getCameraPosition().target;
            tvCoordinates.setText(String.format("Lat: %.4f, Lng: %.4f", center.latitude, center.longitude));
        });
    }
}