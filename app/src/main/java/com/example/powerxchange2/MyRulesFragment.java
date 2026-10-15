package com.example.powerxchange2;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MyRulesFragment extends Fragment {

    private SQLiteRepository repo;
    private UserProfile currentUser;

    private RecyclerView rvRules, rvHistory;
    private TextView tvEmptyRules, tvEmptyHistory;

    private AutoRulesAdapter rulesAdapter;
    private AutoBuyHistoryAdapter historyAdapter;
    private final List<AutoTradeRule> rules = new ArrayList<>();
    private final List<AutoBuyHistory> history = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_my_rules, container, false);

        repo = new SQLiteRepository(requireContext());
        currentUser = repo.getLoggedInUser();

        rvRules = v.findViewById(R.id.rv_auto_rules);
        rvHistory = v.findViewById(R.id.rv_auto_history);
        tvEmptyRules = v.findViewById(R.id.tv_empty_rules);
        tvEmptyHistory = v.findViewById(R.id.tv_empty_history);

        rvRules.setLayoutManager(new LinearLayoutManager(getContext()));
        rulesAdapter = new AutoRulesAdapter(rules, new AutoRulesAdapter.Listener() {
            @Override public void onToggle(AutoTradeRule rule, boolean active) {
                rule.setActive(active);
                repo.saveAutoRule(rule);
            }
            @Override public void onDelete(AutoTradeRule rule) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete rule?")
                        .setMessage("Remove this auto-trade rule?")
                        .setPositiveButton("Delete", (d, w) -> {
                            repo.deleteAutoRule(rule.getId());
                            loadData();
                        })
                        .setNegativeButton("Cancel", null).show();
            }
        });
        rvRules.setAdapter(rulesAdapter);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        historyAdapter = new AutoBuyHistoryAdapter(history);
        rvHistory.setAdapter(historyAdapter);

        MaterialButton btnAdd = v.findViewById(R.id.btn_add_rule);
        MaterialButton btnSchedule = v.findViewById(R.id.btn_schedule_worker);
        btnAdd.setOnClickListener(x -> showAddRuleDialog());
        btnSchedule.setOnClickListener(x -> scheduleWorker());

        loadData();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        currentUser = repo.getLoggedInUser();
        rules.clear();
        history.clear();
        if (currentUser != null) {
            rules.addAll(repo.getAutoRules(currentUser.getId()));
            history.addAll(repo.getAutoBuyHistory(currentUser.getId()));
        }
        rulesAdapter.notifyDataSetChanged();
        historyAdapter.notifyDataSetChanged();

        tvEmptyRules.setVisibility(rules.isEmpty() ? View.VISIBLE : View.GONE);
        rvRules.setVisibility(rules.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmptyHistory.setVisibility(history.isEmpty() ? View.VISIBLE : View.GONE);
        rvHistory.setVisibility(history.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showAddRuleDialog() {
        if (currentUser == null) return;
        if (currentUser.isDelivery()) {
            Toast.makeText(getContext(), "Delivery partners cannot set auto-buy rules.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder b = new AlertDialog.Builder(requireContext());
        View dv = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_auto_rule, null);
        b.setView(dv);
        AlertDialog dialog = b.create();

        Spinner spinner = dv.findViewById(R.id.spinner_rule_type);
        TextInputLayout tilPrice = dv.findViewById(R.id.til_rule_price);
        TextInputLayout tilQty = dv.findViewById(R.id.til_rule_qty);
        TextInputEditText etPrice = dv.findViewById(R.id.et_rule_price);
        TextInputEditText etQty = dv.findViewById(R.id.et_rule_qty);
        MaterialSwitch swActive = dv.findViewById(R.id.switch_new_rule_active);
        MaterialButton btnCancel = dv.findViewById(R.id.btn_rule_cancel);
        MaterialButton btnSave = dv.findViewById(R.id.btn_rule_save);

        String[] types = {"Solar", "Wind", "Battery", "Biogas"};
        ArrayAdapter<String> sp = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, types);
        spinner.setAdapter(sp);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View view, int pos, long id) {
                String type = types[pos];
                String unit = Config.getUnitForEnergyType(type);
                tilPrice.setHint("Max price (₹/" + unit + ")");
                tilQty.setHint("Max quantity (" + unit + ")");
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        btnCancel.setOnClickListener(x -> dialog.dismiss());
        btnSave.setOnClickListener(x -> {
            String type = (String) spinner.getSelectedItem();
            String ps = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
            String qs = etQty.getText() != null ? etQty.getText().toString().trim() : "";
            if (TextUtils.isEmpty(ps) || TextUtils.isEmpty(qs)) {
                Toast.makeText(getContext(), "Fill both fields", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                double price = Double.parseDouble(ps);
                double qty = Double.parseDouble(qs);
                if (price <= 0 || qty <= 0) {
                    Toast.makeText(getContext(), "Values must be > 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                AutoTradeRule r = new AutoTradeRule(0, currentUser.getId(), type,
                        price, qty, swActive.isChecked(), null);
                repo.saveAutoRule(r);
                Toast.makeText(getContext(), "Rule added", Toast.LENGTH_SHORT).show();
                loadData();
                dialog.dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid numbers", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void scheduleWorker() {
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(
                AutoTradeWorker.class, 15, TimeUnit.MINUTES).build();
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "PowerXchangeAutoTradeWork",
                ExistingPeriodicWorkPolicy.UPDATE,
                req);
        Toast.makeText(getContext(), "Background auto-trade enabled (every 15 min).", Toast.LENGTH_SHORT).show();
    }
}