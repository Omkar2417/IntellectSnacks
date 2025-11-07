package com.example.applicationui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ActivityHome extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int CAMERA_REQUEST = 1002;

    private Uri cameraImageUri;
    private File capturedPhotoFile;
    private String ipAddress;

    private Button openCameraButton, openGalleryButton, generateResultButton;
    private TextView resultText;
    private ImageView selectedImageView;

    private String selectedImagePath = null;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ipAddress = getIntent().getStringExtra("IP_ADDRESS");
        if (ipAddress == null) {
            Toast.makeText(this, "IP Address not received!", Toast.LENGTH_LONG).show();
            return;
        }

        openCameraButton = findViewById(R.id.opencamera);
        openGalleryButton = findViewById(R.id.opengallery);
        generateResultButton = findViewById(R.id.generateres);
        resultText = findViewById(R.id.welcomeText);
        selectedImageView = findViewById(R.id.selectedImageView);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ImageView blurView = findViewById(R.id.blurBackground);
            RenderEffect blur = RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP);
            blurView.setRenderEffect(blur);
        }

        openCameraButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                openCamera();
            } else {
                requestPermissions();
            }
        });

        openGalleryButton.setOnClickListener(v -> openGallery());

        generateResultButton.setOnClickListener(v -> {
            if (selectedImagePath != null) {
                Intent intent = new Intent(ActivityHome.this, ActivityDetails.class);
                intent.putExtra("IP_ADDRESS", ipAddress);
                intent.putExtra("SELECTED_IMAGE_PATH", selectedImagePath);

                startActivity(intent);
            } else {
                Toast.makeText(this, "Please capture or select an image first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile;
        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_REQUEST);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private File createImageFile() throws IOException {
        String fileName = "IMG_" + System.currentTimeMillis();
        File file = File.createTempFile(fileName, ".jpg", getCacheDir());
        capturedPhotoFile = file;
        return file;
    }

    private File createTempFileFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) throw new IOException("Unable to open input stream from URI");

        File tempFile = File.createTempFile("upload", ".jpg", getCacheDir());
        tempFile.deleteOnExit();

        try (OutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
        inputStream.close();
        return tempFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) return;

        File imageFile = null;

        if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                imageFile = createTempFileFromUri(imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (requestCode == CAMERA_REQUEST) {
            if (capturedPhotoFile != null && capturedPhotoFile.exists()) {
                imageFile = capturedPhotoFile;
            }
        }

        if (imageFile != null) {
            selectedImagePath = imageFile.getAbsolutePath();
            selectedImageView.setImageURI(Uri.fromFile(imageFile));

            UploadImageHelper.uploadImage(imageFile, ipAddress, new UploadImageHelper.UploadCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> Toast.makeText(ActivityHome.this, "Image uploaded successfully:\n" + response, Toast.LENGTH_LONG).show());
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> Toast.makeText(ActivityHome.this, "Upload failed: " + error, Toast.LENGTH_LONG).show());
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class FetchResultTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line).append("\n");
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                return "Error fetching data.";
            }
            return result.toString().trim();
        }

        @Override
        protected void onPostExecute(String result) {
            if (resultText != null) {
                result = result.replaceFirst("(?i)^(result:|data:)\\s*", "");
                resultText.setText(result);
            } else {
                Toast.makeText(ActivityHome.this, "Result TextView not found", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
