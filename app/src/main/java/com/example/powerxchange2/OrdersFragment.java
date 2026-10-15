package com.example.powerxchange2;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class OrdersFragment extends Fragment {

    private SQLiteRepository repo;
    private UserProfile currentUser;

    private TabLayout tabs;
    private RecyclerView rv;
    private TextView tvEmpty;

    private OrdersAdapter adapter;
    private final List<Trade> items = new ArrayList<>();
    private int tabIndex = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshTick = new Runnable() {
        @Override
        public void run() {
            loadOrders();
            handler.postDelayed(this, 5000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_orders, container, false);

        repo = new SQLiteRepository(requireContext());
        currentUser = repo.getLoggedInUser();

        tabs = v.findViewById(R.id.tab_layout_orders);
        rv = v.findViewById(R.id.rv_orders);
        tvEmpty = v.findViewById(R.id.tv_empty_orders);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        if (currentUser != null && currentUser.isProsumer()) {
            tabs.setVisibility(View.VISIBLE);
            tabs.addTab(tabs.newTab().setText("My Orders"));
            tabs.addTab(tabs.newTab().setText("Sales"));
            tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(TabLayout.Tab tab) {
                    tabIndex = tab.getPosition();
                    loadOrders();
                }
                @Override public void onTabUnselected(TabLayout.Tab tab) {}
                @Override public void onTabReselected(TabLayout.Tab tab) {}
            });
        } else {
            tabs.setVisibility(View.GONE);
        }

        adapter = new OrdersAdapter(items, true, currentUser != null ? currentUser.getId() : 0, repo);
        rv.setAdapter(adapter);

        loadOrders();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.post(refreshTick);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshTick);
    }

    private void loadOrders() {
        currentUser = repo.getLoggedInUser();
        if (currentUser == null) return;

        items.clear();
        if (tabIndex == 1 && currentUser.isProsumer()) {
            items.addAll(repo.getActiveTradesBySeller(currentUser.getId()));
        } else {
            // Buyer's purchases
            items.addAll(repo.getActiveTradesByBuyer(currentUser.getId()));
            // R2-11.5: barter legs where I'm sender OR receiver
            items.addAll(repo.getActiveBarterTradesForUser(currentUser.getId()));

            // dedupe by trade id (a barter leg where I'm buyer appears twice otherwise)
            LinkedHashMap<Long, Trade> map = new LinkedHashMap<>();
            for (Trade t : items) map.put(t.getId(), t);
            items.clear();
            items.addAll(map.values());
        }

        adapter.notifyDataSetChanged();

        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        rv.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
    }
}