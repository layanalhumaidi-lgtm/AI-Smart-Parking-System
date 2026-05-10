package com.example.smartparking;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

public class MyCardsFragment extends Fragment {

    private LinearLayout layoutCardsContainer;
    private ProgressBar pbLoadingCards;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private boolean isSelectionMode = false;

    public static MyCardsFragment newInstance(boolean isSelectionMode) {
        MyCardsFragment fragment = new MyCardsFragment();
        Bundle args = new Bundle();
        args.putBoolean("isSelectionMode", isSelectionMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_cards, container, false);

        if (getArguments() != null) {
            isSelectionMode = getArguments().getBoolean("isSelectionMode", false);
        }

        layoutCardsContainer = view.findViewById(R.id.layoutCardsContainer);
        pbLoadingCards = view.findViewById(R.id.pbLoadingCards);

        TextView tvTitle = view.findViewById(R.id.headerMyCards).findViewById(View.NO_ID);

        view.findViewById(R.id.btnBackMyCards).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        view.findViewById(R.id.btnAddNewCard).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AddCardFragment())
                    .addToBackStack(null)
                    .commit();
        });

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (mAuth.getCurrentUser() != null) {
            loadSavedCards(mAuth.getCurrentUser().getUid());
        } else {
            pbLoadingCards.setVisibility(View.GONE);
        }

        return view;
    }

    private void loadSavedCards(String userId) {
        mDatabase.child("SavedCards").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                layoutCardsContainer.removeAllViews();
                pbLoadingCards.setVisibility(View.GONE);
                boolean hasCards = false;

                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        if (child.hasChild("last4Digits")) {
                            hasCards = true;
                            String cardId = child.getKey();
                            String last4 = child.child("last4Digits").getValue(String.class);
                            String cardHolder = child.child("cardHolderName").getValue(String.class);
                            addCardViewToLayout(cardId, last4, cardHolder);
                        }
                    }
                }

                if (!hasCards) {
                    TextView emptyText = new TextView(getContext());
                    emptyText.setText("لا توجد بطاقات محفوظة حالياً");
                    emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    emptyText.setTextColor(Color.GRAY);
                    emptyText.setPadding(0, 50, 0, 0);
                    layoutCardsContainer.addView(emptyText);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) pbLoadingCards.setVisibility(View.GONE);
                Toast.makeText(getContext(), "خطأ في جلب البطاقات", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addCardViewToLayout(String cardId, String last4, String cardHolder) {
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.item_saved_card, layoutCardsContainer, false);

        TextView tvSavedCardNumber = cardView.findViewById(R.id.tvSavedCardNumber);
        TextView tvSavedCardName = cardView.findViewById(R.id.tvSavedCardName);

        tvSavedCardNumber.setText("••••  ••••  ••••  " + (last4 != null ? last4 : "0000"));
        tvSavedCardName.setText(cardHolder != null && !cardHolder.isEmpty() ? cardHolder : "الاسم غير مسجل");

        cardView.setOnClickListener(v -> {
            if (isSelectionMode) {
                Bundle result = new Bundle();
                result.putString("last4Digits", last4);
                getParentFragmentManager().setFragmentResult("cardRequestKey", result);

                Toast.makeText(getContext(), "تم اختيار البطاقة", Toast.LENGTH_SHORT).show();

                getParentFragmentManager().popBackStack();
            } else {
                AddCardFragment editCardFragment = AddCardFragment.newInstance(cardId);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, editCardFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        layoutCardsContainer.addView(cardView);
    }
}