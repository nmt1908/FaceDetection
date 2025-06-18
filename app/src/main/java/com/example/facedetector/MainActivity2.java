package com.example.facedetector;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.facedetector.model.User;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity2 extends AppCompatActivity {
    private final OkHttpClient httpClient = new OkHttpClient();
    private static final int REQUEST_IMAGE_PICK = 1001;
    private TextView appTitle,abcxyz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button btnPickImage = findViewById(R.id.btnPickImage);
        appTitle = findViewById(R.id.appTitle);
        abcxyz=findViewById(R.id.abcxyz);
        btnPickImage.setOnClickListener(v -> openGallery());

    }
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }
    private File getFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File tempFile = File.createTempFile("selected_img", ".jpg", getCacheDir());
            FileOutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                File imageFile = getFileFromUri(imageUri);
                if (imageFile != null) {
                    uploadImageToApi(imageFile);
                }
            }
        }
    }

    private void uploadImageToApi(File file) {
        new Thread(() -> {
            try {
                byte[] fileBytes;
                try (InputStream is = new FileInputStream(file)) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    int nRead;
                    byte[] data = new byte[16384];
                    while ((nRead = is.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    fileBytes = buffer.toByteArray();
                }

                String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date());

                RequestBody requestBodyPrimary = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("image_file", file.getName(),
                                RequestBody.create(fileBytes, MediaType.parse("image/jpeg")))
                        .build();

                Request requestPrimary = new Request.Builder()
                        .url("http://10.13.32.50:5003/recognize-anti-spoofing")
                        .addHeader("X-API-Key", "vg_login_app")
                        .addHeader("X-Time", currentTime)
                        .post(requestBodyPrimary)
                        .build();

                OkHttpClient clientWithTimeout = httpClient.newBuilder()
                        .callTimeout(5, TimeUnit.SECONDS)
                        .build();

                Response response;
                String responseBody;
                long startTime = System.currentTimeMillis();

                try {
                    response = clientWithTimeout.newCall(requestPrimary).execute();
                    long elapsed = System.currentTimeMillis() - startTime;

                    responseBody = response.body().string();
                    Log.e("Response-Primary", responseBody);
                    Log.e("API-TIME", "Primary API time: " + elapsed + " ms");

                    long finalElapsed = elapsed;
                    runOnUiThread(() -> appTitle.setText("Primary API: " + finalElapsed + " ms"));

                } catch (Exception ex) {
                    Log.e("PrimaryAPI", "Primary API failed, fallback: " + ex.getMessage());

                    // Fallback - API phụ Port 8001
                    RequestBody requestBodyFallback = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("env_token", "8d59d8d588f84fc0a24291b8c36b6206")
                            .addFormDataPart("image_file", file.getName(),
                                    RequestBody.create(file, MediaType.parse("image/jpeg")))
                            .build();

                    Request requestFallback = new Request.Builder()
                            .url("http://10.1.16.23:8001/api/x/fr/env/face_search")
                            .post(requestBodyFallback)
                            .build();

                    startTime = System.currentTimeMillis();
                    response = httpClient.newCall(requestFallback).execute();
                    long elapsed = System.currentTimeMillis() - startTime;

                    responseBody = response.body().string();
                    Log.e("Response-Fallback", responseBody);
                    Log.e("API-TIME", "Fallback API time: " + elapsed + " ms");

                    long finalElapsed = elapsed;
                    runOnUiThread(() -> appTitle.setText("Fallback API: " + finalElapsed + " ms"));
                }

                if (!response.isSuccessful()) {
                    if (response.code() == 400) {
                        abcxyz.setText("RECONIZED FAILED");
                    } else {
                        abcxyz.setText("RECONIZED EROR: " + response.code());
                    }
                    return;
                }

                JSONObject jsonObject = new JSONObject(responseBody);

                if (jsonObject.optBoolean("is_fake", false)) {
                    abcxyz.setText("FAKE FACE ");
                    return;
                }

                if (jsonObject.optInt("is_recognized", 0) == 1) {
                    String name = jsonObject.optString("name");
                    String cardId = jsonObject.optString("id_string");
                    double similarityVal = jsonObject.optDouble("similarity", 0) * 100;

                    if (similarityVal <= 55) {
                        abcxyz.setText("RECONIZED FAILED");
                        return;
                    }
                    abcxyz.setText("RECONIZED OK");
                    String similarity = String.format("%.2f%%", similarityVal);
                    User activeUser = new User(name, cardId, similarity);



                }else{
                    abcxyz.setText("RECONIZED FAILED");
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("FaceAPI_Error", e.getMessage(), e);
            } finally {

            }
        }).start();
    }

}