package com.example.smartparking.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartparking.R;
import com.example.smartparking.models.Reservation;

import java.util.List;

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ViewHolder> {

    private List<Reservation> list;
    private OnReservationClickListener listener;

    public interface OnReservationClickListener {
        void onReservationClick(Reservation reservation);
    }

    public ReservationAdapter(List<Reservation> list, OnReservationClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reservation, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reservation res = list.get(position);
        holder.tvName.setText(res.getParkingName());
        holder.tvPlate.setText(res.getPlateNumber());

        if (res.getDate() != null && res.getStartTime() != null && res.getEndTime() != null) {
            holder.tvDate.setText(res.getDate());
            holder.tvTime.setText(res.getStartTime() + " - " + res.getEndTime());
            holder.tvTime.setVisibility(View.VISIBLE);
        } else {
            holder.tvDate.setText(res.getDuration());
            holder.tvTime.setVisibility(View.GONE);
        }

        holder.tvTotal.setText(res.getTotalPrice());
        holder.tvStatus.setText(res.getStatus());

        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setCornerRadius(12f);

        if ("نشط".equals(res.getStatus())) {
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
            statusBg.setColor(Color.parseColor("#E8F5E9"));
        } else if ("محجوز".equals(res.getStatus())) {
            holder.tvStatus.setTextColor(Color.parseColor("#E65100"));
            statusBg.setColor(Color.parseColor("#FFF3E0"));
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#757575"));
            statusBg.setColor(Color.parseColor("#F5F5F5"));
        }

        holder.tvStatus.setBackground(statusBg);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReservationClick(res);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPlate, tvDate, tvTime, tvTotal, tvStatus;
        public ViewHolder(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvResParkingName);
            tvPlate = v.findViewById(R.id.tvResPlate);
            tvDate = v.findViewById(R.id.tvResDate);
            tvTime = v.findViewById(R.id.tvResTime);
            tvTotal = v.findViewById(R.id.tvResTotal);
            tvStatus = v.findViewById(R.id.tvResStatus);
        }
    }
}