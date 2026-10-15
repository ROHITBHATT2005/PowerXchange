package com.example.powerxchange2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;
import java.util.Locale;

public class PoolsAdapter extends RecyclerView.Adapter<PoolsAdapter.VH> {

    public interface Listener {
        void onJoinPool(EnergyPool pool);
        void onClosePool(EnergyPool pool);
    }

    private final List<EnergyPool> pools;
    private final UserProfile currentUser;
    private final Listener listener;

    public PoolsAdapter(List<EnergyPool> pools, UserProfile currentUser, Listener listener) {
        this.pools = pools;
        this.currentUser = currentUser;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pool, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        EnergyPool p = pools.get(position);

        h.tvPoolTitle.setText(String.format(Locale.US, "%s Pool by %s",
                p.getEnergyType() != null ? p.getEnergyType() : "Energy",
                p.getCreatorName()));
        h.tvPoolPriceRate.setText(String.format(Locale.US, "%s • %s",
                p.getEnergyType() != null ? p.getEnergyType() : "",
                Config.formatPricePerUnit(p.getPricePerUnit(), p.getEnergyType())));
        h.tvPoolStatusBadge.setText(p.getStatus());

        h.tvPoolKwhProgress.setText(String.format(Locale.US,
                "%.2f / %.2f %s (Remaining: %.2f %s)",
                p.getJoinedQty(), p.getTotalQty(), p.getUnit(),
                p.getRemainingQty(), p.getUnit()));

        int pct = p.getProgressPercentage();
        h.tvPoolPct.setText(String.format(Locale.US, "%d%%", pct));
        h.progressPool.setProgress(pct);

        String type = p.getEnergyType() == null ? "" : p.getEnergyType().toLowerCase();
        h.ivPoolIcon.setImageResource(type.contains("wind") ? R.drawable.ic_wind : R.drawable.ic_solar);

        // Location / seller line
        if (h.tvPoolLocation != null) {
            String loc = p.getLocation() != null ? p.getLocation() : "";
            String seller = p.getSellerName() != null ? p.getSellerName() : "";
            h.tvPoolLocation.setText(String.format(Locale.US, "%s • %s", seller, loc));
        }

        boolean isCreator = currentUser != null && currentUser.getId() == p.getCreatorId();
        boolean isOpen = p.isOpen() && p.getRemainingQty() > 0.0001;

        if (isOpen && isCreator) {
            // Creator can close early (R2-2.5)
            h.btnJoinPool.setVisibility(View.VISIBLE);
            h.btnJoinPool.setText("Close Pool Early");
            h.btnJoinPool.setEnabled(true);
            h.btnJoinPool.setOnClickListener(v -> {
                if (listener != null) listener.onClosePool(p);
            });
        } else if (isOpen) {
            h.btnJoinPool.setVisibility(View.VISIBLE);
            h.btnJoinPool.setText("Join Pool");
            h.btnJoinPool.setEnabled(true);
            h.btnJoinPool.setOnClickListener(v -> {
                if (listener != null) listener.onJoinPool(p);
            });
        } else {
            h.btnJoinPool.setVisibility(View.VISIBLE);
            h.btnJoinPool.setEnabled(false);
            h.btnJoinPool.setText(p.isFull() ? "Full" : (p.isClosed() ? "Closed" : "Not open"));
        }
    }

    @Override
    public int getItemCount() { return pools.size(); }

    public static class VH extends RecyclerView.ViewHolder {
        ImageView ivPoolIcon;
        TextView tvPoolTitle, tvPoolPriceRate, tvPoolStatusBadge, tvPoolKwhProgress, tvPoolPct, tvPoolLocation;
        LinearProgressIndicator progressPool;
        MaterialButton btnJoinPool;

        public VH(@NonNull View itemView) {
            super(itemView);
            ivPoolIcon = itemView.findViewById(R.id.iv_pool_icon);
            tvPoolTitle = itemView.findViewById(R.id.tv_pool_title);
            tvPoolPriceRate = itemView.findViewById(R.id.tv_pool_price_rate);
            tvPoolStatusBadge = itemView.findViewById(R.id.tv_pool_status_badge);
            tvPoolKwhProgress = itemView.findViewById(R.id.tv_pool_kwh_progress);
            tvPoolPct = itemView.findViewById(R.id.tv_pool_pct);
            tvPoolLocation = itemView.findViewById(R.id.tv_pool_location);
            progressPool = itemView.findViewById(R.id.progress_pool);
            btnJoinPool = itemView.findViewById(R.id.btn_join_pool);
        }
    }
}