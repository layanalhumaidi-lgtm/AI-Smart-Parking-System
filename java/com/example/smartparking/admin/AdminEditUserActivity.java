package com.example.smartparking.admin;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartparking.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AdminEditUserActivity extends AppCompatActivity {

    private EditText etEditEmail, etEditName, etEditPhone;
    private Button btnSaveChanges;

    private DatabaseReference mDatabase;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_user);

        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            Toast.makeText(this, "خطأ: لم يتم العثور على المستخدم", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etEditEmail = findViewById(R.id.etEditEmail);
        etEditName = findViewById(R.id.etEditName);
        etEditPhone = findViewById(R.id.etEditPhone);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        ImageButton btnBack = findViewById(R.id.btnBack);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(userId);

        loadUserData();

        btnBack.setOnClickListener(v -> finish());
        btnSaveChanges.setOnClickListener(v -> saveUserChanges());
    }

    private void loadUserData() {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);

                    etEditEmail.setText(email);
                    etEditName.setText(name);
                    etEditPhone.setText(phone);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminEditUserActivity.this, "فشل تحميل البيانات", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserChanges() {
        String newName = etEditName.getText().toString().trim();
        String newPhone = etEditPhone.getText().toString().trim();

        if (newName.isEmpty() || newPhone.isEmpty()) {
            Toast.makeText(this, "يرجى ملء الاسم ورقم الجوال", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", newName);
        updates.put("phone", newPhone);

        mDatabase.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "تم تحديث البيانات بنجاح", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "فشل حفظ التعديلات", Toast.LENGTH_SHORT).show();
            }
        });
    }
}