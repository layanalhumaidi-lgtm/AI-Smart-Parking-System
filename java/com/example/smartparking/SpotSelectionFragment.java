package com.example.smartparking;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartparking.adapters.SlotAdapter;
import com.example.smartparking.models.ParkingSlot;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpotSelectionFragment extends Fragment {

    private String parkingLotId, parkingName, parkingPrice;
    private String selectedRegionKey = "A";
    private String currentSelectedSpotId = "";

    private RecyclerView rvSlots;
    private SlotAdapter adapter;
    private List<ParkingSlot> slotList = new ArrayList<>();
    private LinearLayout layoutRegions;
    private TextView tvSelectedSpotDisplay;

    private DatabaseReference mDatabase;

    public static SpotSelectionFragment newInstance(String id, String name, String price) {
        SpotSelectionFragment fragment = new SpotSelectionFragment();
        Bundle args = new Bundle();
        args.putString("id", id);
        args.putString("name", name);
        args.putString("price", price);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_spot_selection, container, false);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (getArguments() != null) {
            parkingLotId = getArguments().getString("id");
            parkingName = getArguments().getString("name");
            parkingPrice = getArguments().getString("price");

            if (parkingLotId == null || parkingLotId.isEmpty()) {
                parkingLotId = parkingName.replaceAll("\\s+", "");
            }
        }

        initViews(view);
        loadRegions();
        loadSlots(selectedRegionKey);

        return view;
    }

    private void initViews(View view) {
        rvSlots = view.findViewById(R.id.rvSlotsGrid);
        rvSlots.setLayoutManager(new GridLayoutManager(getContext(), 2));

        layoutRegions = view.findViewById(R.id.layoutRegionsTabs);
        tvSelectedSpotDisplay = view.findViewById(R.id.tvSelectedSpotId);

        TextView tvTitle = view.findViewById(R.id.tvParkingNameHeader);
        tvTitle.setText(parkingName);

        view.findViewById(R.id.btnContinue).setOnClickListener(v -> {
            if (currentSelectedSpotId.isEmpty()) {
                Toast.makeText(getContext(), "يرجى اختيار موقف أولاً", Toast.LENGTH_SHORT).show();
                return;
            }

            Fragment bookingFragment = com.example.smartparking.BookingFragment.newInstance(parkingLotId, parkingName, parkingPrice, currentSelectedSpotId);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, bookingFragment)
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.btnBackSelection).setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void loadRegions() {
        mDatabase.child("ParkingLots").child(parkingLotId).child("regions")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        layoutRegions.removeAllViews();

                        if (snapshot.exists()) {
                            List<String> regionKeys = new ArrayList<>();
                            for (DataSnapshot regionSnap : snapshot.getChildren()) {
                                regionKeys.add(regionSnap.getKey());
                            }
                            Collections.sort(regionKeys);

                            for (String key : regionKeys) {
                                addRegionTab(key, "المنطقة " + key);
                            }
                        } else {
                            addRegionTab("A", "المنطقة A");
                            addRegionTab("B", "المنطقة B");
                        }

                        addRegionTab("Special", "ذوي الاحتياجات الخاصة");
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void addRegionTab(String key, String displayName) {
        TextView tab = new TextView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, 120);
        params.setMargins(10, 0, 10, 0);
        tab.setLayoutParams(params);
        tab.setPadding(40, 0, 40, 0);
        tab.setText(displayName);
        tab.setGravity(android.view.Gravity.CENTER);
        tab.setTextSize(14);
        tab.setTag(key);

        updateTabStyle(tab, key.equals(selectedRegionKey));

        tab.setOnClickListener(v -> {
            selectedRegionKey = (String) v.getTag();
            updateAllTabsUI();
            loadSlots(selectedRegionKey);
        });
        layoutRegions.addView(tab);
    }

    private void updateAllTabsUI() {
        for (int i = 0; i < layoutRegions.getChildCount(); i++) {
            TextView t = (TextView) layoutRegions.getChildAt(i);
            updateTabStyle(t, t.getTag().equals(selectedRegionKey));
        }
    }

    private void updateTabStyle(TextView tab, boolean isActive) {
        if (isActive) {
            tab.setBackgroundResource(R.drawable.tag_active_bg);
            tab.setTextColor(Color.WHITE);
        } else {
            tab.setBackgroundResource(R.drawable.tag_inactive_bg);
            tab.setTextColor(Color.parseColor("#757575"));
        }
    }

    private void loadSlots(String regionKey) {
        if (regionKey.equals("Special")) {
            loadAllSpecialNeedsSlots();
        } else {
            mDatabase.child("ParkingLots").child(parkingLotId).child("regions").child(regionKey).child("slots")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!isAdded()) return;
                            slotList.clear();
                            if (snapshot.exists()) {
                                for (DataSnapshot slotSnap : snapshot.getChildren()) {
                                    ParkingSlot slot = slotSnap.getValue(ParkingSlot.class);
                                    slotList.add(slot);
                                }
                                sortSlots();
                            } else {
                                generateDummySlots(regionKey);
                            }
                            setupAdapter();
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }
    }

    private void loadAllSpecialNeedsSlots() {
        mDatabase.child("ParkingLots").child(parkingLotId).child("regions")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        slotList.clear();
                        if (snapshot.exists()) {
                            for (DataSnapshot regionSnap : snapshot.getChildren()) {
                                for (DataSnapshot slotSnap : regionSnap.child("slots").getChildren()) {
                                    ParkingSlot slot = slotSnap.getValue(ParkingSlot.class);
                                    if (slot != null && slot.isSpecialNeeds()) {
                                        slotList.add(slot);
                                    }
                                }
                            }
                            sortSlots();
                        }
                        setupAdapter();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }


    private void sortSlots() {
        Collections.sort(slotList, (s1, s2) -> {
            String id1 = s1.getSlotId();
            String id2 = s2.getSlotId();

            if (id1 == null || id2 == null) return 0;

            String prefix1 = id1.replaceAll("[0-9]", "");
            String prefix2 = id2.replaceAll("[0-9]", "");

            int res = prefix1.compareTo(prefix2);
            if (res != 0) return res;

            try {
                int n1 = Integer.parseInt(id1.replaceAll("[^0-9]", ""));
                int n2 = Integer.parseInt(id2.replaceAll("[^0-9]", ""));
                return Integer.compare(n1, n2);
            } catch (NumberFormatException e) {
                return id1.compareTo(id2);
            }
        });
    }

    private void setupAdapter() {
        adapter = new SlotAdapter(slotList, slot -> {
            currentSelectedSpotId = slot.getSlotId();
            tvSelectedSpotDisplay.setText(currentSelectedSpotId);
        });
        rvSlots.setAdapter(adapter);
    }

    private void generateDummySlots(String prefix) {
        slotList.clear();
        slotList.add(new ParkingSlot(prefix + "1", "available", true));
        slotList.add(new ParkingSlot(prefix + "2", "reserved", false));
        slotList.add(new ParkingSlot(prefix + "3", "available", false));
        slotList.add(new ParkingSlot(prefix + "4", "available", false));
        setupAdapter();
    }
}