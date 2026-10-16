package com.example.warrantyvault;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final List<Product> productList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }

    public ProductAdapter(List<Product> productList, OnItemClickListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.tvProductName.setText(product.getProductName());
        holder.tvBrandCategory.setText((product.getBrand() != null ? product.getBrand() : "") + " • " + (product.getCategory() != null ? product.getCategory() : ""));
        String pname = product.getProductName();
        holder.tvInitial.setText(pname != null && !pname.isEmpty() ? pname.substring(0, 1).toUpperCase() : "P");
        holder.tvExpiry.setText("Expires: " + (product.getWarrantyExpiry() != null && !product.getWarrantyExpiry().isEmpty() ? product.getWarrantyExpiry() : "—"));
        holder.tvDaysLeft.setText(WarrantyUtils.describe(product.getWarrantyExpiry()));
        boolean hasBill = product.getBillImageUrl() != null && !product.getBillImageUrl().isEmpty();
        holder.tvDocsBadge.setText(hasBill ? "Bill saved" : "No bill");

        String status = product.getWarrantyStatus();
        holder.tvStatus.setText(status != null ? status.toUpperCase() : "ACTIVE");

        if ("EXPIRED".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_expired);
            holder.tvStatus.setTextColor(Color.parseColor("#E11D48"));
        } else if ("EXPIRING SOON".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_expiring);
            holder.tvStatus.setTextColor(Color.parseColor("#D97706"));
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_active);
            holder.tvStatus.setTextColor(Color.parseColor("#059669"));
        }

        UiUtils.applyPressScaleAnimation(holder.itemView);
        UiUtils.applyLongPressLiftAnimation(holder.itemView, () -> listener.onItemClick(product));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(product));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName, tvBrandCategory, tvExpiry, tvStatus, tvInitial, tvDaysLeft, tvDocsBadge;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvBrandCategory = itemView.findViewById(R.id.tvBrandCategory);
            tvExpiry = itemView.findViewById(R.id.tvExpiry);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvInitial = itemView.findViewById(R.id.tvInitial);
            tvDaysLeft = itemView.findViewById(R.id.tvDaysLeft);
            tvDocsBadge = itemView.findViewById(R.id.tvDocsBadge);
        }
    }
}
