package com.example.smartparking.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartparking.R;
import com.example.smartparking.models.ParkingSlot;

import java.util.List;

public class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.ViewHolder> {

    private List<ParkingSlot> slots;
    private OnSlotClickListener listener;
    private int selectedPosition = -1;

    public interface OnSlotClickListener {
        void onSlotClick(ParkingSlot slot);
    }

    public SlotAdapter(List<ParkingSlot> slots, OnSlotClickListener listener) {
        this.slots = slots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_slot, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ParkingSlot slot = slots.get(position);
        holder.tvId.setText(slot.getSlotId());


        if (slot.isSpecialNeeds()) {
            holder.ivSpecialNeeds.setVisibility(View.VISIBLE);
        } else {
            holder.ivSpecialNeeds.setVisibility(View.GONE);
        }

        if (position == selectedPosition) {
            holder.card.setCardBackgroundColor(Color.parseColor("#D97A9A"));
        } else {

            if ("reserved".equals(slot.getStatus())) {
                holder.card.setCardBackgroundColor(Color.parseColor("#9E9E9E"));
            } else if (slot.isSpecialNeeds()) {
                holder.card.setCardBackgroundColor(Color.parseColor("#C5A36F"));
            } else {
                holder.card.setCardBackgroundColor(Color.parseColor("#8AB682"));
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (!slot.getStatus().equals("reserved")) {
                int previousSelected = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPosition);
                listener.onSlotClick(slot);
            }
        });
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        TextView tvId;
        ImageView ivSpecialNeeds;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (CardView) itemView;
            tvId = itemView.findViewById(R.id.tvSlotId);
            ivSpecialNeeds = itemView.findViewById(R.id.ivSpecialNeedsIcon);
        }
    }
}