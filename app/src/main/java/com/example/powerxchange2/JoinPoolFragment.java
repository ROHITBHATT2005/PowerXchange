package com.example.powerxchange2;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class JoinPoolFragment extends Fragment {

    private SQLiteRepository repo;
    private UserProfile currentUser;

    private TabLayout tabLayout;
    private RecyclerView rvPools;
    private TextView tvEmptyPools;

    private PoolsAdapter adapter;
    private final List<EnergyPool> displayList = new ArrayList<>();
    private int tabIndex = 0; // 0 = Open, 1 = My Pools

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_join_pool, container, false);

        repo = new SQLiteRepository(requireContext());
        currentUser = repo.getLoggedInUser();

        tabLayout = v.findViewById(R.id.tab_layout_pools);
        rvPools = v.findViewById(R.id.rv_pools);
        tvEmptyPools = v.findViewById(R.id.tv_empty_pools);

        rvPools.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PoolsAdapter(displayList, currentUser, new PoolsAdapter.Listener() {
            @Override public void onJoinPool(EnergyPool pool) { showJoinDialog(pool); }
            @Override public void onClosePool(EnergyPool pool) { confirmClosePool(pool); }
        });
        rvPools.setAdapter(adapter);

        setupTabs();
        loadPools();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        repo.expireStalePools();
        currentUser = repo.getLoggedInUser();
        loadPools();
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                tabIndex = tab.getPosition();
                loadPools();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadPools() {
        displayList.clear();
        currentUser = repo.getLoggedInUser();

        // R2-2.1: delivery partners do not use pools
        if (currentUser == null || currentUser.isDelivery()) {
            adapter.notifyDataSetChanged();
            tvEmptyPools.setVisibility(View.VISIBLE);
            rvPools.setVisibility(View.GONE);
            return;
        }

        if (tabIndex == 0) {
            displayList.addAll(repo.getAllOpenPools());
        } else {
            displayList.addAll(repo.getMyPools(currentUser.getId()));
        }
        adapter.notifyDataSetChanged();

        if (displayList.isEmpty()) {
            tvEmptyPools.setVisibility(View.VISIBLE);
            rvPools.setVisibility(View.GONE);
        } else {
            tvEmptyPools.setVisibility(View.GONE);
            rvPools.setVisibility(View.VISIBLE);
        }
    }

    private void showJoinDialog(EnergyPool pool) {
        if (currentUser == null) return;
        if (currentUser.getId() == pool.getCreatorId()) {
            Toast.makeText(getContext(), "You created this pool.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (repo.hasUserJoinedPool(pool.getId(), currentUser.getId())) {
            Toast.makeText(getContext(), "You have already joined this pool.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder b = new AlertDialog.Builder(requireContext());
        View dv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_join_pool, null);
        b.setView(dv);
        AlertDialog dialog = b.create();

        TextInputLayout tilQty = dv.findViewById(R.id.til_join_qty);
        TextInputEditText etQty = dv.findViewById(R.id.et_join_qty);
        TextView tvInfo = dv.findViewById(R.id.tv_join_info);
        TextView tvPay = dv.findViewById(R.id.tv_join_pay);
        MaterialButton btnCancel = dv.findViewById(R.id.btn_join_cancel);
        MaterialButton btnJoin = dv.findViewById(R.id.btn_join_confirm);

        final double remaining = pool.getRemainingQty();
        final double price = pool.getPricePerUnit();
        final String unit = pool.getUnit();

        tilQty.setHint("Quantity (" + unit + ")");
        tvInfo.setText(String.format(Locale.US,
                "Remaining: %.2f %s\nPrice: %s",
                remaining, unit, Config.formatPricePerUnit(price, pool.getEnergyType())));

        etQty.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b2, int c) {}
            public void onTextChanged(CharSequence s, int a, int b2, int c) {}
            public void afterTextChanged(android.text.Editable s) {
                try {
                    double q = Double.parseDouble(s.toString());
                    tvPay.setText(String.format(Locale.US,
                            "You will pay %s for %.2f %s (delivery fee added at checkout).",
                            Config.formatCurrency(q * price), q, unit));
                } catch (Exception e) {
                    tvPay.setText("You will pay ₹0.00");
                }
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnJoin.setOnClickListener(v -> {
            String s = etQty.getText() != null ? etQty.getText().toString().trim() : "";
            if (TextUtils.isEmpty(s)) {
                Toast.makeText(getContext(), "Enter quantity", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                double q = Double.parseDouble(s);
                if (q <= 0 || q > remaining + 0.0001) {
                    Toast.makeText(getContext(),
                            String.format(Locale.US, "Enter between 0 and %.2f %s", remaining, unit),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean ok = repo.joinPool(currentUser, pool.getId(), q);
                if (ok) {
                    Toast.makeText(getContext(), "Joined pool!", Toast.LENGTH_LONG).show();
                    loadPools();
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "Failed. Check balance or pool status.", Toast.LENGTH_LONG).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid quantity", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void confirmClosePool(EnergyPool pool) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Close pool early?")
                .setMessage("The remaining reserved quantity will be released back to the marketplace. " +
                        "Existing members keep their shares.")
                .setPositiveButton("Close Pool", (d, w) -> {
                    boolean ok = repo.closePoolEarly(currentUser, pool.getId());
                    if (ok) {
                        Toast.makeText(getContext(), "Pool closed", Toast.LENGTH_SHORT).show();
                        loadPools();
                    } else {
                        Toast.makeText(getContext(), "Failed to close", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}