package com.example.powerxchange2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private SQLiteRepository repo;

    private TabLayout tabLayoutAuth;
    private LinearLayout layoutLoginForm, layoutSignupForm, layoutUserTypeContainer, layoutCompanySpinnerContainer;

    // Login inputs
    private TextInputEditText etLoginEmail, etLoginPassword;
    private MaterialButton btnLoginSubmit;

    // Signup inputs
    private TextInputEditText etSignupName, etSignupEmail, etSignupPhone, etSignupCity, etSignupPassword;
    private RadioGroup rgRole, rgUserType;
    private RadioButton rbProsumer, rbConsumer, rbDelivery, rbIndividual, rbCorporate;
    private Spinner spinnerCompany;
    private MaterialButton btnSignupSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        repo = new SQLiteRepository(this);

        // Auto login check & routing
        if (repo.isLoggedIn()) {
            UserProfile user = repo.getLoggedInUser();
            if (user != null) {
                routeUserByRole(user);
                return;
            }
        }

        setContentView(R.layout.activity_login);

        tabLayoutAuth = findViewById(R.id.tab_layout_auth);
        layoutLoginForm = findViewById(R.id.layout_login_form);
        layoutSignupForm = findViewById(R.id.layout_signup_form);
        layoutUserTypeContainer = findViewById(R.id.layout_user_type_container);
        layoutCompanySpinnerContainer = findViewById(R.id.layout_company_spinner_container);

        etLoginEmail = findViewById(R.id.et_login_email);
        etLoginPassword = findViewById(R.id.et_login_password);
        btnLoginSubmit = findViewById(R.id.btn_login_submit);

        etSignupName = findViewById(R.id.et_signup_name);
        etSignupEmail = findViewById(R.id.et_signup_email);
        etSignupPhone = findViewById(R.id.et_signup_phone);
        etSignupCity = findViewById(R.id.et_signup_city);
        etSignupPassword = findViewById(R.id.et_signup_password);

        rgRole = findViewById(R.id.rg_role);
        rgUserType = findViewById(R.id.rg_user_type);
        rbProsumer = findViewById(R.id.rb_prosumer);
        rbConsumer = findViewById(R.id.rb_consumer);
        rbDelivery = findViewById(R.id.rb_delivery);
        rbIndividual = findViewById(R.id.rb_individual);
        rbCorporate = findViewById(R.id.rb_corporate);
        spinnerCompany = findViewById(R.id.spinner_company);
        btnSignupSubmit = findViewById(R.id.btn_signup_submit);

        setupCompanySpinner();
        setupTabs();
        setupListeners();
    }

    private void setupCompanySpinner() {
        ArrayAdapter<Config.CompanyInfo> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, Config.INDIAN_COMPANIES);
        spinnerCompany.setAdapter(adapter);
    }

    private void setupTabs() {
        tabLayoutAuth.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    layoutLoginForm.setVisibility(View.VISIBLE);
                    layoutSignupForm.setVisibility(View.GONE);
                } else {
                    layoutLoginForm.setVisibility(View.GONE);
                    layoutSignupForm.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupListeners() {
        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_prosumer) {
                layoutUserTypeContainer.setVisibility(View.VISIBLE);
            } else {
                layoutUserTypeContainer.setVisibility(View.GONE);
            }
        });

        rgUserType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_corporate) {
                layoutCompanySpinnerContainer.setVisibility(View.VISIBLE);
            } else {
                layoutCompanySpinnerContainer.setVisibility(View.GONE);
            }
        });

        btnLoginSubmit.setOnClickListener(v -> handleLogin());
        btnSignupSubmit.setOnClickListener(v -> handleSignup());
    }

    private void handleLogin() {
        String email = etLoginEmail.getText() != null ? etLoginEmail.getText().toString().trim() : "";
        String password = etLoginPassword.getText() != null ? etLoginPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            etLoginEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etLoginPassword.setError("Password is required");
            return;
        }

        UserProfile user = repo.loginUser(email, password);
        if (user != null) {
            repo.saveUserSession(user);
            Toast.makeText(this, "Welcome back, " + user.getName() + "!", Toast.LENGTH_SHORT).show();
            routeUserByRole(user);
        } else {
            Toast.makeText(this, "Invalid credentials.", Toast.LENGTH_LONG).show();
        }
    }

    private void handleSignup() {
        String name = etSignupName.getText() != null ? etSignupName.getText().toString().trim() : "";
        String email = etSignupEmail.getText() != null ? etSignupEmail.getText().toString().trim() : "";
        String phone = etSignupPhone.getText() != null ? etSignupPhone.getText().toString().trim() : "";
        String city = etSignupCity.getText() != null ? etSignupCity.getText().toString().trim() : "Mumbai";
        String password = etSignupPassword.getText() != null ? etSignupPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            etSignupName.setError("Full name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etSignupEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etSignupPassword.setError("Password is required");
            return;
        }

        String role = "CONSUMER";
        if (rbProsumer.isChecked()) {
            role = "PROSUMER";
        } else if (rbDelivery.isChecked()) {
            role = "DELIVERY";
        }

        String userType = "INDIVIDUAL";
        String companyName = "";

        if ("PROSUMER".equalsIgnoreCase(role) && rbCorporate.isChecked()) {
            userType = "CORPORATE";
            Config.CompanyInfo comp = (Config.CompanyInfo) spinnerCompany.getSelectedItem();
            if (comp != null) {
                companyName = comp.name;
                city = comp.location;
            }
        }

        boolean success = repo.registerUser(name, email, phone, password, role, userType, companyName, city);
        if (success) {
            UserProfile user = repo.loginUser(email, password);
            if (user != null) {
                repo.saveUserSession(user);
                Toast.makeText(this, "Signed up successfully", Toast.LENGTH_SHORT).show();
                routeUserByRole(user);
            }
        }
        else{
            Toast.makeText(this, "Failed to register. Email already exists.", Toast.LENGTH_LONG).show();
        }
    }

    private void routeUserByRole(UserProfile user) {
        Intent intent;
        if (user.isDelivery()) {
            intent = new Intent(this, DeliveryDashboardActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        startActivity(intent);
        finish();
    }
}