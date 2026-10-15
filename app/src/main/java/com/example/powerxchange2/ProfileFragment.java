package com.example.powerxchange2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private SQLiteRepository repo;
    private UserProfile currentUser;

    private TextView tvProfileName, tvProfileEmail, tvRoleBadge, tvProfileWallet;
    private TextView tvKpiTraded, tvKpiCo2, tvKpiBiogas, tvKpiSold;
    private LinearLayout layoutBadges, layoutChartCard, layoutSoldCard;
    private TextView tvNoBadges;
    private GreenImpactChartView chart;
    private ImageButton btnLogout;
    private MaterialButton btnAddCredits, btnWithdrawCredits;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        repo = new SQLiteRepository(requireContext());

        tvProfileName = v.findViewById(R.id.tv_profile_name);
        tvProfileEmail = v.findViewById(R.id.tv_profile_email);
        tvRoleBadge = v.findViewById(R.id.tv_role_badge);
        tvProfileWallet = v.findViewById(R.id.tv_profile_wallet);
        tvKpiTraded = v.findViewById(R.id.tv_kpi_traded);
        tvKpiCo2 = v.findViewById(R.id.tv_kpi_co2);
        tvKpiBiogas = v.findViewById(R.id.tv_kpi_biogas);
        tvKpiSold = v.findViewById(R.id.tv_kpi_sold);
        layoutSoldCard = v.findViewById(R.id.layout_kpi_sold_card);
        layoutBadges = v.findViewById(R.id.layout_badges);
        tvNoBadges = v.findViewById(R.id.tv_no_badges);
        chart = v.findViewById(R.id.chart_green_impact);
        btnLogout = v.findViewById(R.id.btn_logout);
        btnAddCredits = v.findViewById(R.id.btn_add_credits);
        btnWithdrawCredits = v.findViewById(R.id.btn_withdraw_credits);

        btnLogout.setOnClickListener(x -> {
            repo.logout();
            Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(getActivity(), LoginActivity.class);
            startActivity(i);
            if (getActivity() != null) getActivity().finish();
        });

        btnAddCredits.setOnClickListener(x -> {
            if (currentUser != null && currentUser.isDelivery()) {
                Toast.makeText(getContext(), "Delivery partners cannot add credits.", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(getActivity(), AddCreditsActivity.class));
        });

        btnWithdrawCredits.setOnClickListener(x ->
                startActivity(new Intent(getActivity(), WithdrawCreditsActivity.class)));

        loadUserData();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }

    private void loadUserData() {
        currentUser = repo.getLoggedInUser();
        if (currentUser == null) return;

        tvProfileName.setText(currentUser.getName());
        tvProfileEmail.setText(currentUser.getEmail());

        if (currentUser.isDelivery()) {
            tvRoleBadge.setText("🚚 DELIVERY PARTNER");
        } else if (currentUser.isProsumer()) {
            tvRoleBadge.setText("⚡ PROSUMER (Producer & Seller)");
        } else {
            tvRoleBadge.setText("🌱 CONSUMER (Energy Buyer)");
        }
        tvProfileWallet.setText("Wallet Balance: " + Config.formatCurrency(currentUser.getWalletBalance()));

        // R2-4.4 / R2-4.3: real KPI data from completed trades
        SQLiteRepository.EnergyTotals totals = repo.getBuyerEnergyTotals(currentUser.getId());
        String mainEnergy = String.format(Locale.US, "%.2f kWh", totals.totalKwh());
        tvKpiTraded.setText(mainEnergy);
        tvKpiCo2.setText(String.format(Locale.US, "%.2f kg", totals.co2Kg));

        if (totals.biogasM3 > 0) {
            tvKpiBiogas.setVisibility(View.VISIBLE);
            tvKpiBiogas.setText(String.format(Locale.US, "%.2f m³", totals.biogasM3));
        } else {
            tvKpiBiogas.setVisibility(View.GONE);
        }

        // R2-4.7: prosumer sees energy sold
        if (currentUser.isProsumer()) {
            layoutSoldCard.setVisibility(View.VISIBLE);
            SQLiteRepository.EnergyTotals sold = repo.getSellerEnergyTotals(currentUser.getId());
            tvKpiSold.setText(String.format(Locale.US, "%.2f kWh / %.2f m³",
                    sold.totalKwh(), sold.biogasM3));
        } else {
            layoutSoldCard.setVisibility(View.GONE);
        }

        // R2-4.6: only real badges
        updateBadges(totals);

        // R2-4.5: real chart data
        List<SQLiteRepository.MonthlyPoint> points = repo.getBuyerMonthlyImpact(currentUser.getId(), 6);
        chart.setData(points);
    }

    private void updateBadges(SQLiteRepository.EnergyTotals totals) {
        layoutBadges.removeAllViews();

        List<String> earned = new ArrayList<>();
        int completed = repo.getCompletedBuyerTradeCount(currentUser.getId());
        int pools = repo.getJoinedPoolCount(currentUser.getId());

        if (completed >= 1) earned.add("🌱 First Trade");
        if (totals.co2Kg >= 100) earned.add("💚 Green Starter");
        if (pools >= 3) earned.add("🏆 Community Hero");

        if (earned.isEmpty()) {
            tvNoBadges.setVisibility(View.VISIBLE);
            return;
        }
        tvNoBadges.setVisibility(View.GONE);

        for (String label : earned) {
            TextView t = new TextView(getContext());
            t.setText(label);
            t.setTextColor(getResources().getColor(R.color.green_badge_text, null));
            t.setBackgroundResource(R.drawable.bg_badge_green);
            int p = (int) (14 * getResources().getDisplayMetrics().density);
            t.setPadding(p, p / 2, p, p / 2);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = (int) (8 * getResources().getDisplayMetrics().density);
            t.setLayoutParams(lp);
            layoutBadges.addView(t);
        }
    }
}