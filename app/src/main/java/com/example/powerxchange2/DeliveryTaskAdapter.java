package com.example.powerxchange2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DeliveryTaskAdapter extends RecyclerView.Adapter<DeliveryTaskAdapter.VH> {

    public interface Listener {
        void onAccept(DeliveryTask task);
        void onPickedUp(DeliveryTask task);
        void onDelivered(DeliveryTask task, String code);
    }

    private final List<DeliveryTask> tasks;
    private final Listener listener;
    private final Map<Long, Integer> attempts = new HashMap<>();

    public DeliveryTaskAdapter(List<DeliveryTask> tasks, Listener l) {
        this.tasks = tasks;
        this.listener = l;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_delivery_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DeliveryTask t = tasks.get(position);

        h.tvItem.setText(String.format(Locale.US, "%.2f %s %s",
                t.getQuantity(), t.getUnit(), t.getEnergyType()));

        // R2-11.4: BARTER — Leg N tag
        if (t.isBarter()) {
            h.tvStatus.setText(String.format(Locale.US, "BARTER — Leg %d", t.getLegNo()));
        } else {
            h.tvStatus.setText(t.getStatus());
        }

        h.tvPickup.setText("📍 PICKUP: " + t.getSellerName() + " (" + t.getPickupAddress() + ")");
        h.tvDrop.setText("🏠 DROP: " + t.getBuyerName() + " (" + t.getDropAddress() + ")");
        h.tvDistance.setText(String.format(Locale.US, "🛣️ Distance: %.1f km", t.getDistanceKm()));
        h.tvEarning.setText("Earning: " + Config.formatCurrency(t.getPartnerCut()));
        h.tvFee.setText("Total Fee: " + Config.formatCurrency(t.getDeliveryFeeTotal()));

        String s = t.getStatus();
        h.etCode.setText("");
        if ("AVAILABLE".equalsIgnoreCase(s)) {
            h.btnAccept.setVisibility(View.VISIBLE);
            h.btnAccept.setEnabled(true);
            h.btnAccept.setText("Accept Delivery");
            h.btnAccept.setOnClickListener(v -> { if (listener != null) listener.onAccept(t); });
            h.btnPickup.setVisibility(View.GONE);
            h.layoutCode.setVisibility(View.GONE);
        } else if ("ACCEPTED".equalsIgnoreCase(s)) {
            h.btnAccept.setVisibility(View.GONE);
            h.btnPickup.setVisibility(View.VISIBLE);
            h.btnPickup.setText("Mark Order Picked Up");
            h.btnPickup.setEnabled(true);
            h.btnPickup.setOnClickListener(v -> { if (listener != null) listener.onPickedUp(t); });
            h.layoutCode.setVisibility(View.GONE);
        } else if ("PICKED_UP".equalsIgnoreCase(s)) {
            h.btnAccept.setVisibility(View.GONE);
            h.btnPickup.setVisibility(View.GONE);
            h.layoutCode.setVisibility(View.VISIBLE);
            h.btnDeliver.setOnClickListener(v -> {
                String input = h.etCode.getText() != null ? h.etCode.getText().toString().trim() : "";
                if (input.length() != 6) {
                    Toast.makeText(v.getContext(), "Enter 6-digit code", Toast.LENGTH_SHORT).show();
                    return;
                }
                int a = attempts.getOrDefault(t.getId(), 0) + 1;
                attempts.put(t.getId(), a);
                if (a > 5) {
                    Toast.makeText(v.getContext(), "Max attempts reached", Toast.LENGTH_LONG).show();
                    h.btnDeliver.setEnabled(false);
                    return;
                }
                if (listener != null) listener.onDelivered(t, input);
            });
        } else if ("DELIVERED".equalsIgnoreCase(s)) {
            h.btnAccept.setVisibility(View.VISIBLE);
            h.btnAccept.setText("Delivered ✓");
            h.btnAccept.setEnabled(false);
            h.btnPickup.setVisibility(View.GONE);
            h.layoutCode.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return tasks.size(); }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvItem, tvStatus, tvPickup, tvDrop, tvDistance, tvEarning, tvFee;
        MaterialButton btnAccept, btnPickup, btnDeliver;
        LinearLayout layoutCode;
        TextInputEditText etCode;

        public VH(@NonNull View v) {
            super(v);
            tvItem = v.findViewById(R.id.tv_delivery_item);
            tvStatus = v.findViewById(R.id.tv_delivery_status);
            tvPickup = v.findViewById(R.id.tv_pickup_location);
            tvDrop = v.findViewById(R.id.tv_drop_location);
            tvDistance = v.findViewById(R.id.tv_distance_km);
            tvEarning = v.findViewById(R.id.tv_delivery_partner_cut);
            tvFee = v.findViewById(R.id.tv_delivery_fee_total);
            btnAccept = v.findViewById(R.id.btn_accept_delivery);
            btnPickup = v.findViewById(R.id.btn_mark_picked_up);
            btnDeliver = v.findViewById(R.id.btn_mark_delivered);
            layoutCode = v.findViewById(R.id.layout_code_verification);
            etCode = v.findViewById(R.id.et_delivery_code_input);
        }
    }
}