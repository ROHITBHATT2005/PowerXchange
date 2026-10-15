package com.example.powerxchange2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class SwapRequestAdapter extends RecyclerView.Adapter<SwapRequestAdapter.SwapViewHolder> {

    public interface OnAcceptSwapListener {
        void onAcceptSwap(BarterOffer barter);
    }

    private final List<BarterOffer> barters;
    private final OnAcceptSwapListener listener;
    private final UserProfile currentUser;
    public SwapRequestAdapter(List<BarterOffer> barters, UserProfile currentUser, OnAcceptSwapListener listener) {
        this.barters = barters;
        this.currentUser = currentUser;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SwapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_swap_request, parent, false);
        return new SwapViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SwapViewHolder holder, int position) {
        BarterOffer barter = barters.get(position);

        holder.tvRequesterName.setText(barter.getCreatorName());
        holder.tvSwapStatus.setText(barter.getStatus());

        holder.tvOfferKwh.setText(String.format(Locale.US, "%.1f %s %s", barter.getGiveQty(), barter.getGiveUnit(), barter.getGiveType()));
        holder.tvRequestKwh.setText(String.format(Locale.US, "%.1f %s %s", barter.getWantQty(), barter.getWantUnit(), barter.getWantType()));

        String noteLoc = String.format(Locale.US, "📍 %s %s", barter.getLocation(),
                (barter.getNote() != null && !barter.getNote().isEmpty() ? "• " + barter.getNote() : ""));
        holder.tvTimeSlot.setText(noteLoc);

        if ("ACCEPTED".equalsIgnoreCase(barter.getStatus())) {
            holder.btnAcceptSwap.setEnabled(false);
            holder.btnAcceptSwap.setText("Accepted ✓");
        } else if (currentUser != null && currentUser.getId() == barter.getCreatorId()) {
            holder.btnAcceptSwap.setEnabled(false);
            holder.btnAcceptSwap.setText("Your Offer");
        } else {
            holder.btnAcceptSwap.setEnabled(true);
            holder.btnAcceptSwap.setText("Accept Barter");
            holder.btnAcceptSwap.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAcceptSwap(barter);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return barters.size();
    }

    public static class SwapViewHolder extends RecyclerView.ViewHolder {
        TextView tvRequesterName, tvSwapStatus, tvOfferKwh, tvRequestKwh, tvTimeSlot;
        MaterialButton btnAcceptSwap;

        public SwapViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRequesterName = itemView.findViewById(R.id.tv_requester_name);
            tvSwapStatus = itemView.findViewById(R.id.tv_swap_status);
            tvOfferKwh = itemView.findViewById(R.id.tv_offer_kwh);
            tvRequestKwh = itemView.findViewById(R.id.tv_request_kwh);
            tvTimeSlot = itemView.findViewById(R.id.tv_time_slot);
            btnAcceptSwap = itemView.findViewById(R.id.btn_accept_swap);
        }
    }
}