package com.example.applicationui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ActivityIPAddress extends AppCompatActivity {

    EditText ipAddressInput;
    Button btnProcess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ipaddress);  // make sure this matches your XML file name

        ipAddressInput = findViewById(R.id.IpAdressInput);
        btnProcess = findViewById(R.id.btnProcess);

        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ipAddress = ipAddressInput.getText().toString().trim();

                // Simple validation
                if (ipAddress.isEmpty()) {
                    Toast.makeText(ActivityIPAddress.this, "Please enter IP Address", Toast.LENGTH_SHORT).show();
                    return;
                }

                // You can add additional validation for correct IP address format if needed here

                Intent intent = new Intent(ActivityIPAddress.this, ActivityHome.class);
                intent.putExtra("IP_ADDRESS", ipAddress);
                startActivity(intent);
            }
        });
    }
}
