package com.example.smartparking.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartparking.R;
import com.example.smartparking.models.ParkingLot;

import java.util.List;

public class AdminParkingAdapter extends RecyclerView.Adapter<AdminParkingAdapter.ViewHolder> {

    private List<ParkingLot> list;
    private OnParkingClickListener listener;

    public interface OnParkingClickListener {
        void onEditClick(ParkingLot parking);
        void onDeleteClick(ParkingLot parking);
    }

    public AdminParkingAdapter(List<ParkingLot> list, OnParkingClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_parking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ParkingLot item = list.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvPrice.setText(item.getPrice() + " ر.س/ساعة");

        holder.tvCapacity.setText(item.getAvailableCount() + " متاح");

        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(item));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPrice, tvCapacity;
        ImageView btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvParkingTitle);
            tvPrice = itemView.findViewById(R.id.tvParkingPrice);
            tvCapacity = itemView.findViewById(R.id.tvParkingCapacity);
            btnEdit = itemView.findViewById(R.id.btnEditParking);
            btnDelete = itemView.findViewById(R.id.btnDeleteParking);
        }
    }
}