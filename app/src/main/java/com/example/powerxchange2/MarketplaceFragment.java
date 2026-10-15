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
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MarketplaceFragment extends Fragment {

    private SQLiteRepository repo;
    private UserProfile currentUser;

    private RecyclerView rvListings;
    private TextView tvEmptyListings, tvMarketplaceWallet;
    private ChipGroup chipGroupFilter;
    private ExtendedFloatingActionButton fabPublishListing;

    private EnergyListingAdapter adapter;
    private final List<EnergyListing> listingList = new ArrayList<>();
    private String energyFilter = "ALL";
    private String sellerFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_marketplace, container, false);

        repo = new SQLiteRepository(requireContext());
        currentUser = repo.getLoggedInUser();

        rvListings = v.findViewById(R.id.rv_listings);
        tvEmptyListings = v.findViewById(R.id.tv_empty_listings);
        tvMarketplaceWallet = v.findViewById(R.id.tv_marketplace_wallet);
        chipGroupFilter = v.findViewById(R.id.chip_group_filter);
        fabPublishListing = v.findViewById(R.id.fab_publish_listing);

        if (currentUser != null && currentUser.isProsumer()) {
            fabPublishListing.setVisibility(View.VISIBLE);
        } else {
            fabPublishListing.setVisibility(View.GONE);
        }

        rvListings.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EnergyListingAdapter(listingList, currentUser, new EnergyListingAdapter.Listener() {
            @Override public void onBuyClick(EnergyListing listing) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openTradeConfirm(listing);
                }
            }
            @Override public void onCreatePoolClick(EnergyListing listing) {
                showCreatePoolDialog(listing);
            }
        });
        rvListings.setAdapter(adapter);

        setupChipListeners();
        fabPublishListing.setOnClickListener(view -> showAddListingDialog());

        refreshWalletHeader();
        loadListings();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        repo.expireStalePools();
        refreshWalletHeader();
        loadListings();
    }

    private void refreshWalletHeader() {
        currentUser = repo.getLoggedInUser();
        if (currentUser != null && tvMarketplaceWallet != null) {
            tvMarketplaceWallet.setText("Wallet: " + Config.formatCurrency(currentUser.getWalletBalance()));
        }
    }

    private void setupChipListeners() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            energyFilter = "ALL";
            sellerFilter = "ALL";
            if (!checkedIds.isEmpty()) {
                int id = checkedIds.get(0);
                if (id == R.id.chip_solar) energyFilter = "Solar";
                else if (id == R.id.chip_wind) energyFilter = "Wind";
                else if (id == R.id.chip_battery) energyFilter = "Battery";
                else if (id == R.id.chip_biogas) energyFilter = "Biogas";
                else if (id == R.id.chip_corporate) sellerFilter = "CORPORATE";
            }
            loadListings();
        });
    }

    private void loadListings() {
        listingList.clear();
        listingList.addAll(repo.getAllListings(energyFilter, sellerFilter));
        adapter.notifyDataSetChanged();

        if (listingList.isEmpty()) {
            tvEmptyListings.setVisibility(View.VISIBLE);
            rvListings.setVisibility(View.GONE);
        } else {
            tvEmptyListings.setVisibility(View.GONE);
            rvListings.setVisibility(View.VISIBLE);
        }
    }

    // ---------- Publish listing (Prosumer only) ----------
    private void showAddListingDialog() {
        if (currentUser == null || !currentUser.isProsumer()) {
            Toast.makeText(getContext(), "Only PROSUMERS can create listings.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder b = new AlertDialog.Builder(requireContext());
        View dv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_listing, null);
        b.setView(dv);
        AlertDialog dialog = b.create();

        Spinner spinnerType = dv.findViewById(R.id.spinner_energy_type);
        TextInputLayout tilQuantity = dv.findViewById(R.id.til_quantity);
        TextInputLayout tilPrice = dv.findViewById(R.id.til_price);
        TextInputEditText etQty = dv.findViewById(R.id.et_kwh);
        TextInputEditText etPrice = dv.findViewById(R.id.et_price);
        TextInputEditText etLocation = dv.findViewById(R.id.et_location);
        TextView tvMarketHint = dv.findViewById(R.id.tv_market_hint);
        MaterialButton btnCancel = dv.findViewById(R.id.btn_cancel_listing);
        MaterialButton btnSubmit = dv.findViewById(R.id.btn_submit_listing);

        if (currentUser.getCityLocation() != null && !currentUser.getCityLocation().isEmpty())
            etLocation.setText(currentUser.getCityLocation());

        String[] types = {"Solar", "Wind", "Battery", "Biogas"};
        ArrayAdapter<String> sp = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, types);
        spinnerType.setAdapter(sp);

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View view, int pos, long id) {
                String type = types[pos];
                String unit = Config.getUnitForEnergyType(type);
                double market = Config.getMarketPrice(type);
                tilQuantity.setHint("Quantity (" + unit + ")");
                tilPrice.setHint("Price per " + unit + " (₹)");
                // R2-7.3: informational market price hint
                if (tvMarketHint != null) {
                    tvMarketHint.setText(String.format(Locale.US,
                            "Market price: ₹%.2f/%s (informational only)", market, unit));
                }
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            String type = (String) spinnerType.getSelectedItem();
            String qtyStr = etQty.getText() != null ? etQty.getText().toString().trim() : "";
            String priceStr = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
            String loc = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";

            if (TextUtils.isEmpty(qtyStr) || TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(loc)) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                double qty = Double.parseDouble(qtyStr);
                double price = Double.parseDouble(priceStr);
                if (qty <= 0 || price <= 0) {
                    Toast.makeText(getContext(), "Quantity and price must be > 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Round to 2 decimals (R2-7.2)
                price = Math.round(price * 100.0) / 100.0;

                boolean ok = repo.addListing(currentUser, type, qty, price, loc);
                if (ok) {
                    Toast.makeText(getContext(), "Listing published", Toast.LENGTH_SHORT).show();
                    loadListings();
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "Failed to publish", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid numbers", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    // ---------- R2-2: Create Pool dialog ----------
    private void showCreatePoolDialog(EnergyListing listing) {
        if (currentUser == null) return;
        if (currentUser.isDelivery()) {
            Toast.makeText(getContext(), "Delivery partners cannot pool.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser.getId() == listing.getSellerId()) return;

        AlertDialog.Builder b = new AlertDialog.Builder(requireContext());
        View dv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_pool, null);
        b.setView(dv);
        AlertDialog dialog = b.create();

        TextView tvInfo = dv.findViewById(R.id.tv_pool_info);
        TextView tvMarket = dv.findViewById(R.id.tv_pool_market);
        TextInputEditText etMyQty = dv.findViewById(R.id.et_my_qty);
        TextInputLayout tilMyQty = dv.findViewById(R.id.til_my_qty);
        TextView tvPay = dv.findViewById(R.id.tv_pool_pay);
        MaterialButton btnCancel = dv.findViewById(R.id.btn_pool_cancel);
        MaterialButton btnCreate = dv.findViewById(R.id.btn_pool_create);

        String unit = listing.getUnit();
        tilMyQty.setHint("Your share (" + unit + ")");
        tvInfo.setText(String.format(Locale.US,
                "Available: %.2f %s\nPrice: %s",
                listing.getQuantity(), unit,
                Config.formatPricePerUnit(listing.getPricePerUnit(), listing.getEnergyType())));
        tvMarket.setText(String.format(Locale.US,
                "Remaining %.2f %s will be reserved for other pool members.",
                listing.getQuantity(), unit));

        final double[] lastPayable = {0};
        etMyQty.setOnFocusChangeListener((v, hasFocus) -> {});
        etMyQty.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b2, int c) {}
            public void onTextChanged(CharSequence s, int a, int b2, int c) {}
            public void afterTextChanged(android.text.Editable s) {
                try {
                    double q = Double.parseDouble(s.toString());
                    double pay = q * listing.getPricePerUnit();
                    lastPayable[0] = pay;
                    tvPay.setText(String.format(Locale.US,
                            "You will pay %s for %.2f %s (delivery fee added at checkout).",
                            Config.formatCurrency(pay), q, unit));
                } catch (Exception e) {
                    tvPay.setText("You will pay ₹0.00");
                }
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnCreate.setOnClickListener(v -> {
            String s = etMyQty.getText() != null ? etMyQty.getText().toString().trim() : "";
            if (TextUtils.isEmpty(s)) { Toast.makeText(getContext(), "Enter your share", Toast.LENGTH_SHORT).show(); return; }
            try {
                double qty = Double.parseDouble(s);
                if (qty <= 0 || qty > listing.getQuantity() + 0.0001) {
                    Toast.makeText(getContext(),
                            String.format(Locale.US, "Enter between 0 and %.2f %s", listing.getQuantity(), unit),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean ok = repo.createPool(currentUser, listing, qty);
                if (ok) {
                    Toast.makeText(getContext(), "Pool created!", Toast.LENGTH_LONG).show();
                    refreshWalletHeader();
                    loadListings();
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "Failed. Check wallet balance.", Toast.LENGTH_LONG).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid quantity", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}