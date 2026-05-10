package com.example.smartparking.admin;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartparking.R;
import com.example.smartparking.models.ParkingSlot;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminAddEditParkingActivity extends AppCompatActivity {

    private EditText etParkingName, etParkingAddress, etTotalRegions, etLat, etLng, etPrice;
    private EditText etTotalSlots;
    private Spinner spinnerCategory;
    private Button btnSaveParking, btnCancel, btnPickLocation;
    private TextView btnAddRegion;
    private LinearLayout layoutRegionsContainer;
    private TextView tvPageTitle;

    private DatabaseReference mDatabase;
    private String parkingId = null;

    private FrameLayout layoutUploadImage;
    private LinearLayout layoutImagePlaceholder;
    private ImageView ivSelectedImage, ivUploadSuccess;
    private ProgressBar pbImageUpload;

    private ActivityResultLauncher<Intent> mapPickerLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private String uploadedImageUrl = "";
    private boolean isUploadingImage = false;

    private String[] categories = {"الجامعة", "الحوية", "المنتزه"};
    private ArrayAdapter<String> spinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_edit_parking);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("ParkingLots");


        etParkingName = findViewById(R.id.etParkingName);
        etParkingAddress = findViewById(R.id.etParkingAddress);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        etTotalRegions = findViewById(R.id.etTotalRegions);
        etTotalSlots = findViewById(R.id.etTotalSlots);
        btnAddRegion = findViewById(R.id.btnAddRegion);
        layoutRegionsContainer = findViewById(R.id.layoutRegionsContainer);
        etLat = findViewById(R.id.etLat);
        etLng = findViewById(R.id.etLng);
        etPrice = findViewById(R.id.etPrice);
        btnSaveParking = findViewById(R.id.btnSaveParking);
        btnCancel = findViewById(R.id.btnCancel);
        btnPickLocation = findViewById(R.id.btnPickLocation);
        tvPageTitle = findViewById(R.id.tvPageTitle);
        ImageButton btnBack = findViewById(R.id.btnBack);

        layoutUploadImage = findViewById(R.id.layoutUploadImage);
        layoutImagePlaceholder = findViewById(R.id.layoutImagePlaceholder);
        ivSelectedImage = findViewById(R.id.ivSelectedImage);
        ivUploadSuccess = findViewById(R.id.ivUploadSuccess);
        pbImageUpload = findViewById(R.id.pbImageUpload);

        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);

        if (getIntent().hasExtra("parkingId")) {
            parkingId = getIntent().getStringExtra("parkingId");
            tvPageTitle.setText("تعديل بيانات الموقف");
            btnSaveParking.setText("تحديث الموقف");
            loadExistingParkingData();
        }


        btnAddRegion.setOnClickListener(v -> {
            String regionsStr = etTotalRegions.getText().toString().trim();
            if (regionsStr.isEmpty()) {
                Toast.makeText(this, "أدخل عدد المناطق أولاً", Toast.LENGTH_SHORT).show();
                return;
            }
            int maxCount = Integer.parseInt(regionsStr);
            int currentCount = layoutRegionsContainer.getChildCount();

            if (currentCount >= maxCount) {
                Toast.makeText(this, "لقد وصلت للحد الأقصى لعدد المناطق المحددة (" + maxCount + ")", Toast.LENGTH_SHORT).show();
                return;
            }

            char nextRegionChar = (char) ('A' + currentCount);
            addRegionView(String.valueOf(nextRegionChar), "", "");
        });

        mapPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String lat = result.getData().getStringExtra("lat");
                        String lng = result.getData().getStringExtra("lng");
                        etLat.setText(lat);
                        etLng.setText(lng);
                        Toast.makeText(this, "تم تحديد الموقع بنجاح", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            ivSelectedImage.setImageURI(selectedImageUri);
                            ivSelectedImage.setVisibility(View.VISIBLE);
                            layoutImagePlaceholder.setVisibility(View.GONE);
                            processAndEncodeImage(selectedImageUri);
                        }
                    }
                }
        );

        btnPickLocation.setOnClickListener(v -> {
            Intent intent = new Intent(AdminAddEditParkingActivity.this, MapPickerActivity.class);
            mapPickerLauncher.launch(intent);
        });

        layoutUploadImage.setOnClickListener(v -> {
            if (!isUploadingImage) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                imagePickerLauncher.launch(intent);
            }
        });

        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());
        btnSaveParking.setOnClickListener(v -> saveParkingData());
    }


    private void calculateOverallTotalSlots() {
        int overallTotal = 0;
        for (int i = 0; i < layoutRegionsContainer.getChildCount(); i++) {
            View regionView = layoutRegionsContainer.getChildAt(i);
            EditText etRegionSlots = regionView.findViewById(R.id.etRegionSlots);
            String slotsStr = etRegionSlots.getText().toString().trim();
            if (!slotsStr.isEmpty()) {
                overallTotal += Integer.parseInt(slotsStr);
            }
        }
        etTotalSlots.setText(String.valueOf(overallTotal));
    }


    private void addRegionView(String regionKey, String slots, String special) {
        View regionView = LayoutInflater.from(this).inflate(R.layout.item_admin_region_input, layoutRegionsContainer, false);
        TextView tvRegionName = regionView.findViewById(R.id.tvRegionName);
        tvRegionName.setText(regionKey);
        regionView.setTag(regionKey);

        EditText etRegionSlots = regionView.findViewById(R.id.etRegionSlots);
        EditText etRegionSpecial = regionView.findViewById(R.id.etRegionSpecial);

        if (!slots.isEmpty()) etRegionSlots.setText(slots);
        if (!special.isEmpty()) etRegionSpecial.setText(special);


        etRegionSlots.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                calculateOverallTotalSlots();
            }
        });

        layoutRegionsContainer.addView(regionView);

        calculateOverallTotalSlots();
    }

    private void loadExistingParkingData() {
        if (parkingId == null) return;

        mDatabase.child(parkingId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    etParkingName.setText(snapshot.child("title").getValue(String.class));
                    etParkingAddress.setText(snapshot.child("address").getValue(String.class));
                    etPrice.setText(snapshot.child("price").getValue(String.class));
                    etLat.setText(snapshot.child("latitude").getValue(String.class));
                    etLng.setText(snapshot.child("longitude").getValue(String.class));

                    String category = snapshot.child("category").getValue(String.class);
                    if (category != null) {
                        int spinnerPosition = spinnerAdapter.getPosition(category);
                        if (spinnerPosition >= 0) spinnerCategory.setSelection(spinnerPosition);
                    }


                    String availabilityStr = snapshot.child("availabilityText").getValue(String.class);
                    if (availabilityStr != null) etTotalSlots.setText(availabilityStr);

                    if (snapshot.hasChild("regions")) {
                        int regionCount = (int) snapshot.child("regions").getChildrenCount();
                        etTotalRegions.setText(String.valueOf(regionCount));
                        layoutRegionsContainer.removeAllViews();

                        for (DataSnapshot regionSnap : snapshot.child("regions").getChildren()) {
                            String regionKey = regionSnap.getKey();
                            long totalSlotsInRegion = regionSnap.child("slots").getChildrenCount();
                            long specialNeedsInRegion = 0;

                            for (DataSnapshot slotSnap : regionSnap.child("slots").getChildren()) {
                                Boolean isSpecial = slotSnap.child("specialNeeds").getValue(Boolean.class);
                                if (isSpecial != null && isSpecial) {
                                    specialNeedsInRegion++;
                                }
                            }

                            addRegionView(regionKey, String.valueOf(totalSlotsInRegion), String.valueOf(specialNeedsInRegion));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void processAndEncodeImage(Uri imageUri) {
        isUploadingImage = true;
        pbImageUpload.setVisibility(View.VISIBLE);
        ivUploadSuccess.setVisibility(View.GONE);
        btnSaveParking.setEnabled(false);
        btnSaveParking.setText("جاري معالجة الصورة...");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean success = false;
            String encodedString = "";

            try {
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(imageStream);

                int maxWidth = 800;
                int maxHeight = 800;
                float ratio = Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight());
                int width = Math.round(ratio * bitmap.getWidth());
                int height = Math.round(ratio * bitmap.getHeight());
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
                byte[] imageBytes = baos.toByteArray();

                encodedString = "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                success = true;

            } catch (Exception e) {
                e.printStackTrace();
            }

            final boolean isSuccess = success;
            final String finalEncodedString = encodedString;

            handler.post(() -> {
                isUploadingImage = false;
                pbImageUpload.setVisibility(View.GONE);
                btnSaveParking.setEnabled(true);
                btnSaveParking.setText(parkingId != null ? "تحديث الموقف" : "إضافة الموقف");

                if (isSuccess) {
                    uploadedImageUrl = finalEncodedString;
                    ivUploadSuccess.setVisibility(View.VISIBLE);
                    Toast.makeText(AdminAddEditParkingActivity.this, "تم تجهيز الصورة بنجاح!", Toast.LENGTH_SHORT).show();
                } else {
                    layoutImagePlaceholder.setVisibility(View.VISIBLE);
                    ivSelectedImage.setVisibility(View.GONE);
                    Toast.makeText(AdminAddEditParkingActivity.this, "فشل معالجة الصورة", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void saveParkingData() {
        if (isUploadingImage) {
            Toast.makeText(this, "يرجى الانتظار حتى تكتمل معالجة الصورة", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etParkingName.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String address = etParkingAddress.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String lat = etLat.getText().toString().trim();
        String lng = etLng.getText().toString().trim();

        if (name.isEmpty() || price.isEmpty()) {
            Toast.makeText(this, "اسم الموقف والسعر مطلوبان", Toast.LENGTH_SHORT).show();
            return;
        }

        if (layoutRegionsContainer.getChildCount() == 0) {
            Toast.makeText(this, "يرجى إنشاء المناطق وإدخال السعة المحددة لكل منطقة", Toast.LENGTH_SHORT).show();
            return;
        }

        int overallTotalSlots = 0;
        Map<String, Object> regionsData = new HashMap<>();

        for (int i = 0; i < layoutRegionsContainer.getChildCount(); i++) {
            View regionView = layoutRegionsContainer.getChildAt(i);
            String regionNameStr = (String) regionView.getTag();

            EditText etRegionSlots = regionView.findViewById(R.id.etRegionSlots);
            EditText etRegionSpecial = regionView.findViewById(R.id.etRegionSpecial);

            String rSlotsStr = etRegionSlots.getText().toString().trim();
            String rSpecialStr = etRegionSpecial.getText().toString().trim();

            if (rSlotsStr.isEmpty() || rSpecialStr.isEmpty()) {
                Toast.makeText(this, "يرجى ملء جميع حقول سعة المناطق", Toast.LENGTH_SHORT).show();
                return;
            }

            int rSlots = Integer.parseInt(rSlotsStr);
            int rSpecial = Integer.parseInt(rSpecialStr);
            overallTotalSlots += rSlots;

            Map<String, Object> slotsData = new HashMap<>();
            for (int j = 1; j <= rSlots; j++) {
                String slotName = regionNameStr + j;
                String status = "available";
                boolean isSpecialNeeds = (j <= rSpecial);
                slotsData.put(slotName, new ParkingSlot(slotName, status, isSpecialNeeds));
            }
            Map<String, Object> regionMap = new HashMap<>();
            regionMap.put("slots", slotsData);
            regionsData.put(regionNameStr, regionMap);
        }


        etTotalSlots.setText(String.valueOf(overallTotalSlots));

        String id = parkingId;
        if (id == null) {
            id = mDatabase.push().getKey();
        }

        if (id == null) {
            Toast.makeText(this, "حدث خطأ في إنشاء المعرف", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> parkingData = new HashMap<>();
        parkingData.put("title", name);
        parkingData.put("category", category);
        parkingData.put("address", address);
        parkingData.put("price", price);
        parkingData.put("availabilityText", String.valueOf(overallTotalSlots));

        if (!lat.isEmpty() && !lng.isEmpty()) {
            parkingData.put("latitude", lat);
            parkingData.put("longitude", lng);
        }

        if (!uploadedImageUrl.isEmpty()) {
            parkingData.put("imageUrl", uploadedImageUrl);
        }

        parkingData.put("regions", regionsData);

        mDatabase.child(id).updateChildren(parkingData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "تم حفظ الموقف بتقسيماته الجديدة بنجاح", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "حدث خطأ أثناء الحفظ", Toast.LENGTH_SHORT).show();
            }
        });
    }
}