package com.example.powerxchange2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class EnergyListingAdapter extends RecyclerView.Adapter<EnergyListingAdapter.VH> {

    public interface Listener {
        void onBuyClick(EnergyListing listing);
        void onCreatePoolClick(EnergyListing listing);
    }

    private final List<EnergyListing> listings;
    private final UserProfile currentUser;
    private final Listener listener;

    public EnergyListingAdapter(List<EnergyListing> listings, UserProfile currentUser, Listener listener) {
        this.listings = listings;
        this.currentUser = currentUser;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_energy_listing, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        EnergyListing l = listings.get(position);

        h.tvSellerName.setText(l.getSellerName());
        h.tvCorporateBadge.setVisibility(l.isCorporate() ? View.VISIBLE : View.GONE);

        h.tvEnergyTypeLocation.setText(String.format(Locale.US, "%s • %s", l.getEnergyType(), l.getLocation()));
        h.tvKwhBadge.setText(String.format(Locale.US, "%.2f %s", l.getQuantity(), l.getUnit()));

        h.tvUnitPrice.setText(Config.formatPricePerUnit(l.getPricePerUnit(), l.getEnergyType()));
        h.tvTotalPrice.setText(String.format(Locale.US, "Total: %s", Config.formatCurrency(l.getTotalPrice())));

        String type = l.getEnergyType() == null ? "" : l.getEnergyType().toLowerCase();
        if (type.contains("solar")) h.ivTypeIcon.setImageResource(R.drawable.ic_solar);
        else if (type.contains("wind")) h.ivTypeIcon.setImageResource(R.drawable.ic_wind);
        else if (type.contains("battery")) h.ivTypeIcon.setImageResource(R.drawable.ic_battery);
        else if (type.contains("biogas")) h.ivTypeIcon.setImageResource(R.drawable.ic_biogas);
        else h.ivTypeIcon.setImageResource(R.drawable.ic_solar);

        boolean isOwn = currentUser != null && currentUser.getId() == l.getSellerId();
        boolean isDelivery = currentUser != null && currentUser.isDelivery();

        if (isOwn) {
            h.btnTradeNow.setEnabled(false);
            h.btnTradeNow.setText("Your Listing");
            h.btnTradeNow.setOnClickListener(null);
        } else if (isDelivery) {
            h.btnTradeNow.setVisibility(View.GONE);
        } else {
            h.btnTradeNow.setVisibility(View.VISIBLE);
            h.btnTradeNow.setEnabled(true);
            h.btnTradeNow.setText("Buy Now");
            h.btnTradeNow.setOnClickListener(v -> {
                if (listener != null) listener.onBuyClick(l);
            });
        }

        if (isOwn || isDelivery) {
            h.btnCreatePool.setVisibility(View.GONE);
        } else {
            h.btnCreatePool.setVisibility(View.VISIBLE);
            h.btnCreatePool.setOnClickListener(v -> {
                if (listener != null) listener.onCreatePoolClick(l);
            });
        }
    }

    @Override
    public int getItemCount() { return listings.size(); }

    public static class VH extends RecyclerView.ViewHolder {
        ImageView ivTypeIcon;
        TextView tvSellerName, tvCorporateBadge, tvEnergyTypeLocation, tvKwhBadge, tvUnitPrice, tvTotalPrice;
        MaterialButton btnCreatePool, btnTradeNow;

        public VH(@NonNull View itemView) {
            super(itemView);
            ivTypeIcon = itemView.findViewById(R.id.iv_type_icon);
            tvSellerName = itemView.findViewById(R.id.tv_seller_name);
            tvCorporateBadge = itemView.findViewById(R.id.tv_corporate_badge);
            tvEnergyTypeLocation = itemView.findViewById(R.id.tv_energy_type_location);
            tvKwhBadge = itemView.findViewById(R.id.tv_kwh_badge);
            tvUnitPrice = itemView.findViewById(R.id.tv_unit_price);
            tvTotalPrice = itemView.findViewById(R.id.tv_total_price);
            btnCreatePool = itemView.findViewById(R.id.btn_create_pool);
            btnTradeNow = itemView.findViewById(R.id.btn_trade_now);
        }
    }
}