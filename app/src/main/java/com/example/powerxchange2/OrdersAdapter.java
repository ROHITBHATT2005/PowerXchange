package com.example.powerxchange2;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.VH> {

    private final boolean isBuyerView;   // kept for compatibility
    private final List<Trade> trades;
    private final long currentUserId;
    private final SQLiteRepository repo;

    public OrdersAdapter(List<Trade> trades, boolean isBuyerView, long currentUserId, SQLiteRepository repo) {
        this.trades = trades;
        this.isBuyerView = isBuyerView;
        this.currentUserId = currentUserId;
        this.repo = repo;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Trade t = trades.get(position);
        boolean isBarter = t.isBarter();

        // R2-11.5: RECEIVING / SENDING labels for barter
        if (isBarter) {
            if (currentUserId == t.getBuyerId()) {
                h.tvOrderItem.setText(String.format(Locale.US,
                        "RECEIVING: %s %.2f %s from %s",
                        t.getEnergyType(), t.getQuantity(), t.getUnit(), t.getSellerName()));
            } else {
                h.tvOrderItem.setText(String.format(Locale.US,
                        "SENDING: %s %.2f %s to %s",
                        t.getEnergyType(), t.getQuantity(), t.getUnit(), t.getBuyerName()));
            }
        } else {
            h.tvOrderItem.setText(String.format(Locale.US, "%.2f %s %s",
                    t.getQuantity(), t.getUnit(), t.getEnergyType()));
        }

        h.tvOrderAmount.setText(Config.formatCurrency(t.getTotalPrice()));
        h.tvOrderSeller.setText(String.format(Locale.US, "Sender: %s", t.getSellerName()));
        h.tvOrderBuyer.setText(String.format(Locale.US, "Receiver: %s", t.getBuyerName()));
        h.tvOrderTime.setText(t.getTimestamp());

        String partner = repo.getPartnerNameForTrade(t.getId());
        h.tvOrderPartner.setText("Partner: " + (partner != null ? partner : "Not assigned"));

        String status = repo.getDeliveryStatusForTrade(t.getId());
        h.tvOrderStatus.setText(status);
        paintTracker(h, status);

        // R2-11.5: code visible only on the RECEIVING card
        boolean isReceiver = currentUserId == t.getBuyerId();
        if (isReceiver) {
            h.layoutCode.setVisibility(View.VISIBLE);
            h.tvOrderDeliveryCode.setText(t.getDeliveryCode());
            h.btnCopyCode.setOnClickListener(v -> {
                ClipboardManager cm = (ClipboardManager) v.getContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("Delivery Code", t.getDeliveryCode()));
                    Toast.makeText(v.getContext(), "Code copied", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            h.layoutCode.setVisibility(View.GONE);
        }
    }

    private void paintTracker(VH h, String status) {
        int active = 0;
        if ("PLACED".equalsIgnoreCase(status)) active = 1;
        else if ("ACCEPTED".equalsIgnoreCase(status)) active = 2;
        else if ("PICKED UP".equalsIgnoreCase(status) || "PICKED_UP".equalsIgnoreCase(status)) active = 3;
        else if ("DELIVERED".equalsIgnoreCase(status)) active = 4;

        int on = 0xFF00875A;
        int off = 0xFFDADADA;

        h.step1.setBackgroundColor(active >= 1 ? on : off);
        h.step2.setBackgroundColor(active >= 2 ? on : off);
        h.step3.setBackgroundColor(active >= 3 ? on : off);
        h.step4.setBackgroundColor(active >= 4 ? on : off);
    }

    @Override
    public int getItemCount() { return trades.size(); }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvOrderItem, tvOrderAmount, tvOrderSeller, tvOrderBuyer, tvOrderTime,
                tvOrderPartner, tvOrderStatus, tvOrderDeliveryCode;
        MaterialButton btnCopyCode;
        View layoutCode, step1, step2, step3, step4;

        public VH(@NonNull View v) {
            super(v);
            tvOrderItem = v.findViewById(R.id.tv_order_item);
            tvOrderAmount = v.findViewById(R.id.tv_order_amount);
            tvOrderSeller = v.findViewById(R.id.tv_order_seller);
            tvOrderBuyer = v.findViewById(R.id.tv_order_buyer);
            tvOrderTime = v.findViewById(R.id.tv_order_time);
            tvOrderPartner = v.findViewById(R.id.tv_order_partner);
            tvOrderStatus = v.findViewById(R.id.tv_order_status);
            tvOrderDeliveryCode = v.findViewById(R.id.tv_order_delivery_code);
            btnCopyCode = v.findViewById(R.id.btn_copy_code);
            layoutCode = v.findViewById(R.id.layout_order_code);
            step1 = v.findViewById(R.id.step1);
            step2 = v.findViewById(R.id.step2);
            step3 = v.findViewById(R.id.step3);
            step4 = v.findViewById(R.id.step4);
        }
    }
}