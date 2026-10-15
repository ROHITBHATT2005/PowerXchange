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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SwapRequestFragment extends Fragment {

    private SQLiteRepository repo;
    private UserProfile currentUser;

    private RecyclerView rvSwaps;
    private TextView tvEmptySwaps;
    private ExtendedFloatingActionButton fabProposeSwap;

    private SwapRequestAdapter adapter;
    private final List<BarterOffer> barterList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_swap_request, container, false);

        repo = new SQLiteRepository(requireContext());
        currentUser = repo.getLoggedInUser();

        rvSwaps = v.findViewById(R.id.rv_swaps);
        tvEmptySwaps = v.findViewById(R.id.tv_empty_swaps);
        fabProposeSwap = v.findViewById(R.id.fab_propose_swap);

        if (currentUser != null && currentUser.isProsumer()) {
            fabProposeSwap.setVisibility(View.VISIBLE);
        } else {
            fabProposeSwap.setVisibility(View.GONE);
        }

        rvSwaps.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SwapRequestAdapter(barterList, currentUser, barter -> {
            // R2-11.1: accept barter and create two delivery legs
            SQLiteRepository.BarterAcceptResult r =
                    repo.acceptBarterAndCreateLegs(currentUser, barter.getId());
            if (r.success) {
                Toast.makeText(getContext(),
                        "Barter accepted — 2 deliveries created", Toast.LENGTH_LONG).show();
                loadSwaps();
            } else {
                Toast.makeText(getContext(), r.errorMessage, Toast.LENGTH_LONG).show();
            }
        });
        rvSwaps.setAdapter(adapter);

        fabProposeSwap.setOnClickListener(view -> showProposeSwapDialog());
        loadSwaps();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        currentUser = repo.getLoggedInUser();
        loadSwaps();
    }

    private void loadSwaps() {
        barterList.clear();
        currentUser = repo.getLoggedInUser();

        if (currentUser != null && currentUser.isProsumer()) {
            barterList.addAll(repo.getAllOpenBarters());
        }

        adapter = new SwapRequestAdapter(barterList, currentUser, barter -> {
            SQLiteRepository.BarterAcceptResult r =
                    repo.acceptBarterAndCreateLegs(currentUser, barter.getId());
            if (r.success) {
                Toast.makeText(getContext(),
                        "Barter accepted — 2 deliveries created", Toast.LENGTH_LONG).show();
                loadSwaps();
            } else {
                Toast.makeText(getContext(), r.errorMessage, Toast.LENGTH_LONG).show();
            }
        });
        rvSwaps.setAdapter(adapter);

        if (barterList.isEmpty()) {
            tvEmptySwaps.setVisibility(View.VISIBLE);
            rvSwaps.setVisibility(View.GONE);
        } else {
            tvEmptySwaps.setVisibility(View.GONE);
            rvSwaps.setVisibility(View.VISIBLE);
        }
    }

    private void showProposeSwapDialog() {
        if (currentUser == null || !currentUser.isProsumer()) {
            Toast.makeText(getContext(), "Only PROSUMERS can create barter offers.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_swap, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        Spinner spinnerGive = dialogView.findViewById(R.id.spinner_give_type);
        Spinner spinnerWant = dialogView.findViewById(R.id.spinner_want_type);

        TextInputLayout tilGiveQty = dialogView.findViewById(R.id.til_give_qty);
        TextInputLayout tilWantQty = dialogView.findViewById(R.id.til_want_qty);

        TextInputEditText etOfferQty = dialogView.findViewById(R.id.et_offer_kwh);
        TextInputEditText etRequestQty = dialogView.findViewById(R.id.et_request_kwh);
        TextInputEditText etNote = dialogView.findViewById(R.id.et_time_slot);

        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel_swap);
        MaterialButton btnSubmit = dialogView.findViewById(R.id.btn_submit_swap);

        String[] energyTypes = {"Solar", "Wind", "Battery", "Biogas"};
        ArrayAdapter<String> adapterGive = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, energyTypes);
        ArrayAdapter<String> adapterWant = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, energyTypes);

        spinnerGive.setAdapter(adapterGive);
        spinnerWant.setAdapter(adapterWant);
        spinnerGive.setSelection(0);
        spinnerWant.setSelection(3);

        spinnerGive.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String unit = Config.getUnitForEnergyType(energyTypes[position]);
                tilGiveQty.setHint("Offered Quantity (" + unit + ")");
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerWant.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String unit = Config.getUnitForEnergyType(energyTypes[position]);
                tilWantQty.setHint("Requested Quantity (" + unit + ")");
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            String giveType = (String) spinnerGive.getSelectedItem();
            String wantType = (String) spinnerWant.getSelectedItem();
            String giveQtyStr = etOfferQty.getText() != null ? etOfferQty.getText().toString().trim() : "";
            String wantQtyStr = etRequestQty.getText() != null ? etRequestQty.getText().toString().trim() : "";
            String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";

            if (TextUtils.isEmpty(giveQtyStr) || TextUtils.isEmpty(wantQtyStr)) {
                Toast.makeText(getContext(), "Please specify both quantities", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double giveQty = Double.parseDouble(giveQtyStr);
                double wantQty = Double.parseDouble(wantQtyStr);
                if (giveQty <= 0 || wantQty <= 0) {
                    Toast.makeText(getContext(), "Quantities must be > 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean success = repo.createBarterOffer(currentUser, giveType, giveQty,
                        wantType, wantQty, note, currentUser.getCityLocation());
                if (success) {
                    Toast.makeText(getContext(), "Barter proposed", Toast.LENGTH_SHORT).show();
                    loadSwaps();
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "Failed to propose barter", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid numbers", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}