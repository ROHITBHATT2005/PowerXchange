package com.example.powerxchange2;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private SQLiteRepository repo;
    private UserProfile currentUser;
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repo = new SQLiteRepository(this);
        currentUser = repo.getLoggedInUser();
        if (currentUser == null) { finish(); return; }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // R2-6: header + bottom nav insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            toolbar.setPadding(toolbar.getPaddingLeft(), top, toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            bottomNavigationView.setPadding(bottomNavigationView.getPaddingLeft(),
                    bottomNavigationView.getPaddingTop(),
                    bottomNavigationView.getPaddingRight(), bottom);
            return insets;
        });

        configureMenuForRole();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_market) { loadFragment(new MarketplaceFragment(), "Marketplace"); return true; }
            else if (id == R.id.nav_pool) { loadFragment(new JoinPoolFragment(), "Community Pools"); return true; }
            else if (id == R.id.nav_rules) { loadFragment(new MyRulesFragment(), "Auto-Buy Rules"); return true; }
            else if (id == R.id.nav_swap) { loadFragment(new SwapRequestFragment(), "Barter Exchange"); return true; }
            else if (id == R.id.nav_orders) { loadFragment(new OrdersFragment(), "Orders"); return true; }
            else if (id == R.id.nav_profile) { loadFragment(new ProfileFragment(), "Profile & Wallet"); return true; }
            return false;
        });

        if (savedInstanceState == null) bottomNavigationView.setSelectedItemId(R.id.nav_market);
    }

    private void configureMenuForRole() {
        Menu menu = bottomNavigationView.getMenu();
        MenuItem itemRules = menu.findItem(R.id.nav_rules);
        MenuItem itemSwap = menu.findItem(R.id.nav_swap);
        MenuItem itemOrders = menu.findItem(R.id.nav_orders);
        MenuItem itemPool = menu.findItem(R.id.nav_pool);

        if (currentUser.isProsumer()) {
            if (itemRules != null) itemRules.setVisible(true);
            if (itemOrders != null) itemOrders.setVisible(true);
            if (itemSwap != null) itemSwap.setVisible(true);
            if (itemPool != null) itemPool.setVisible(true);
        } else {
            if (itemSwap != null) itemSwap.setVisible(false);
            if (itemRules != null) itemRules.setVisible(true);
            if (itemOrders != null) itemOrders.setVisible(true);
            if (itemPool != null) itemPool.setVisible(true);
        }
    }

    public void loadFragment(Fragment fragment, String title) {
        if (toolbar != null && title != null) toolbar.setTitle(title);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.commit();
    }

    public void openTradeConfirm(EnergyListing listing) {
        if (toolbar != null) toolbar.setTitle("Checkout & Pay");
        TradeConfirmFragment f = TradeConfirmFragment.newInstance(listing);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, f);
        ft.addToBackStack(null);
        ft.commit();
    }
}