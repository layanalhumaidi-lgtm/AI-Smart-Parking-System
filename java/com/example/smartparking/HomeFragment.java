package com.example.smartparking;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartparking.adapters.ParkingAdapter;
import com.example.smartparking.models.ParkingLot;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private RecyclerView rvParkingLots;
    private ParkingAdapter adapter;
    private List<ParkingLot> fullList = new ArrayList<>();
    private List<ParkingLot> filteredList = new ArrayList<>();

    private EditText etSearch;
    private TextView tabAll, tabUni, tabHaw, tabMon;

    private String currentCategory = "الكل";
    private String currentSearchQuery = "";

    private DatabaseReference mDatabase;
    private FusedLocationProviderClient fusedLocationClient;
    private Location userLocation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        etSearch = view.findViewById(R.id.etSearchParking);
        tabAll = view.findViewById(R.id.tabAll);
        tabUni = view.findViewById(R.id.tabUniversity);
        tabHaw = view.findViewById(R.id.tabHawiyah);
        tabMon = view.findViewById(R.id.tabAlMontazah);

        rvParkingLots = view.findViewById(R.id.rvParkingLots);
        rvParkingLots.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ParkingAdapter(filteredList, parkingLot -> {
            Fragment spotSelection = SpotSelectionFragment.newInstance(
                    parkingLot.getId(),
                    parkingLot.getTitle(),
                    parkingLot.getPrice()
            );
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, spotSelection)
                    .addToBackStack(null)
                    .commit();
        });
        rvParkingLots.setAdapter(adapter);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("ParkingLots");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        setupSearchAndTabs();
        checkLocationPermissionAndLoad();

        return view;
    }

    private void setupSearchAndTabs() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        View.OnClickListener tabClickListener = v -> {
            resetTabsUI();

            v.setBackgroundResource(R.drawable.tag_active_bg);
            ((TextView) v).setTextColor(Color.WHITE);

            if (v.getId() == R.id.tabAll) currentCategory = "الكل";
            else if (v.getId() == R.id.tabUniversity) currentCategory = "الجامعة";
            else if (v.getId() == R.id.tabHawiyah) currentCategory = "الحوية";
            else if (v.getId() == R.id.tabAlMontazah) currentCategory = "المنتزه";

            applyFilters();
        };

        tabAll.setOnClickListener(tabClickListener);
        tabUni.setOnClickListener(tabClickListener);
        tabHaw.setOnClickListener(tabClickListener);
        tabMon.setOnClickListener(tabClickListener);
    }

    private void resetTabsUI() {
        TextView[] tabs = {tabAll, tabUni, tabHaw, tabMon};
        for (TextView t : tabs) {
            t.setBackgroundResource(R.drawable.tag_inactive_bg);
            t.setTextColor(Color.parseColor("#888888"));
        }
    }

    private void applyFilters() {
        filteredList.clear();
        for (ParkingLot lot : fullList) {
            boolean matchesSearch = lot.getTitle().toLowerCase().contains(currentSearchQuery);
            boolean matchesCategory = currentCategory.equals("الكل") ||
                    (lot.getCategory() != null && lot.getCategory().equals(currentCategory));

            if (matchesSearch && matchesCategory) {
                filteredList.add(lot);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void checkLocationPermissionAndLoad() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        } else {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationTokenSource().getToken())
                    .addOnSuccessListener(location -> {
                        if (location != null) userLocation = location;
                        loadParkingLots();
                    })
                    .addOnFailureListener(e -> loadParkingLots());
        }
    }

    private void loadParkingLots() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                fullList.clear();
                int colorIndex = 0;

                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        String title = data.child("title").getValue(String.class);
                        String category = data.child("category").getValue(String.class);
                        String price = data.child("price").getValue(String.class);
                        String imageUrl = data.child("imageUrl").getValue(String.class);
                        String latStr = data.child("latitude").getValue(String.class);
                        String lngStr = data.child("longitude").getValue(String.class);

                        String distanceDisplay = calculateDistance(latStr, lngStr);

                        int available = 0, special = 0;
                        if (data.hasChild("regions")) {
                            for (DataSnapshot region : data.child("regions").getChildren()) {
                                for (DataSnapshot slot : region.child("slots").getChildren()) {
                                    if (!"reserved".equals(slot.child("status").getValue(String.class))) {
                                        available++;
                                        Boolean isSp = slot.child("specialNeeds").getValue(Boolean.class);
                                        if (isSp != null && isSp) special++;
                                    }
                                }
                            }
                        }

                        int gradient = (colorIndex % 2 == 0) ? R.drawable.parking_card_gradient_1 : R.drawable.parking_card_gradient_2;

                        if (title != null) {
                            ParkingLot lot = new ParkingLot(title, category != null ? category : "الجامعة",
                                    available, special, price != null ? price : "0",
                                    distanceDisplay, gradient, imageUrl);
                            lot.setId(data.getKey());
                            fullList.add(lot);
                            colorIndex++;
                        }
                    }
                }
                applyFilters();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String calculateDistance(String latStr, String lngStr) {
        if (userLocation != null && latStr != null && lngStr != null) {
            try {
                float[] res = new float[1];
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                        Double.parseDouble(latStr), Double.parseDouble(lngStr), res);
                if (res[0] < 1000) return String.format(Locale.getDefault(), "%.0f متر", res[0]);
                return String.format(Locale.getDefault(), "%.1f كم", res[0] / 1000);
            } catch (Exception e) { return "1.2 كم"; }
        }
        return "غير متوفر";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 101) loadParkingLots();
    }
}