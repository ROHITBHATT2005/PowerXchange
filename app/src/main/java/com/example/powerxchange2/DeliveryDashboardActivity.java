package com.example.powerxchange2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class DeliveryDashboardActivity extends AppCompatActivity {

    private SQLiteRepository repo;
    private UserProfile partner;

    private TextView tvPartnerName,tvPartnerWallet, tvEmpty, tvTotalEarnings;
    private ImageButton btnLogout;
    private TabLayout tabs;
    private RecyclerView rv;
    private LinearLayout layoutEarnings;
    private MaterialButton btnWithdraw;
    private DeliveryTaskAdapter adapter;
    private final List<DeliveryTask> list = new ArrayList<>();
    private int tabIndex = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_delivery_dashboard);
        repo = new SQLiteRepository(this);
        partner = repo.getLoggedInUser();
        if (partner == null || !partner.isDelivery()) {
            Toast.makeText(this, "Unauthorized", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        tvPartnerName = findViewById(R.id.tv_partner_name);
        tvPartnerWallet = findViewById(R.id.tv_partner_wallet);
        tvEmpty = findViewById(R.id.tv_empty_delivery);
        tvTotalEarnings = findViewById(R.id.tv_total_earnings_value);
        btnLogout = findViewById(R.id.btn_delivery_logout);
        tabs = findViewById(R.id.tab_layout_delivery);
        rv = findViewById(R.id.rv_delivery_tasks);
        layoutEarnings = findViewById(R.id.layout_earnings_summary);
        btnWithdraw = findViewById(R.id.btn_partner_withdraw);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeliveryTaskAdapter(list, new DeliveryTaskAdapter.Listener() {
            @Override
            public void onAccept(DeliveryTask task) {
                if (repo.acceptDeliveryTask(task.getId(), partner.getId())) {
                    Toast.makeText(DeliveryDashboardActivity.this, "Accepted", Toast.LENGTH_SHORT).show();
                    load();
                }
            }

            @Override
            public void onPickedUp(DeliveryTask task) {
                if (repo.markPickedUp(task.getId(), partner.getId())) {
                    Toast.makeText(DeliveryDashboardActivity.this, "Marked picked up", Toast.LENGTH_SHORT).show();
                    notifyLocal("Your order has been picked up", task);
                    load();
                }
            }

            @Override
            public void onDelivered(DeliveryTask task, String code) {
                boolean ok = repo.completeDeliveryTask(task.getId(), partner.getId(), code);
                if (ok) {
                    Toast.makeText(DeliveryDashboardActivity.this,
                            "Delivered! Earning credited.", Toast.LENGTH_LONG).show();
                    notifyLocal("Your order has been delivered", task);
                    refreshHeader();
                    load();
                } else {
                    Toast.makeText(DeliveryDashboardActivity.this, "Incorrect code", Toast.LENGTH_SHORT).show();
                }
            }
        });
        rv.setAdapter(adapter);

        btnLogout.setOnClickListener(v -> {
            repo.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        btnWithdraw.setOnClickListener(v ->
                startActivity(new Intent(this, WithdrawCreditsActivity.class)));

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                tabIndex = tab.getPosition();
                load();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        refreshHeader();
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHeader();
        load();
    }

    private void refreshHeader() {
        partner = repo.getUserById(partner.getId());
        if (partner != null) {
            tvPartnerName.setText(partner.getName());
            tvPartnerWallet.setText("Wallet: " + Config.formatCurrency(partner.getWalletBalance()));
        }
    }

    private void load() {
        list.clear();

        if (tabIndex == 3) {
            rv.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
            layoutEarnings.setVisibility(View.VISIBLE);
            double e = repo.getPartnerTotalEarnings(partner.getId());
            tvTotalEarnings.setText(Config.formatCurrency(e));
            return;
        }
        layoutEarnings.setVisibility(View.GONE);

        if (tabIndex == 1) {
            // Active = ACCEPTED + PICKED_UP
            list.addAll(repo.getDeliveryTasksByStatus("ACCEPTED", partner.getId()));
            list.addAll(repo.getDeliveryTasksByStatus("PICKED_UP", partner.getId()));
        } else if (tabIndex == 2) {
            list.addAll(repo.getDeliveryTasksByStatus("DELIVERED", partner.getId()));
        } else {
            list.addAll(repo.getDeliveryTasksByStatus("AVAILABLE", partner.getId()));
        }

        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        rv.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // R2-11.6: simple in-app notification (buyer sees it too)
    private void notifyLocal(String title, DeliveryTask task) {
        NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (mgr == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    "px_delivery", "Delivery updates", NotificationManager.IMPORTANCE_DEFAULT);
            mgr.createNotificationChannel(ch);
        }
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, "px_delivery")
                .setSmallIcon(R.drawable.ic_check)
                .setContentTitle(title)
                .setContentText(task.getEnergyType() + " " + task.getQuantity() + " " + task.getUnit())
                .setAutoCancel(true);
        try {
            mgr.notify((int) System.currentTimeMillis(), b.build());
        } catch (Exception ignored) {}
    }
}