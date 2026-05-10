package com.example.smartparking;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Calendar;
import java.util.Locale;

public class BookingFragment extends Fragment {

    private TextView tvParkingName, tvPrice, tvDate, tvStartTime, tvEndTime, tvTotal;
    private EditText etPlate;
    private Button btnConfirm;
    private View btnBack;

    private String parkingLotId, parkingLotName, parkingLotPrice, selectedSpotId;
    private Calendar calendar = Calendar.getInstance();

    private int selectedYear, selectedMonth, selectedDay;
    private int startHour = 8, startMinute = 0;
    private int endHour = 10, endMinute = 0;
    private int calculatedHours = 2;

    public static BookingFragment newInstance(String id, String name, String price, String spotId) {
        BookingFragment fragment = new BookingFragment();
        Bundle args = new Bundle();
        args.putString("id", id);
        args.putString("name", name);
        args.putString("price", price);
        args.putString("spotId", spotId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_booking, container, false);

        if (getArguments() != null) {
            parkingLotId = getArguments().getString("id"); // حفظ الـ ID
            parkingLotName = getArguments().getString("name");
            parkingLotPrice = getArguments().getString("price");
            selectedSpotId = getArguments().getString("spotId");
        }

        tvParkingName = view.findViewById(R.id.tvBookingParkingName);
        tvPrice = view.findViewById(R.id.tvBookingParkingPrice);
        tvDate = view.findViewById(R.id.tvSelectedDate);
        tvStartTime = view.findViewById(R.id.tvSelectedStartTime);
        tvEndTime = view.findViewById(R.id.tvSelectedEndTime);
        tvTotal = view.findViewById(R.id.tvBookingTotalPrice);
        etPlate = view.findViewById(R.id.etPlateNumber);
        btnConfirm = view.findViewById(R.id.btnConfirmBooking);
        btnBack = view.findViewById(R.id.btnBackFromBooking);

        tvParkingName.setText(parkingLotName + " - موقف: " + selectedSpotId);
        tvPrice.setText(parkingLotPrice);

        selectedYear = calendar.get(Calendar.YEAR);
        selectedMonth = calendar.get(Calendar.MONTH);
        selectedDay = calendar.get(Calendar.DAY_OF_MONTH);
        tvDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay));

        tvStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute));
        tvEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute));

        calculateTotal();

        view.findViewById(R.id.btnSelectDate).setOnClickListener(v -> {
            new DatePickerDialog(getContext(), (view1, y, m, d) -> {
                selectedYear = y;
                selectedMonth = m;
                selectedDay = d;
                String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d);
                tvDate.setText(date);
            }, selectedYear, selectedMonth, selectedDay).show();
        });

        view.findViewById(R.id.btnSelectStartTime).setOnClickListener(v -> {
            new TimePickerDialog(getContext(), (view1, hourOfDay, minute) -> {
                startHour = hourOfDay;
                startMinute = minute;
                tvStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute));
                calculateTotal();
            }, startHour, startMinute, false).show();
        });

        view.findViewById(R.id.btnSelectEndTime).setOnClickListener(v -> {
            new TimePickerDialog(getContext(), (view1, hourOfDay, minute) -> {
                endHour = hourOfDay;
                endMinute = minute;
                tvEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute));
                calculateTotal();
            }, endHour, endMinute, false).show();
        });

        btnConfirm.setOnClickListener(v -> navigateToCheckout());
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        return view;
    }

    private void calculateTotal() {
        int durationMinutes = (endHour * 60 + endMinute) - (startHour * 60 + startMinute);

        if (durationMinutes <= 0) {
            tvTotal.setText("0 ر.س");
            calculatedHours = 0;
            return;
        }

        calculatedHours = (int) Math.ceil(durationMinutes / 60.0);
        if (calculatedHours == 0) calculatedHours = 1;

        int pricePerHour = 15;
        try {
            if (parkingLotPrice != null) {
                String numericPrice = parkingLotPrice.replaceAll("[^0-9]", "");
                if (!numericPrice.isEmpty()) {
                    pricePerHour = Integer.parseInt(numericPrice);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        int total = calculatedHours * pricePerHour;
        tvTotal.setText(total + " ر.س");
    }

    private void navigateToCheckout() {
        String plate = etPlate.getText().toString().trim();
        if (plate.isEmpty()) {
            Toast.makeText(getContext(), "يرجى إدخال رقم اللوحة", Toast.LENGTH_SHORT).show();
            return;
        }

        if (calculatedHours <= 0) {
            Toast.makeText(getContext(), "يرجى اختيار وقت مغادرة صحيح بعد وقت الوصول", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar startCal = Calendar.getInstance();
        startCal.set(selectedYear, selectedMonth, selectedDay, startHour, startMinute, 0);
        long startTimestamp = startCal.getTimeInMillis();

        Calendar endCal = Calendar.getInstance();
        endCal.set(selectedYear, selectedMonth, selectedDay, endHour, endMinute, 0);
        long endTimestamp = endCal.getTimeInMillis();

        if (endTimestamp <= startTimestamp) {
            Toast.makeText(getContext(), "وقت المغادرة يجب أن يكون بعد وقت الوصول", Toast.LENGTH_SHORT).show();
            return;
        }

        String durationStr = calculatedHours + " ساعات";
        String totalPrice = tvTotal.getText().toString();

        String dateStr = tvDate.getText().toString();
        String startStr = tvStartTime.getText().toString();
        String endStr = tvEndTime.getText().toString();


        Fragment checkoutFragment = CheckoutFragment.newInstance(
                parkingLotId,
                parkingLotName,
                selectedSpotId,
                plate,
                durationStr,
                totalPrice,
                parkingLotPrice,
                dateStr,
                startStr,
                endStr,
                startTimestamp,
                endTimestamp
        );

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, checkoutFragment)
                .addToBackStack(null)
                .commit();
    }
}