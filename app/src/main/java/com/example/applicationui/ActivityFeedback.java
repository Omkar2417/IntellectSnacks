package com.example.applicationui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ActivityFeedback extends AppCompatActivity {

    RadioGroup goalRadioGroup;
    EditText suggestionEditText;
    Button submitButton;

    // Replace with your PHP file URL (no ? at the end!)
    String baseUrl = "http://www.testproject.info/Suhani/app_ui/feedback.php";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback); // ✅ Ensure layout file name matches

        // ✅ Updated with correct IDs from XML
        goalRadioGroup = findViewById(R.id.radioGroup_goal);
        suggestionEditText = findViewById(R.id.editText_suggestions);
        submitButton = findViewById(R.id.button_submit);

        submitButton.setOnClickListener(v -> {
            int selectedId = goalRadioGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Please select a goal status", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedRadio = findViewById(selectedId);
            String goalStatus = selectedRadio.getText().toString();
            String suggestion = suggestionEditText.getText().toString().trim();

            sendReviewToServer(goalStatus, suggestion);
        });
    }

    private void sendReviewToServer(String goalStatus, String suggestion) {
        try {
            String encodedGoal = URLEncoder.encode(goalStatus, "UTF-8");
            String encodedSuggestion = URLEncoder.encode(suggestion, "UTF-8");

            String fullUrl = baseUrl + "?goal_status=" + encodedGoal + "&suggestion=" + encodedSuggestion;

            StringRequest request = new StringRequest(Request.Method.GET, fullUrl,
                    response -> {
                        // Log or toast exact server response for debugging

                        if (response.trim().equalsIgnoreCase("success")) {
                            Toast.makeText(this, "Feedback submitted!", Toast.LENGTH_SHORT).show();
                            goalRadioGroup.clearCheck();
                            suggestionEditText.setText("");
                        } else {
                            Toast.makeText(this, "Submission failed: " + response, Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show()
            );

            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(request);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Toast.makeText(this, "Encoding error!", Toast.LENGTH_SHORT).show();
        }
    }
}