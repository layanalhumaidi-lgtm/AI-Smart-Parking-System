package com.example.smartparking;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AddCardFragment extends Fragment {

    private EditText etCardName, etCardNumber, etCardExpiry, etCardCVV;
    private TextView tvMockCardName, tvMockCardNumber;
    private Button btnSaveCard;
    private View btnBackAddCard;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;


    private String existingCardId = null;


    public static AddCardFragment newInstance(String cardId) {
        AddCardFragment fragment = new AddCardFragment();
        Bundle args = new Bundle();
        args.putString("cardId", cardId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_card, container, false);

        if (getArguments() != null) {
            existingCardId = getArguments().getString("cardId");
        }

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etCardName = view.findViewById(R.id.etCardName);
        etCardNumber = view.findViewById(R.id.etCardNumber);
        etCardExpiry = view.findViewById(R.id.etCardExpiry);
        etCardCVV = view.findViewById(R.id.etCardCVV);
        tvMockCardName = view.findViewById(R.id.tvMockCardName);
        tvMockCardNumber = view.findViewById(R.id.tvMockCardNumber);
        btnSaveCard = view.findViewById(R.id.btnSaveCard);
        btnBackAddCard = view.findViewById(R.id.btnBackAddCard);

        setupLiveMockupUpdates();


        if (existingCardId != null) {
            btnSaveCard.setText("تحديث بيانات البطاقة");
            loadExistingCardData();
        }

        btnBackAddCard.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnSaveCard.setOnClickListener(v -> handleSaveCard());

        return view;
    }


    private void loadExistingCardData() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();


        mDatabase.child("SavedCards").child(userId).child(existingCardId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && isAdded()) {
                            String name = snapshot.child("cardHolderName").getValue(String.class);
                            String number = snapshot.child("cardNumber").getValue(String.class);
                            String expiry = snapshot.child("expiryDate").getValue(String.class);
                            String cvv = snapshot.child("cvv").getValue(String.class);

                            if (name != null) etCardName.setText(name);
                            if (number != null) etCardNumber.setText(number);
                            if (expiry != null) etCardExpiry.setText(expiry);
                            if (cvv != null) etCardCVV.setText(cvv);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "خطأ في تحميل بيانات البطاقة", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void setupLiveMockupUpdates() {
        etCardName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    tvMockCardName.setText("الاسم على البطاقة");
                } else {
                    tvMockCardName.setText(s.toString());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etCardNumber.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;

                String rawStr = s.toString().replaceAll("\\s+", "");
                StringBuilder formattedStr = new StringBuilder();

                for (int i = 0; i < rawStr.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formattedStr.append(" ");
                    }
                    formattedStr.append(rawStr.charAt(i));
                }

                etCardNumber.setText(formattedStr.toString());
                etCardNumber.setSelection(formattedStr.length());

                if (rawStr.isEmpty()) {
                    tvMockCardNumber.setText("••••  ••••  ••••  ••••");
                } else {
                    tvMockCardNumber.setText(formattedStr.toString());
                }

                isFormatting = false;
            }
        });
    }

    private void handleSaveCard() {
        String name = etCardName.getText().toString().trim();
        String rawNumber = etCardNumber.getText().toString().replaceAll("\\s+", "");
        String expiry = etCardExpiry.getText().toString().trim();
        String cvv = etCardCVV.getText().toString().trim();

        if (name.isEmpty() || rawNumber.isEmpty() || expiry.isEmpty() || cvv.isEmpty()) {
            Toast.makeText(getContext(), "يرجى ملء جميع بيانات البطاقة", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rawNumber.length() < 16) {
            Toast.makeText(getContext(), "رقم البطاقة غير مكتمل", Toast.LENGTH_SHORT).show();
            return;
        }

        String last4Digits = rawNumber.substring(rawNumber.length() - 4);

        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();

            Map<String, Object> cardData = new HashMap<>();
            cardData.put("cardHolderName", name);
            cardData.put("cardNumber", rawNumber);
            cardData.put("last4Digits", last4Digits);
            cardData.put("expiryDate", expiry);
            cardData.put("cvv", cvv);


            String cardIdToSave = existingCardId;
            if (cardIdToSave == null) {

                cardIdToSave = mDatabase.child("SavedCards").child(userId).push().getKey();
            }

            if (cardIdToSave != null) {

                mDatabase.child("SavedCards").child(userId).child(cardIdToSave).setValue(cardData)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Bundle result = new Bundle();
                                result.putString("last4Digits", last4Digits);
                                getParentFragmentManager().setFragmentResult("cardRequestKey", result);

                                String successMessage = (existingCardId == null) ? "تم حفظ البطاقة بنجاح" : "تم تحديث البطاقة بنجاح";
                                Toast.makeText(getContext(), successMessage, Toast.LENGTH_SHORT).show();
                                getParentFragmentManager().popBackStack();
                            } else {
                                Toast.makeText(getContext(), "فشل حفظ البطاقة في قاعدة البيانات", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        } else {
            Toast.makeText(getContext(), "يرجى تسجيل الدخول أولاً", Toast.LENGTH_SHORT).show();
        }
    }
}