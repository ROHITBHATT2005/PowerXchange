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

public class AddCreditsActivity extends AppCompatActivity {
    private SQLiteRepository repo;
    private WalletRepository walletRepo;
    private UserProfile user;
    private EditText etAmount, etCard, etName, etExpiry, etCvv;
    private RadioButton rbCredit, rbDebit;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_add_credits);

        repo = new SQLiteRepository(this);
        walletRepo = repo.getWalletRepository();
        user = repo.getLoggedInUser();
        if (user == null) { finish(); return; }

        etAmount = findViewById(R.id.et_add_amount);
        etCard = findViewById(R.id.et_add_card_number);
        etName = findViewById(R.id.et_add_card_name);
        etExpiry = findViewById(R.id.et_add_card_expiry);
        etCvv = findViewById(R.id.et_add_card_cvv);
        rbCredit = findViewById(R.id.rb_add_credit);
        rbDebit = findViewById(R.id.rb_add_debit);

        findViewById(R.id.toolbar_add).setOnClickListener(v -> finish());
        ((MaterialButton) findViewById(R.id.btn_add_pay)).setOnClickListener(v -> doAdd());

        findViewById(R.id.btn_quick_500).setOnClickListener(v -> etAmount.setText("500"));
        findViewById(R.id.btn_quick_1000).setOnClickListener(v -> etAmount.setText("1000"));
        findViewById(R.id.btn_quick_5000).setOnClickListener(v -> etAmount.setText("5000"));
        findViewById(R.id.btn_quick_10000).setOnClickListener(v -> etAmount.setText("10000"));
    }

    private void doAdd() {
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
        if (amount < Config.MIN_ADD_CREDITS) {
            Toast.makeText(this, "Minimum ₹" + (int) Config.MIN_ADD_CREDITS, Toast.LENGTH_SHORT).show();
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

        if (card.balance < amount) {
            Toast.makeText(this, "Insufficient card balance", Toast.LENGTH_LONG).show();
            return;
        }

        boolean ok = walletRepo.addCreditsFromCard(user.getId(), card, amount);
        if (ok) {
            Toast.makeText(this, "Credits added", Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
        }
    }
}