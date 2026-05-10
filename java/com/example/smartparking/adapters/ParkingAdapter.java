package com.example.smartparking.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartparking.R;
import com.example.smartparking.models.ParkingLot;

import java.util.List;

public class ParkingAdapter extends RecyclerView.Adapter<ParkingAdapter.ViewHolder> {

    private List<ParkingLot> parkingLotList;
    private OnParkingClickListener listener;

    public interface OnParkingClickListener {
        void onParkingClick(ParkingLot parkingLot);
    }

    public ParkingAdapter(List<ParkingLot> list, OnParkingClickListener listener) {
        this.parkingLotList = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_parking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ParkingLot item = parkingLotList.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvPrice.setText(item.getPrice() + " ر.س/ساعة");
        holder.tvDistance.setText(item.getDistance());

        int available = item.getAvailableCount();
        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setCornerRadius(30f);

        if (available == 0) {
            holder.tvAvailability.setText("ممتلئ");
            holder.tvAvailability.setTextColor(Color.parseColor("#9E9E9E"));
            bgShape.setColor(Color.parseColor("#F5F5F5"));

            holder.tvSpecialNeeds.setText("0 مواقف متاحة");
            holder.tvSpecialIcon.setVisibility(View.VISIBLE);
        } else if (available <= 5) {
            holder.tvAvailability.setText(available + " متاح");
            holder.tvAvailability.setTextColor(Color.parseColor("#D97A9A"));
            bgShape.setColor(Color.parseColor("#FCE4EC"));

            holder.tvSpecialNeeds.setText(item.getSpecialNeedsCount() + " موقف ذوي الاحتياجات");
            holder.tvSpecialIcon.setVisibility(View.VISIBLE);
        } else {
            holder.tvAvailability.setText(available + " متاح");
            holder.tvAvailability.setTextColor(Color.parseColor("#4CAF50"));
            bgShape.setColor(Color.parseColor("#E8F5E9"));

            holder.tvSpecialNeeds.setText(item.getSpecialNeedsCount() + " مواقف ذوي الاحتياجات");
            holder.tvSpecialIcon.setVisibility(View.VISIBLE);
        }

        holder.tvAvailability.setBackground(bgShape);

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty() && item.getImageUrl().startsWith("data:image")) {
            try {
                String base64String = item.getImageUrl().split(",")[1];
                byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                holder.viewHeader.setImageBitmap(decodedByte);
                holder.viewHeader.setBackgroundResource(0);
            } catch (Exception e) {
                holder.viewHeader.setImageBitmap(null);
                holder.viewHeader.setBackgroundResource(item.getGradientResId());
            }
        } else {
            holder.viewHeader.setImageBitmap(null);
            holder.viewHeader.setBackgroundResource(item.getGradientResId());
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onParkingClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return parkingLotList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAvailability, tvSpecialNeeds, tvSpecialIcon, tvPrice, tvDistance;
        ImageView viewHeader;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvParkingTitle);
            tvAvailability = itemView.findViewById(R.id.tvParkingAvailability);
            tvSpecialNeeds = itemView.findViewById(R.id.tvSpecialNeeds);
            tvSpecialIcon = itemView.findViewById(R.id.tvSpecialIcon);
            tvPrice = itemView.findViewById(R.id.tvParkingPrice);
            tvDistance = itemView.findViewById(R.id.tvParkingDistance);
            viewHeader = itemView.findViewById(R.id.viewHeaderColor);
        }
    }
}