package com.example.smartparking.admin;


import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartparking.R;
import com.example.smartparking.adapters.AdminUserAdapter;
import com.example.smartparking.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;
import java.util.List;

public class AdminManageUsersActivity extends AppCompatActivity {

    private TextView tvTotalUsers;
    private RecyclerView rvUsers;
    private AdminUserAdapter adapter;
    private List<User> userList;

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_users);

        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        rvUsers = findViewById(R.id.rvUsers);
        ImageButton btnBack = findViewById(R.id.btnBack);

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();

        adapter = new AdminUserAdapter(userList, new AdminUserAdapter.OnUserClickListener() {
            @Override
            public void onEditClick(User user) {

                Intent intent = new Intent(AdminManageUsersActivity.this, AdminEditUserActivity.class);
                intent.putExtra("userId", user.getUserId());
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(User user) {
                new AlertDialog.Builder(AdminManageUsersActivity.this)
                        .setTitle("تأكيد الحذف")
                        .setMessage("هل أنت متأكد أنك تريد حذف المستخدم: " + user.getFullName() + "؟")
                        .setPositiveButton("نعم", (dialog, which) -> deleteUser(user.getUserId()))
                        .setNegativeButton("إلغاء", null)
                        .show();
            }
        });
        rvUsers.setAdapter(adapter);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsers();
    }

    private void loadUsers() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                int count = 0;
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    User user = userSnap.getValue(User.class);
                    if (user != null) {
                        userList.add(user);
                        count++;
                    }
                }
                tvTotalUsers.setText(String.valueOf(count));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminManageUsersActivity.this, "فشل جلب البيانات", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteUser(String userId) {
        mDatabase.child(userId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "تم حذف المستخدم بنجاح", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "فشل عملية الحذف", Toast.LENGTH_SHORT).show();
            }
        });
    }
}