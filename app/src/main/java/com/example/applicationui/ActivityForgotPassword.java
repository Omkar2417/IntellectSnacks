package com.example.applicationui;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ActivityForgotPassword extends AppCompatActivity {

    EditText emailField, passwordField;
    Button resetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailField = findViewById(R.id.editTextEmail);
        passwordField = findViewById(R.id.resetPassword);
        resetButton = findViewById(R.id.buttonReset);

        resetButton.setOnClickListener(view -> {
            String email = emailField.getText().toString().trim();
            String newPassword = passwordField.getText().toString().trim();

            if (email.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(this, "Please fill both fields", Toast.LENGTH_SHORT).show();
            } else {
                sendResetRequest(email, newPassword);
            }
        });
    }

    private void sendResetRequest(String email, String newPassword) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Resetting password...");
        dialog.setCancelable(false);
        dialog.show();

        try {
            // Encode parameters for URL safety
            String encodedEmail = URLEncoder.encode(email, "UTF-8");
            String encodedPassword = URLEncoder.encode(newPassword, "UTF-8");

            String url = "http://www.testproject.info/Suhani/app_ui/reset_password.php?EmailID=" + encodedEmail + "&Password=" + encodedPassword;

            StringRequest request = new StringRequest(Request.Method.GET, url,
                    response -> {
                        dialog.dismiss();
                        if (response.contains("successfully")) {
                            Toast.makeText(this, "Password reset successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, response, Toast.LENGTH_LONG).show();
                        }
                    },
                    error -> {
                        dialog.dismiss();
                        Toast.makeText(this, "Error: " + error.toString(), Toast.LENGTH_LONG).show();
                    });

            Volley.newRequestQueue(this).add(request);

        } catch (UnsupportedEncodingException e) {
            dialog.dismiss();
            Toast.makeText(this, "Encoding error", Toast.LENGTH_SHORT).show();
        }
    }
}
