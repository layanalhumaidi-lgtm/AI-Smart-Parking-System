package com.example.smartparking;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartparking.MainActivity;
import com.example.smartparking.admin.AdminLoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etPhone, etPassword, etFullName, etEmail;
    private TextView tvLoginTab, tvRegisterTab, tvMainTitle, tvSubTitle; //tvBottomInfo, tvBottomAction;
    private LinearLayout layoutSignUpFields;
    private RelativeLayout layoutLoginExtras;
    private Button btnSubmit;
    private CheckBox cbRememberMe;

    private boolean isLoginMode = true;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        boolean isRemembered = prefs.getBoolean("isRemembered", false);

        if (mAuth.getCurrentUser() != null) {
            if (isRemembered) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
                return;
            } else {
                mAuth.signOut();
            }
        }

        setContentView(R.layout.activity_login);

        initViews();

        tvLoginTab.setOnClickListener(v -> switchMode(true));
        tvRegisterTab.setOnClickListener(v -> switchMode(false));

        btnSubmit.setOnClickListener(v -> {
            if (isLoginMode) {
                handleLogin();
            } else {
                handleSignUp();
            }
        });
        TextView tvAdminLogin = findViewById(R.id.tvAdminLogin);
        tvAdminLogin.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, AdminLoginActivity.class));
        });
    }

    private void initViews() {
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);

        tvLoginTab = findViewById(R.id.tvLoginTab);
        tvRegisterTab = findViewById(R.id.tvRegisterTab);
        tvMainTitle = findViewById(R.id.tvMainTitle);
        tvSubTitle = findViewById(R.id.tvSubTitle);

        layoutSignUpFields = findViewById(R.id.layoutSignUpFields);
        layoutLoginExtras = findViewById(R.id.layoutLoginExtras);
        btnSubmit = findViewById(R.id.btnSubmit);
        cbRememberMe = findViewById(R.id.cbRememberMe);
    }

    private void switchMode(boolean loginMode) {
        this.isLoginMode = loginMode;
        if (isLoginMode) {
            tvMainTitle.setText("تسجيل الدخول");
            tvSubTitle.setText("مرحباً بك في نظام مواقف الطائف الذكي");
            btnSubmit.setText("تسجيل الدخول");
            layoutSignUpFields.setVisibility(View.GONE);
            layoutLoginExtras.setVisibility(View.VISIBLE);
            tvLoginTab.setBackgroundResource(R.drawable.tab_active_background);
            tvLoginTab.setTextColor(Color.WHITE);
            tvRegisterTab.setBackgroundResource(android.R.color.transparent);
            tvRegisterTab.setTextColor(Color.parseColor("#888888"));
        } else {
            tvMainTitle.setText("إنشاء حساب جديد");
            tvSubTitle.setText("انضم إلينا الآن لتجربة مواقف أذكى");
            btnSubmit.setText("إنشاء الحساب");
            layoutSignUpFields.setVisibility(View.VISIBLE);
            layoutLoginExtras.setVisibility(View.GONE);
            tvRegisterTab.setBackgroundResource(R.drawable.tab_active_background);
            tvRegisterTab.setTextColor(Color.WHITE);
            tvLoginTab.setBackgroundResource(android.R.color.transparent);
            tvLoginTab.setTextColor(Color.parseColor("#888888"));
        }
    }

    private void handleLogin() {
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "يرجى إدخال الجوال وكلمة المرور", Toast.LENGTH_SHORT).show();
            return;
        }

        Query query = mDatabase.child("Users").orderByChild("phone").equalTo(phone);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String email = "";
                    for (DataSnapshot userSnap : snapshot.getChildren()) {
                        email = userSnap.child("email").getValue(String.class);
                    }

                    mAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                                    prefs.edit().putBoolean("isRemembered", cbRememberMe.isChecked()).apply();

                                    Toast.makeText(LoginActivity.this, "تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(LoginActivity.this, "فشل تسجيل الدخول: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                } else {
                    Toast.makeText(LoginActivity.this, "رقم الجوال غير مسجل", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, "خطأ في الاتصال بقاعدة البيانات", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSignUp() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "يرجى ملء جميع البيانات", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToDatabase(user.getUid(), name, email, phone);
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "فشل إنشاء الحساب: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDatabase(String userId, String name, String email, String phone) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("fullName", name);
        userData.put("email", email);
        userData.put("phone", phone);
        userData.put("userId", userId);

        mDatabase.child("Users").child(userId).setValue(userData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                        prefs.edit().putBoolean("isRemembered", true).apply();

                        Toast.makeText(LoginActivity.this, "تم إنشاء الحساب وحفظ البيانات بنجاح!", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finishAffinity();
                    } else {
                        Toast.makeText(LoginActivity.this, "فشل حفظ البيانات: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}