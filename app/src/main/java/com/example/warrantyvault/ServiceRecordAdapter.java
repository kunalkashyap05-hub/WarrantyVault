package com.example.warrantyvault;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ServiceRecordAdapter extends RecyclerView.Adapter<ServiceRecordAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(ServiceRecord record);
    }

    private final List<ServiceRecord> list;
    private final OnDeleteListener deleteListener;

    public ServiceRecordAdapter(List<ServiceRecord> list, OnDeleteListener deleteListener) {
        this.list = list;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_service_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServiceRecord record = list.get(position);
        holder.tvProductName.setText(record.getProductName() != null ? record.getProductName() : "Product Service");
        holder.tvServiceCentre.setText((record.getServiceCentre() != null ? record.getServiceCentre() : "") + " • " + (record.getServiceDate() != null ? record.getServiceDate() : ""));
        holder.tvDescription.setText(record.getDescription() != null && !record.getDescription().isEmpty() ? record.getDescription() : "Routine Service");
        String cost = record.getCost() != null ? record.getCost() : "0";
        try {
            double c = Double.parseDouble(cost);
            cost = c == Math.floor(c) ? String.valueOf((long) c) : String.format(java.util.Locale.getDefault(), "%.2f", c);
        } catch (NumberFormatException ignored) {
        }
        holder.tvCostBadge.setText("₹" + cost);

        UiUtils.applyPressScaleAnimation(holder.itemView);
        // long-press a record to delete it
        holder.itemView.setOnLongClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(record);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName, tvServiceCentre, tvDescription, tvCostBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvServiceCentre = itemView.findViewById(R.id.tvServiceCentre);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCostBadge = itemView.findViewById(R.id.tvCostBadge);
        }
    }
}
