package com.example.applicationui;

import okhttp3.*;

import java.io.File;
import java.io.IOException;

public class UploadImageHelper {

    public interface UploadCallback {
        void onSuccess(String response);
        void onFailure(String error);
    }

    public static void uploadImage(File imageFile, String ipAddress, UploadCallback callback) {
        OkHttpClient client = new OkHttpClient();

        RequestBody fileBody = RequestBody.create(imageFile, MediaType.parse("image/jpeg"));

        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", imageFile.getName(), fileBody)
                .build();

        // Dynamically build the server URL with provided IP address
        String serverUrl = "http://" + ipAddress + ":5000/upload";

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(requestBody)
                .build();

        // Async call
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onFailure(e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (callback != null) {
                    if (response.isSuccessful()) {
                        String respStr = response.body().string();
                        callback.onSuccess(respStr);
                    } else {
                        callback.onFailure("Upload failed: " + response.message());
                    }
                }
            }
        });
    }
}
