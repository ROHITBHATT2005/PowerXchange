package com.example.powerxchange2;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class TradeConfirmFragment extends Fragment {

    private static final String ARG_LISTING_ID = "arg_listing_id";

    private SQLiteRepository repo;
    private EnergyListing listing;
    private UserProfile currentUser;

    private TextView tvTradeSeller, tvTradeSource, tvTradeKwh, tvTradeEnergyPrice;
    private TextView tvTradeDistance, tvTradeDeliveryFee, tvTradePlatformFee, tvTradePartnerFee, tvTradeTotalPayable;
    private MaterialButton btnConfirmTradeSubmit;

    private double distanceKm = 5.0; // default estimated distance in km

    public static TradeConfirmFragment newInstance(EnergyListing listing) {
        TradeConfirmFragment fragment = new TradeConfirmFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_LISTING_ID, listing.getId());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repo = new SQLiteRepository(requireContext());
        if (getArguments() != null) {
            long listingId = getArguments().getLong(ARG_LISTING_ID);
            listing = repo.getListingById(listingId);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_trade_confirm, container, false);

        currentUser = repo.getLoggedInUser();

        tvTradeSeller = v.findViewById(R.id.tv_trade_seller);
        tvTradeSource = v.findViewById(R.id.tv_trade_source);
        tvTradeKwh = v.findViewById(R.id.tv_trade_kwh);
        tvTradeEnergyPrice = v.findViewById(R.id.tv_trade_energy_price);

        tvTradeDistance = v.findViewById(R.id.tv_trade_distance);
        tvTradeDeliveryFee = v.findViewById(R.id.tv_trade_delivery_fee);
        tvTradePlatformFee = v.findViewById(R.id.tv_trade_platform_fee);
        tvTradePartnerFee = v.findViewById(R.id.tv_trade_partner_fee);
        tvTradeTotalPayable = v.findViewById(R.id.tv_trade_total_payable);

        btnConfirmTradeSubmit = v.findViewById(R.id.btn_confirm_trade_submit);

        populateDetails();
        btnConfirmTradeSubmit.setOnClickListener(view -> finalizeTrade());

        return v;
    }

    private void populateDetails() {
        if (listing == null) return;

        tvTradeSeller.setText(listing.getSellerName());
        tvTradeSource.setText(listing.getEnergyType());
        tvTradeKwh.setText(String.format(Locale.US, "%.2f %s", listing.getQuantity(), listing.getUnit()));

        double energyPrice = listing.getTotalPrice(); // quantity * price_per_unit
        tvTradeEnergyPrice.setText(Config.formatCurrency(energyPrice));

        double deliveryFeeTotal = Config.BASE_DELIVERY_FEE + (Config.PER_KM_RATE * distanceKm);
        double platformCut = deliveryFeeTotal * Config.PLATFORM_CUT_RATIO;
        double partnerCut = deliveryFeeTotal * Config.PARTNER_CUT_RATIO;
        double totalPayable = energyPrice + deliveryFeeTotal;

        tvTradeDistance.setText(String.format(Locale.US, "%.1f km", distanceKm));
        tvTradeDeliveryFee.setText(Config.formatCurrency(deliveryFeeTotal));
        tvTradePlatformFee.setText(Config.formatCurrency(platformCut));
        tvTradePartnerFee.setText(Config.formatCurrency(partnerCut));
        tvTradeTotalPayable.setText(Config.formatCurrency(totalPayable));
    }

    private void finalizeTrade() {
        if (currentUser == null || listing == null) return;

        SQLiteRepository.TradeResult result = repo.executePurchase(currentUser, listing, distanceKm);
        if (result.success) {
            showDeliveryCodeDialog(result.deliveryCode);
        } else {
            Toast.makeText(getContext(), result.errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void showDeliveryCodeDialog(String code) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Order Confirmed! 6-Digit Delivery Code");
        builder.setMessage("Show this code to the delivery partner when your energy arrives:\n\n" + code);

        builder.setPositiveButton("Copy Code", (dialog, which) -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Delivery Code", code);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Code copied: " + code, Toast.LENGTH_SHORT).show();
            }
            finishCheckout();
        });

        builder.setNegativeButton("OK", (dialog, which) -> finishCheckout());
        builder.setCancelable(false);
        builder.show();
    }

    private void finishCheckout() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragment(new OrdersFragment(), "My Orders");
        }
    }
}