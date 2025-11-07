package com.example.applicationui;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class ActivityLogin extends AppCompatActivity {

    TextView signupText,ForgotPasswordText;
    EditText username, password;
    Button loginButton,signbtn;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

//        signupText = findViewById(R.id.signuptext);
        loginButton = findViewById(R.id.loginbutton);
        username = findViewById(R.id.editTextUsername);
        password = findViewById(R.id.editTextPassword);
        ForgotPasswordText = findViewById(R.id.ResetPassword);
        signbtn=findViewById(R.id.signbutton);

        signbtn.setOnClickListener(view -> {
            Intent intent = new Intent(ActivityLogin.this, ActivityRegister.class);
            startActivity(intent);
        });

        loginButton.setOnClickListener(view -> {
            String email = username.getText().toString().trim();
            String pass = password.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(ActivityLogin.this, "All fields are required", Toast.LENGTH_SHORT).show();
            } else {
                validation(email, pass);  // Sending raw user/pass without encoding
            }
        });

        ForgotPasswordText.setOnClickListener(view -> {
            String user = username.getText().toString().trim();
            String pass = password.getText().toString().trim();
            Intent intent = new Intent(ActivityLogin.this, ActivityForgotPassword.class);
            startActivity(intent);
        });

    }

    private void validation(String user, String pass) {
        ProgressDialog progressDialog = new ProgressDialog(ActivityLogin.this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Please wait...");
        progressDialog.show();

        // Unsafe if special characters exist
        String url = "http://www.testproject.info/Suhani/app_ui/ui_Login.php?Username=" + user + "&Password=" + pass;

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    progressDialog.dismiss();
                    Log.d("LoginResponse", "Server Response: " + response);

                    if (response.contains("error")) {
                        Toast.makeText(ActivityLogin.this, "Invalid Username or Password", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ActivityLogin.this, "Login Successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ActivityLogin.this, ActivityIPAddress.class);
                        intent.putExtra("Username", user);
                        startActivity(intent);
                        finishAffinity();
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Log.e("VolleyError", "Error: " + error.toString());
                    Toast.makeText(ActivityLogin.this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                }
        );

        RequestQueue queue = Volley.newRequestQueue(ActivityLogin.this);
        queue.add(request);
    }


}

