package com.example.applicationui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ActivityDetails extends AppCompatActivity {

    private ImageView imageView;
    private TextView tvHarmfulIngredients, tvSuggestions;
    private String ipAddress;
    private String nerIngredientsText = "";
    Button feedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        imageView = findViewById(R.id.imageView);
        tvHarmfulIngredients = findViewById(R.id.tvHarmfulIngredients);
        tvSuggestions = findViewById(R.id.tvSuggestions);
        feedback = findViewById(R.id.feedback);

        feedback.setOnClickListener(v -> {
            Intent intent = new Intent(ActivityDetails.this, ActivityFeedback.class);
            startActivity(intent);
        });

        ipAddress = getIntent().getStringExtra("IP_ADDRESS");
        String imagePath = getIntent().getStringExtra("SELECTED_IMAGE_PATH");

        if (imagePath != null) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                imageView.setImageBitmap(bitmap);
            }
        }

        fetchOCRAndStartPipeline();
    }
    private void fetchOCRAndStartPipeline() {
        String ocrUrl = "http://www.testproject.info/Suhani/app_ui/ocrResult.php";
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                ocrUrl,
                response -> {
                    Log.d("OCR_PLAIN", "Plain Response: " + response);  // ✅ Debug

                    try {
                        // Parse manually
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("text", response.trim());

                        // Extract and pass
                        String nerIngredientsText = jsonObject.getString("text");
                        getCorrectionResults(nerIngredientsText);

                    } catch (JSONException e) {
                        Log.e("ManualJSON", "JSON conversion failed: " + e.getMessage());
                    }
                },
                error -> Log.e("VolleyError", "Error fetching plain text: " + error.toString())
        );

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(stringRequest);
    }


    private void getCorrectionResults(String ingredientTextFromOCR) {
        String url = "http://" + ipAddress + ":5000/correct_ingredients_db";
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonRequest = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        // Get the string of comma-separated ingredients
                        String ingredientText = response.getString("ingredients");
                        Log.d("RawCorrectionResponse", ingredientText);

                        // Convert to JSONArray format expected by next method
                        JSONArray nerArray = new JSONArray();
                        String[] items = ingredientText.split(",");

                        for (String item : items) {
                            JSONObject obj = new JSONObject();
                            obj.put("ingredient", item.trim());
                            nerArray.put(obj);
                        }
Log.d("output complete",nerArray.toString());
                        //Log.d("FormattedNERArray", nerArray.toString());

                        fetchMatchResults(nerArray);  // Pass to next method

                    } catch (Exception e) {
                        Log.e("CorrectionError", "JSON parsing error: " + e.getMessage());
                    }
                },
                error -> Log.e("CorrectionError", "Volley error: " + error.toString())
        );

        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(jsonRequest);
    }




    private void fetchMatchResults(JSONArray correctedIngredients) {
        String url = "http://" + ipAddress + ":5000/match_ingredients";
        RequestQueue queue = Volley.newRequestQueue(this);

        // Wrap JSONArray in a JSONObject
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("ingredients", correctedIngredients);
        } catch (JSONException e) {
            Log.e("JSON", "Error creating JSON: " + e.getMessage());
            return;
        }

        JsonObjectRequest jsonRequest = new JsonObjectRequest(
                Request.Method.POST, url, jsonBody,
                response -> {
                    try {

                        Log.d("MatchResultFull", "Full JSON Response: " + response.toString());

                        JSONArray resultArray = response.getJSONArray("results");
                        parseResponseAndDisplay(resultArray);
                    } catch (Exception e) {
                        Log.e("ParseError", "Error parsing response: " + e.getMessage());
                    }
                },
                error -> Log.e("Volley", "Server error: " + error.toString())
        );

        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(jsonRequest);
    }



    private void parseResponseAndDisplay(JSONArray responseArray) throws JSONException {
        StringBuilder harmfulIngredients = new StringBuilder();
        StringBuilder suggestions = new StringBuilder();

        boolean hasHarmful = false;  // Flag to track if any harmful ingredient was found

        for (int i = 0; i < responseArray.length(); i++) {
            JSONObject item = responseArray.getJSONObject(i);
            String ingredient = item.getString("ingredient");
            JSONArray matches = item.optJSONArray("matches");

            // If matches is null or empty
            if (matches == null || matches.length() == 0) {
                continue;  // Skip this ingredient, no harmful info
            }

            hasHarmful = true;  // Found at least one harmful item

            harmfulIngredients.append("Ingredient: ").append(ingredient).append("\n");

            for (int j = 0; j < matches.length(); j++) {
                JSONObject match = matches.getJSONObject(j);
                JSONObject details = match.getJSONObject("details");

                String sideEffects = details.optString("Side effects", "N/A");


                harmfulIngredients
                        .append("  Side Effects: ").append(sideEffects).append("\n\n");
            }

            suggestions.append("• Avoid excessive use of ").append(ingredient).append("\n");
        }

        // If no harmful ingredients were found
        if (!hasHarmful) {
            tvHarmfulIngredients.setText("No harmful ingredients present.");
            tvSuggestions.setText("No suggestions needed.");
        } else {
            tvHarmfulIngredients.setText(harmfulIngredients.toString().trim());
            tvSuggestions.setText(suggestions.toString().trim());
        }
    }


}
