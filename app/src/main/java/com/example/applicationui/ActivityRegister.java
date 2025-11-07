package com.example.applicationui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class ActivityRegister extends AppCompatActivity {

    EditText usernameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    Button registerButton;

    // API endpoint
    String apiURL = "http://www.testproject.info/Suhani/app_ui/registeruser.php";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        usernameEditText = findViewById(R.id.editTextUsername);
        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText); // new field
        registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerUser();
            }
        });
    }

    private void registerUser() {
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                String fullUrl = apiURL +
                        "?Username=" + URLEncoder.encode(username, "UTF-8") +
                        "&EmailID=" + URLEncoder.encode(email, "UTF-8") +
                        "&Password=" + URLEncoder.encode(password, "UTF-8") +
                        "&Confirm_Password=" + URLEncoder.encode(confirmPassword, "UTF-8");

                URL url = new URL(fullUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String responseStr = response.toString();
                    Log.d("RegisterResponse", responseStr);

                    runOnUiThread(() -> {
                        Toast.makeText(ActivityRegister.this, "Registered Successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ActivityRegister.this, ActivityLogin.class);
                        startActivity(intent);
                        finish(); // optional: prevents returning to Register screen on back press
                    });


                } else {
                    Log.e("RegisterError", "HTTP error code: " + responseCode);
                    runOnUiThread(() ->
                            Toast.makeText(this, "Server error: " + responseCode, Toast.LENGTH_SHORT).show()
                    );
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
}