package com.example.powerxchange2;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class AutoBuyHistoryAdapter extends RecyclerView.Adapter<AutoBuyHistoryAdapter.VH> {

    private final List<AutoBuyHistory> items;

    public AutoBuyHistoryAdapter(List<AutoBuyHistory> items) { this.items = items; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_auto_buy_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AutoBuyHistory a = items.get(position);
        h.tvTime.setText(a.createdAt);
        h.tvItem.setText(String.format(Locale.US, "%.2f %s %s from %s",
                a.quantity, a.unit, a.energyType, a.sellerName));
        h.tvPrice.setText(a.isBought()
                ? "Paid: " + Config.formatCurrency(a.pricePaid)
                : "No charge");
        h.tvStatus.setText(a.status);
        h.tvStatus.setTextColor(a.isBought() ? Color.parseColor("#00875A") : Color.parseColor("#B00020"));
    }

    @Override
    public int getItemCount() { return items.size(); }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvTime, tvItem, tvPrice, tvStatus;
        public VH(@NonNull View v) {
            super(v);
            tvTime = v.findViewById(R.id.tv_ab_time);
            tvItem = v.findViewById(R.id.tv_ab_item);
            tvPrice = v.findViewById(R.id.tv_ab_price);
            tvStatus = v.findViewById(R.id.tv_ab_status);
        }
    }
}