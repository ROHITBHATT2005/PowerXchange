package com.example.powerxchange2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;
import java.util.Locale;

public class AutoRulesAdapter extends RecyclerView.Adapter<AutoRulesAdapter.VH> {

    public interface Listener{
        void onToggle(AutoTradeRule rule, boolean active);
        void onDelete(AutoTradeRule rule);
    }

    private final List<AutoTradeRule> rules;
    private final Listener listener;

    public AutoRulesAdapter(List<AutoTradeRule> rules, Listener l) {
        this.rules = rules; this.listener = l;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_auto_rule, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AutoTradeRule r = rules.get(position);
        String unit = Config.getUnitForEnergyType(r.getEnergyType());
        h.tvRuleTitle.setText(String.format(Locale.US, "%s — max ₹%.2f/%s",
                r.getEnergyType(), r.getMaxPrice(), unit));
        h.tvRuleSubtitle.setText(String.format(Locale.US, "Max quantity: %.2f %s", r.getMaxQty(), unit));

        h.switchActive.setOnCheckedChangeListener(null);
        h.switchActive.setChecked(r.isActive());
        h.switchActive.setOnCheckedChangeListener((btn, checked) -> {
            if (listener != null) listener.onToggle(r, checked);
        });

        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(r);
        });
    }

    @Override
    public int getItemCount() { return rules.size(); }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvRuleTitle, tvRuleSubtitle;
        MaterialSwitch switchActive;
        ImageButton btnDelete;

        public VH(@NonNull View v) {
            super(v);
            tvRuleTitle = v.findViewById(R.id.tv_rule_title);
            tvRuleSubtitle = v.findViewById(R.id.tv_rule_subtitle);
            switchActive = v.findViewById(R.id.switch_rule_active);
            btnDelete = v.findViewById(R.id.btn_rule_delete);
        }
    }
}