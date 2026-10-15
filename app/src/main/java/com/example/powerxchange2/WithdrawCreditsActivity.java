package com.example.powerxchange2;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class WithdrawCreditsActivity extends AppCompatActivity {

    private SQLiteRepository repo;
    private WalletRepository walletRepo;
    private UserProfile user;

    private EditText etAmount, etCard, etName, etExpiry, etCvv;
    private RadioButton rbCredit, rbDebit;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_withdraw_credits);

        repo = new SQLiteRepository(this);
        walletRepo = repo.getWalletRepository();
        user = repo.getLoggedInUser();
        if (user == null) { finish(); return; }

        etAmount = findViewById(R.id.et_wd_amount);
        etCard = findViewById(R.id.et_wd_card_number);
        etName = findViewById(R.id.et_wd_card_name);
        etExpiry = findViewById(R.id.et_wd_card_expiry);
        etCvv = findViewById(R.id.et_wd_card_cvv);
        rbCredit = findViewById(R.id.rb_wd_credit);
        rbDebit = findViewById(R.id.rb_wd_debit);

        // R2-5.4: no explicit back button needed; system back handles it
        ((MaterialButton) findViewById(R.id.btn_wd_pay)).setOnClickListener(v -> doWithdraw());

        findViewById(R.id.btn_wd_quick_100).setOnClickListener(v -> etAmount.setText("100"));
        findViewById(R.id.btn_wd_quick_500).setOnClickListener(v -> etAmount.setText("500"));
        findViewById(R.id.btn_wd_quick_1000).setOnClickListener(v -> etAmount.setText("1000"));

        TextView tvBal = findViewById(R.id.tv_wd_current_balance);
        tvBal.setText("Wallet: " + Config.formatCurrency(user.getWalletBalance()));
    }

    private void doWithdraw() {
        String amountStr = etAmount.getText().toString().trim();
        String cardStr = etCard.getText().toString().trim();
        String nameStr = etName.getText().toString().trim();
        String expStr = etExpiry.getText().toString().trim();
        String cvvStr = etCvv.getText().toString().trim();
        String chosenType = rbCredit.isChecked() ? "CREDIT" : "DEBIT";

        if (TextUtils.isEmpty(amountStr) || TextUtils.isEmpty(cardStr) || TextUtils.isEmpty(nameStr)
                || TextUtils.isEmpty(expStr) || TextUtils.isEmpty(cvvStr)) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }
        if (amount < Config.MIN_WITHDRAW) {
            Toast.makeText(this, "Minimum ₹" + (int) Config.MIN_WITHDRAW, Toast.LENGTH_SHORT).show();
            return;
        }
        if (amount > user.getWalletBalance()) {
            Toast.makeText(this, "Insufficient wallet balance", Toast.LENGTH_LONG).show();
            return;
        }

        DemoCard card = walletRepo.findCardByNumber(cardStr);
        if (card == null
                || !card.holderName.equalsIgnoreCase(nameStr)
                || !card.expiry.equals(expStr)
                || !card.cvv.equals(cvvStr)
                || !card.cardType.equalsIgnoreCase(chosenType)) {
            Toast.makeText(this, "Card details do not match", Toast.LENGTH_LONG).show();
            return;
        }

        boolean ok = walletRepo.withdrawToCard(user.getId(), card, amount);
        if (ok) {
            Toast.makeText(this, "Withdrawn to card", Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
        }
    }
}