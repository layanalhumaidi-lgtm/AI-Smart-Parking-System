package com.example.smartparking.admin;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.smartparking.LoginActivity;
import com.example.smartparking.R;
import com.google.firebase.auth.FirebaseAuth;

public class AdminDashboardActivity extends AppCompatActivity {

    private CardView cardManageUsers, cardManageParking, cardSystemStats;
    private Button btnAdminLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);


        cardManageUsers = findViewById(R.id.cardManageUsers);
        cardManageParking = findViewById(R.id.cardManageParking);
        cardSystemStats = findViewById(R.id.cardSystemStats);
        btnAdminLogout = findViewById(R.id.btnAdminLogout);


        cardManageUsers.setOnClickListener(v -> {

            startActivity(new Intent(AdminDashboardActivity.this, AdminManageUsersActivity.class));

        });

        cardManageParking.setOnClickListener(v -> {

            startActivity(new Intent(AdminDashboardActivity.this, AdminManageParkingActivity.class));
        });

        cardSystemStats.setOnClickListener(v -> {

            startActivity(new Intent(AdminDashboardActivity.this, AdminSystemStatsActivity.class));

        });


        btnAdminLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(AdminDashboardActivity.this, LoginActivity.class));
            finishAffinity();
        });
    }
}