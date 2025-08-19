//package com.example.facedetector;
//import android.Manifest;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.net.ConnectivityManager;
//import android.net.NetworkCapabilities;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.util.Log;
//import android.view.Display;
//import android.view.View;
//import android.widget.LinearLayout;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.camera.camera2.interop.Camera2CameraInfo;
//import androidx.camera.core.CameraInfo;
//import androidx.camera.core.CameraSelector;
//import androidx.camera.core.ImageAnalysis;
//import androidx.camera.core.ImageCapture;
//import androidx.camera.core.ImageCaptureException;
//import androidx.camera.core.Preview;
//import androidx.camera.lifecycle.ProcessCameraProvider;
//import androidx.camera.view.PreviewView;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import com.example.facedetector.model.User;
//import com.google.common.util.concurrent.ListenableFuture;
//import org.json.JSONObject;
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.InputStream;
//import java.text.SimpleDateFormat;
//import java.util.Collections;
//import java.util.Date;
//import java.util.Locale;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//import okhttp3.MediaType;
//import okhttp3.MultipartBody;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.RequestBody;
//import okhttp3.Response;
//public class MainActivity extends AppCompatActivity implements FaceAnalyzer.FaceDetectionListener {
//    private PreviewView previewView;
//    private FaceGraphicOverlay graphicOverlay;
//    private ImageCapture imageCapture;
//    private ProcessCameraProvider cameraProvider;
//    private long faceStraightStartTime = 0;
//    private boolean isTakingPhoto = false;
//    private final OkHttpClient httpClient = new OkHttpClient();
//    private static final int LOW_LIGHT_THRESHOLD = 50;
//    private static final int LOW_LIGHT_FRAME_LIMIT = 30;
//    private static final int HIGH_LIGHT_THRESHOLD = 90;
//    TextView alertTextView, labelName, nameTextView, labelCardId, cardIDTextView, labelSimilarity, similarityTextView, appTitle;
//    ProgressBar loadingSpinner;
//    private LinearLayout loadingContainer, userInfoPanel;
//    private String currentCameraId = "0";
//    private boolean canSwitchFrontToBack = true;
//    private boolean canSwitchBackToFront = true;
//    private int lowLightCount = 0;
//    private long IDLE_DELAY_MS;
//    private boolean isCountingDownToIdle = false;
//    private Handler idleHandler = new Handler(Looper.getMainLooper());
//    private final Handler mainHandler = new Handler(Looper.getMainLooper());
//
//    // Executor riêng cho ImageAnalysis & các tác vụ nặng
//    private ExecutorService analyzerExecutor;
//    private View idleOverlay;
//    private TextView idleClock;
//    private boolean isIdle = false;
//    private final Handler clockHandler = new Handler(Looper.getMainLooper());
//    private final Runnable clockTick = new Runnable() {
//        @Override public void run() {
//            String t = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
//            idleClock.setText(t);
//            clockHandler.postDelayed(this, 1000);
//        }
//    };
//    // Cooldown chống rebind liên tục
//    private static final long SWITCH_COOLDOWN_MS = 3000;
//
//    private long lastSwitchTs = 0;
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        analyzerExecutor = Executors.newSingleThreadExecutor();
//        if (!isInternetAvailable()) {
//            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_LONG).show();
//        }
//        IDLE_DELAY_MS = getSharedPreferences("settings", MODE_PRIVATE).getInt("time_waiting", 8000);
//        loadingContainer = findViewById(R.id.loadingContainer);
//        userInfoPanel = findViewById(R.id.userInfoPanel);
//        previewView = findViewById(R.id.previewView);
//        graphicOverlay = findViewById(R.id.graphicOverlay);
//        alertTextView = findViewById(R.id.labelAlert);
//        labelName = findViewById(R.id.labelUserName);
//        nameTextView = findViewById(R.id.userName);
//        labelCardId = findViewById(R.id.labelUserCardId);
//        cardIDTextView = findViewById(R.id.userCardId);
//        labelSimilarity = findViewById(R.id.labelUserSimilarity);
//        similarityTextView = findViewById(R.id.userSimilarity);
//        loadingSpinner = findViewById(R.id.loadingSpinner);
//        appTitle = findViewById(R.id.appTitle);
//        idleOverlay = findViewById(R.id.idleOverlay);
//        idleOverlay.bringToFront();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            idleOverlay.setElevation(100f);
//        }
//        idleOverlay.setClickable(true);
//        idleOverlay.setFocusable(true);
//        idleOverlay.setOnTouchListener((v, e) -> isIdle);
//        idleClock = findViewById(R.id.idleClock);
//        graphicOverlay.setCameraFacing(true); // camera trước
//
//        String cameraId = getIntent().getStringExtra("camera_id");
//        if (cameraId == null) cameraId = "0";
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//                == PackageManager.PERMISSION_GRANTED) {
//            startCamera(cameraId);
//        } else {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
//        }
//    }
////    private final Runnable goIdleRunnable = () -> {
////        Intent intent = new Intent(MainActivity.this, IdleActivity.class);
////        intent.putExtra("camera_id", currentCameraId);
////        startActivity(intent);
////        finish();
////    };
//private final Runnable goIdleRunnable = this::enterIdle;
//
//    private void enterIdle() {
//    if (isIdle) return;
//    isIdle = true;
//
//    // Che preview nhưng KHÔNG unbind camera
//    runOnUiThread(() -> {
//        try {
//            graphicOverlay.setWillNotDraw(true);
//            // có thể dùng animate cho mượt
//            previewView.animate().alpha(0f).setDuration(180).start();
//
//            // show overlay
//            idleOverlay.setAlpha(0f);
//            idleOverlay.setVisibility(View.VISIBLE);
//            idleOverlay.animate().alpha(1f).setDuration(180).start();
//
//            // start clock
//            clockHandler.removeCallbacks(clockTick);
//            clockHandler.post(clockTick);
//        } catch (Exception ignore) {}
//    });
//}
//
//    private void exitIdle() {
//        if (!isIdle) return;
//        isIdle = false;
//
//        runOnUiThread(() -> {
//            try {
//                // Hiện preview trở lại
//                previewView.animate().alpha(1f).setDuration(150).start();
//                graphicOverlay.setWillNotDraw(false);
//                graphicOverlay.invalidate();
//
//                // hide overlay
//                idleOverlay.animate().alpha(0f).setDuration(150)
//                        .withEndAction(() -> idleOverlay.setVisibility(View.GONE))
//                        .start();
//
//                clockHandler.removeCallbacks(clockTick);
//            } catch (Exception ignore) {}
//        });
//    }
//    @Override
//    public void onAverageLuminance(double luminance) {
//        // Hysteresis + chỉ switch khi THỰC SỰ đổi camera
//        if (currentCameraId.equals("0")) { // đang FRONT
//            if (luminance < LOW_LIGHT_THRESHOLD && canSwitchFrontToBack) {
//                lowLightCount++;
//                if (lowLightCount >= LOW_LIGHT_FRAME_LIMIT) {
//                    lowLightCount = 0;
//                    canSwitchFrontToBack = false;
//                    canSwitchBackToFront = true;
//                    Log.d("Switch Camera", "0→1 (dark)");
//                    switchCameraIfNeeded("1");
//                }
//            } else {
//                lowLightCount = 0;
//                // khi đủ sáng trở lại mới cho phép lần chuyển tiếp theo
//                if (!canSwitchFrontToBack && luminance > HIGH_LIGHT_THRESHOLD) {
//                    canSwitchFrontToBack = true;
//                }
//            }
//        } else { // đang BACK ("1")
//            if (luminance > HIGH_LIGHT_THRESHOLD && canSwitchBackToFront) {
//                lowLightCount++;
//                if (lowLightCount >= LOW_LIGHT_FRAME_LIMIT) {
//                    lowLightCount = 0;
//                    canSwitchBackToFront = false;
//                    canSwitchFrontToBack = true;
//                    Log.d("Switch Camera", "1→0 (bright)");
//                    switchCameraIfNeeded("0");
//                }
//            } else {
//                lowLightCount = 0;
//                // khi tối lại mới cho phép lần chuyển tiếp theo
//                if (!canSwitchBackToFront && luminance < LOW_LIGHT_THRESHOLD) {
//                    canSwitchBackToFront = true;
//                }
//            }
//        }
//    }
//    private void switchCameraIfNeeded(String newId) {
//        if (newId.equals(currentCameraId)) return; // tránh rebind cùng ID
//        long now = System.currentTimeMillis();
//        if (now - lastSwitchTs < SWITCH_COOLDOWN_MS) return; // cooldown
//        lastSwitchTs = now;
//        startCamera(newId);
//    }
////    public void onFaceAreaLarge(boolean isLarge) {
////        runOnUiThread(() -> {
////            if (isLarge) {
////                if (isCountingDownToIdle) {
////                    idleHandler.removeCallbacks(goIdleRunnable);
////                    isCountingDownToIdle = false;
////                }
////            } else {
////                if (!isCountingDownToIdle) {
////                    idleHandler.postDelayed(goIdleRunnable, IDLE_DELAY_MS);
////                    isCountingDownToIdle = true;
////                }
////            }
////        });
////    }
//@Override
//public void onFaceAreaLarge(boolean isLarge) {
//    runOnUiThread(() -> {
//        if (isLarge) {
//            // Nếu đang đếm để Idle thì hủy đếm
//            if (isCountingDownToIdle) {
//                idleHandler.removeCallbacks(goIdleRunnable);
//                isCountingDownToIdle = false;
//            }
//            // Nếu đang idle -> thức dậy
//            if (isIdle) {
//                exitIdle();
//            }
//        } else {
//            // Không có mặt/ mặt nhỏ -> bắt đầu đếm để vào Idle
//            if (!isCountingDownToIdle && !isIdle) {
//                idleHandler.postDelayed(goIdleRunnable, IDLE_DELAY_MS);
//                isCountingDownToIdle = true;
//            }
//        }
//    });
//}
//
//    private void uploadImageToApi(File file) {
//        // Toàn bộ chạy ở background (được gọi từ executor)
//        try {
//            byte[] fileBytes;
//            try (InputStream is = new FileInputStream(file)) {
//                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//                byte[] data = new byte[16384];
//                int nRead;
//                while ((nRead = is.read(data, 0, data.length)) != -1) {
//                    buffer.write(data, 0, nRead);
//                }
//                fileBytes = buffer.toByteArray();
//            }
//            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//                    .format(new Date());
//            RequestBody requestBodyPrimary = new MultipartBody.Builder()
//                    .setType(MultipartBody.FORM)
//                    .addFormDataPart("image_file", file.getName(),
//                            RequestBody.create(fileBytes, MediaType.parse("image/jpeg")))
//                    .build();
//            Request requestPrimary = new Request.Builder()
//                    .url("http://10.13.32.51:5001/recognize-anti-spoofing")
//                    .addHeader("X-API-Key", "vg_login_app")
//                    .addHeader("X-Time", currentTime)
//                    .post(requestBodyPrimary)
//                    .build();
//            OkHttpClient clientWithTimeout = httpClient.newBuilder()
//                    .connectTimeout(3, TimeUnit.SECONDS)
//                    .readTimeout(4, TimeUnit.SECONDS)
//                    .writeTimeout(4, TimeUnit.SECONDS)
//                    .callTimeout(6, TimeUnit.SECONDS)
//                    .build();
//            Response response;
//            String responseBody;
//            long startTime = System.currentTimeMillis();
//            try (Response resp = clientWithTimeout.newCall(requestPrimary).execute()) {
//                long elapsed = System.currentTimeMillis() - startTime;
//                response = resp;
//                responseBody = response.body() != null ? response.body().string() : "";
//                Log.e("Response-Primary", responseBody);
//                Log.e("API-TIME", "Primary API time: " + elapsed + " ms");
//            } catch (Exception ex) {
//                Log.e("PrimaryAPI", "Primary API failed, fallback: " + ex.getMessage());
//                // Fallback
//                RequestBody requestBodyFallback = new MultipartBody.Builder()
//                        .setType(MultipartBody.FORM)
//                        .addFormDataPart("env_token", "8d59d8d588f84fc0a24291b8c36b6206")
//                        .addFormDataPart("image_file", file.getName(),
//                                RequestBody.create(file, MediaType.parse("image/jpeg")))
//                        .build();
//                Request requestFallback = new Request.Builder()
//                        .url("http://10.1.16.23:8001/api/x/fr/env/face_search")
//                        .post(requestBodyFallback)
//                        .build();
//                startTime = System.currentTimeMillis();
//                try (Response resp2 = httpClient.newCall(requestFallback).execute()) {
//                    long elapsed2 = System.currentTimeMillis() - startTime;
//                    response = resp2;
//                    responseBody = response.body() != null ? response.body().string() : "";
//                    Log.e("Response-Fallback", responseBody);
//                    Log.e("API-TIME", "Fallback API time: " + elapsed2 + " ms");
//                }
//            }
//            if (response == null || !response.isSuccessful()) {
//                final int code = (response != null) ? response.code() : -1;
//                if (code == 400) {
//                    handleRecognitionFail();
//                } else {
//                    showToastOnMainThread("API error: " + code);
//                }
//                return;
//            }
//            JSONObject jsonObject = new JSONObject(responseBody);
//            if (jsonObject.optBoolean("is_fake", false)) {
//                handleRecognitionFail();
//                return;
//            }
//            if (jsonObject.optInt("is_recognized", 0) == 1) {
//                String name = jsonObject.optString("name");
//                String cardId = jsonObject.optString("id_string");
//                double similarityVal = jsonObject.optDouble("similarity", 0) * 100;
//
//                if (similarityVal <= 55) {
//                    handleRecognitionFail();
//                    return;
//                }
//
//                String similarity = String.format(Locale.US, "%.2f%%", similarityVal);
//                User activeUser = new User(name, cardId, similarity);
//
//                runOnUiThread(() -> {
//                    alertTextView.setText("Facial recognition successful");
//
//                    labelName.setVisibility(View.VISIBLE);
//                    labelCardId.setVisibility(View.VISIBLE);
//                    labelSimilarity.setVisibility(View.VISIBLE);
//                    nameTextView.setVisibility(View.VISIBLE);
//                    cardIDTextView.setVisibility(View.VISIBLE);
//                    similarityTextView.setVisibility(View.VISIBLE);
//
//                    labelName.setText("Name:");
//                    nameTextView.setText(activeUser.getName());
//                    labelCardId.setText("Card ID:");
//                    cardIDTextView.setText(activeUser.getCardId());
//                    labelSimilarity.setText("Similarity:");
//                    similarityTextView.setText(activeUser.getSimilarity());
//
//                    userInfoPanel.setVisibility(View.VISIBLE);
//                    userInfoPanel.postDelayed(() -> userInfoPanel.setVisibility(View.GONE), 1500);
//
//                    // Bật relay ngay, tắt sau 3s (không dùng sleep)
//                    turnRelayOnThenOff();
//                });
//
//            } else {
//                runOnUiThread(() -> alertTextView.setText("Face not recognized"));
//                handleRecognitionFail();
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            showToastOnMainThread("Error: " + e.getMessage());
//            Log.e("FaceAPI_Error", e.getMessage(), e);
//        } finally {
//            runOnUiThread(() -> isTakingPhoto = false);
//        }
//    }
//    private void turnRelayOnThenOff() {
//        // Bật
//        analyzerExecutor.execute(() -> {
//            try {
//                Runtime.getRuntime().exec(new String[]{"sh", "-c",
//                        "H=$(cat /sys/class/leds/relay_ctl/max_brightness); echo $H > /sys/class/leds/relay_ctl/brightness"});
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//        // Tắt sau 3s (không block)
//        mainHandler.postDelayed(() -> analyzerExecutor.execute(() -> {
//            try {
//                Runtime.getRuntime().exec(new String[]{"sh", "-c",
//                        "echo 0 > /sys/class/leds/relay_ctl/brightness"});
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }), 3000);
//    }
//    private void handleRecognitionFail() {
//        runOnUiThread(() -> {
//            userInfoPanel.setVisibility(View.VISIBLE);
//            alertTextView.setVisibility(View.VISIBLE);
//            alertTextView.setText("Facial recognition failed");
//
//            labelName.setVisibility(View.GONE);
//            nameTextView.setVisibility(View.GONE);
//            labelCardId.setVisibility(View.GONE);
//            cardIDTextView.setVisibility(View.GONE);
//            labelSimilarity.setVisibility(View.GONE);
//            similarityTextView.setVisibility(View.GONE);
//
//            loadingContainer.setVisibility(View.GONE);
//
//            alertTextView.postDelayed(() -> alertTextView.setText(""), 2000);
//        });
//    }
//    private void showToastOnMainThread(String message) {
//        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
//    }
//    private void startCamera(String cameraId) {
//        // Guard: nếu đang đúng camera thì bỏ qua
//        if (cameraId.equals(currentCameraId) && cameraProvider != null) {
//            return;
//        }
//        currentCameraId = cameraId;
//        isTakingPhoto = false;
//        faceStraightStartTime = 0;
//
//        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
//
//        cameraProviderFuture.addListener(() -> {
//            try {
//                cameraProvider = cameraProviderFuture.get();
//
//                Preview preview = new Preview.Builder().build();
//                preview.setSurfaceProvider(previewView.getSurfaceProvider());
//
//                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
//                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                        .build();
//                // Phân tích chạy off-main
//                imageAnalysis.setAnalyzer(analyzerExecutor,
//                        new FaceAnalyzer(this, graphicOverlay, this));
//
//                int rotation;
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                    rotation = previewView.getDisplay() != null ? previewView.getDisplay().getRotation() : SurfaceRotationCompat.getDefaultRotation();
//                } else {
//                    Display d = getWindowManager().getDefaultDisplay();
//                    rotation = d.getRotation();
//                }
//
//                imageCapture = new ImageCapture.Builder()
//                        .setTargetRotation(rotation)
//                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
//                        .build();
//
//                CameraSelector cameraSelector = new CameraSelector.Builder()
//                        .addCameraFilter(cameraInfos -> {
//                            for (CameraInfo info : cameraInfos) {
//                                Camera2CameraInfo camera2Info = Camera2CameraInfo.from(info);
//                                if (camera2Info.getCameraId().equals(cameraId)) {
//                                    return Collections.singletonList(info);
//                                }
//                            }
//                            return Collections.emptyList();
//                        })
//                        .build();
//
//                cameraProvider.unbindAll();
//                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture);
//
//            } catch (ExecutionException | InterruptedException e) {
//                e.printStackTrace();
//            }
//        }, ContextCompat.getMainExecutor(this));
//    }
//    private void stopCamera() {
//        if (cameraProvider != null) {
//            cameraProvider.unbindAll();
//        }
//        isTakingPhoto = false;
//        faceStraightStartTime = 0;
//    }
//    @Override
//    public void onFaceLookingStraight() {
//        runOnUiThread(() -> {
//            if (isTakingPhoto) return;
//
//            if (faceStraightStartTime == 0) {
//                faceStraightStartTime = System.currentTimeMillis();
//            } else {
//                long elapsed = System.currentTimeMillis() - faceStraightStartTime;
//                if (elapsed >= 2000) {
//                    isTakingPhoto = true;
//                    faceStraightStartTime = 0;
//                    takePhoto();
//                }
//            }
//        });
//    }
//    @Override
//    public void onFaceNotLookingStraight() {
//        runOnUiThread(() -> faceStraightStartTime = 0);
//    }
////    private void takePhoto() {
////        if (imageCapture == null) {
////            isTakingPhoto = false;
////            return;
////        }
////        String filename = "IMG_" + System.currentTimeMillis() + ".jpg";
////        File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
////        if (dir == null) {
////            isTakingPhoto = false;
////            return;
////        }
////        File file = new File(dir, filename);
////        ImageCapture.OutputFileOptions outputOptions =
////                new ImageCapture.OutputFileOptions.Builder(file).build();
////
////        // Callback chạy trên executor để tránh block UI
////        imageCapture.takePicture(outputOptions, analyzerExecutor, new ImageCapture.OnImageSavedCallback() {
////            @Override
////            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
////                try {
////                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
////                    if (bitmap == null) {
////                        runOnUiThread(() -> isTakingPhoto = false);
////                        return;
////                    }
////                    // Resize nhẹ để upload nhanh, giảm CPU
////                    int originalWidth = bitmap.getWidth();
////                    int originalHeight = bitmap.getHeight();
////                    int targetWidth = 500;
////                    int targetHeight = (int) ((float) targetWidth / originalWidth * originalHeight);
////                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
////
////                    try (FileOutputStream out = new FileOutputStream(file)) {
////                        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
////                    }
////                    // Gửi API (background)
////                    uploadImageToApi(file);
////
////                } catch (Exception e) {
////                    e.printStackTrace();
////                    runOnUiThread(() -> isTakingPhoto = false);
////                }
////            }
////            @Override
////            public void onError(@NonNull ImageCaptureException exception) {
////                exception.printStackTrace();
////                runOnUiThread(() -> isTakingPhoto = false);
////            }
////        });
////    }
//private void takePhoto() {
//    if (imageCapture == null) {
//        setTakingPhoto(false);
//        return;
//    }
//
//    File file = createImageFile();
//    if (file == null) {
//        setTakingPhoto(false);
//        return;
//    }
//
//    ImageCapture.OutputFileOptions outputOptions =
//            new ImageCapture.OutputFileOptions.Builder(file).build();
//
//    imageCapture.takePicture(outputOptions, analyzerExecutor, new ImageCapture.OnImageSavedCallback() {
//        @Override
//        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
//            processAndUpload(file);
//        }
//
//        @Override
//        public void onError(@NonNull ImageCaptureException exception) {
//            exception.printStackTrace();
//            setTakingPhoto(false);
//        }
//    });
//}
//
//    private File createImageFile() {
//        File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
//        if (dir == null) return null;
//        return new File(dir, "IMG_" + System.currentTimeMillis() + ".jpg");
//    }
//
//    private void processAndUpload(File file) {
//        try {
//            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
//            if (bitmap == null) {
//                setTakingPhoto(false);
//                return;
//            }
//
//            Bitmap resized = resizeBitmap(bitmap, 500);
//            bitmap.recycle(); // giải phóng ảnh gốc
//
//            try (FileOutputStream out = new FileOutputStream(file)) {
//                resized.compress(Bitmap.CompressFormat.JPEG, 90, out);
//            }
//            resized.recycle();
//
//            uploadImageToApi(file); // chạy async
//        } catch (Exception e) {
//            e.printStackTrace();
//            setTakingPhoto(false);
//        }
//    }
//
//    private Bitmap resizeBitmap(Bitmap src, int targetWidth) {
//        int targetHeight = (int) ((float) targetWidth / src.getWidth() * src.getHeight());
//        return Bitmap.createScaledBitmap(src, targetWidth, targetHeight, true);
//    }
//
//    private void setTakingPhoto(boolean taking) {
//        runOnUiThread(() -> isTakingPhoto = taking);
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            startCamera("0");
//        }
//    }
//    public boolean isInternetAvailable() {
//        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        if (connectivityManager != null) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
//                return capabilities != null &&
//                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
//                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
//                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
//            } else {
//                android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
//                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
//            }
//        }
//        return false;
//    }
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (analyzerExecutor != null) {
//            analyzerExecutor.shutdown();
//        }
//        clockHandler.removeCallbacks(clockTick);
//
//    }
//    private static class SurfaceRotationCompat {
//        static int getDefaultRotation() {
//            // fallback nếu không truy cập được display (hiếm)
//            return 0;
//        }
//    }
//}
package com.example.facedetector;
import java.io.File;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CaptureRequest;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.facedetector.model.User;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements FaceAnalyzer.FaceDetectionListener {
    private PreviewView previewView;
    private FaceGraphicOverlay graphicOverlay;
    private ImageCapture imageCapture;
    private ProcessCameraProvider cameraProvider;
    private long faceStraightStartTime = 0;
    private boolean isTakingPhoto = false;

    private final OkHttpClient httpClient = new OkHttpClient();

    private static final int LOW_LIGHT_THRESHOLD = 50;
    private static final int LOW_LIGHT_FRAME_LIMIT = 30;
    private static final int HIGH_LIGHT_THRESHOLD = 90;
    private static final long SUCCESS_COOLDOWN_MS = 4000; // sau khi nhận diện OK
    private static final long FAILURE_COOLDOWN_MS = 2500; // sau khi fail
    private long nextAllowedTs = 0L;

    // Yêu cầu “mặt rời khung” trước khi cho phép nhận diện lại
    private boolean requireFaceExit = false;
    private static final String TAG_FR = "FR_FLOW";
    private long attemptSeq = 0; // tăng dần mỗi lần chuẩn bị bắn API
    TextView alertTextView, labelName, nameTextView, labelCardId, cardIDTextView, labelSimilarity, similarityTextView, appTitle;
    ProgressBar loadingSpinner;
    private LinearLayout loadingContainer, userInfoPanel;
    private String currentCameraId = "0";
    private boolean canSwitchFrontToBack = true;
    private boolean canSwitchBackToFront = true;
    private int lowLightCount = 0;

    private long IDLE_DELAY_MS;
    private boolean isCountingDownToIdle = false;
    private final Handler idleHandler = new Handler(Looper.getMainLooper());
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Executor riêng cho ImageAnalysis & các tác vụ nặng
    private ExecutorService analyzerExecutor;
    private SoundPoolHelper sp;

    private static final String LED_RED = "/sys/class/leds/led4/brightness";
    private static final String LED_GREEN = "/sys/class/leds/led5/brightness";
    private static final String LED_WHITE = "/sys/class/leds/camera_white/brightness";
    private static final String[] LED_TRIGGERS = {
            "/sys/class/leds/camera_white/trigger",
            "/sys/class/leds/led4/trigger",
            "/sys/class/leds/led5/trigger",
            "/sys/class/leds/led7/trigger"  // nếu tồn tại
    };
    private View idleOverlay;
    private TextView idleClock;
    private boolean isIdle = false;
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            String t = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            idleClock.setText(t);
            clockHandler.postDelayed(this, 1000);
        }
    };

    // Cooldown chống rebind liên tục
    private static final long SWITCH_COOLDOWN_MS = 3000;
    private long lastSwitchTs = 0;
    private boolean kioskLocked = true;   // đang khoá app
    private int titleTapCount = 0;
    private Handler titleTapResetHandler = new Handler(Looper.getMainLooper());
    private Runnable titleTapReset = () -> titleTapCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        analyzerExecutor = Executors.newSingleThreadExecutor();

        if (!isInternetAvailable()) {
            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_LONG).show();
        }

        IDLE_DELAY_MS = getSharedPreferences("settings", MODE_PRIVATE).getInt("time_waiting", 8000);

        loadingContainer = findViewById(R.id.loadingContainer);
        userInfoPanel = findViewById(R.id.userInfoPanel);
        previewView = findViewById(R.id.previewView);
        graphicOverlay = findViewById(R.id.graphicOverlay);
        alertTextView = findViewById(R.id.labelAlert);
        labelName = findViewById(R.id.labelUserName);
        nameTextView = findViewById(R.id.userName);
        labelCardId = findViewById(R.id.labelUserCardId);
        cardIDTextView = findViewById(R.id.userCardId);
        labelSimilarity = findViewById(R.id.labelUserSimilarity);
        similarityTextView = findViewById(R.id.userSimilarity);
        loadingSpinner = findViewById(R.id.loadingSpinner);
        appTitle = findViewById(R.id.appTitle);
        idleOverlay = findViewById(R.id.idleOverlay);
        idleOverlay.bringToFront();
        applyImmersiveMode();
        sp = new SoundPoolHelper(getApplicationContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            idleOverlay.setElevation(100f);
        }
        idleOverlay.setClickable(true);
        idleOverlay.setFocusable(true);
        idleOverlay.setOnTouchListener((v, e) -> isIdle);
        idleClock = findViewById(R.id.idleClock);

        graphicOverlay.setCameraFacing(true); // camera trước

        String cameraId = getIntent().getStringExtra("camera_id");
        if (cameraId == null) cameraId = "0";

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera(cameraId);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        }
        View titleBar = findViewById(R.id.titleBar);
        titleBar.setOnClickListener(v -> {
            titleTapCount++;
            titleTapResetHandler.removeCallbacks(titleTapReset);
            titleTapResetHandler.postDelayed(titleTapReset, 1500); // 1.5s reset đếm

            if (titleTapCount >= 4) {
                titleTapCount = 0;
                showExitPinDialog();
            }
        });

        // 3) Bật khoá (Lock Task / Screen pinning). Nếu app chưa là Device Owner,
        //    startLockTask() sẽ chuyển sang screen pinning (người dùng sẽ thấy prompt lần đầu).
        enterKiosk();
    }
    private void enterKiosk() {
        kioskLocked = true;
        applyImmersiveMode();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startLockTask(); // Nếu đã whitelisted bởi DPM -> true kiosk; nếu không -> screen pinning
            }
        } catch (Exception ignored) {}
    }

    private void exitKiosk() {
        kioskLocked = false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                stopLockTask();
            }
        } catch (Exception ignored) {}
        // Tuỳ bạn: finish() hay về màn hình khác
        finish();
    }

    // Immersive full screen: ẩn status bar + nav bar (3 nút)
    private void applyImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            final View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }

    // Khi Activity lấy lại focus, re-apply immersive (tránh hệ thống tự hiện lại)
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersiveMode();
    }

    // Khoá Back khi đang kiosk
    @Override
    public void onBackPressed() {
        if (kioskLocked) {
            // bỏ trống để vô hiệu hoá Back
            return;
        }
        super.onBackPressed();
    }

    private void showExitPinDialog() {
        if (!kioskLocked) return;

        runOnUiThread(() -> {
            EditText pinInput = new EditText(this);
            pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            pinInput.setHint("Enter PIN");
            pinInput.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(8) });

            new AlertDialog.Builder(this)
                    .setTitle("Unlock App")
                    .setMessage("Enter PIN to exit")
                    .setView(pinInput)
                    .setCancelable(false)
                    .setPositiveButton("OK", (d, w) -> {
                        String pin = pinInput.getText() != null ? pinInput.getText().toString().trim() : "";
                        if ("0000".equals(pin)) {
                            exitKiosk();
                        } else {
                            Toast.makeText(this, "Wrong PIN", Toast.LENGTH_SHORT).show();
                            // Vẫn giữ kiosk, re-apply immersive
                            applyImmersiveMode();
                        }
                    })
                    .setNegativeButton("Cancel", (d, w) -> {
                        // vẫn khoá
                        applyImmersiveMode();
                    })
                    .show();
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (kioskLocked) {
            // chắc chắn đang ở chế độ khoá và ẩn thanh
            applyImmersiveMode();
            try { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) startLockTask(); } catch (Exception ignored) {}
        }
    }
    private final Runnable goIdleRunnable = this::enterIdle;

    private void enterIdle() {
        if (isIdle) return;
        isIdle = true;

        runOnUiThread(() -> {
            try {
                graphicOverlay.setWillNotDraw(true);
                previewView.animate().alpha(0f).setDuration(180).start();

                idleOverlay.setAlpha(0f);
                idleOverlay.setVisibility(View.VISIBLE);
                idleOverlay.animate().alpha(1f).setDuration(180).start();

                clockHandler.removeCallbacks(clockTick);
                clockHandler.post(clockTick);
            } catch (Exception ignore) {}
        });
    }
    private void ledExec(String sh) {
        analyzerExecutor.execute(() -> {
            try {
                Runtime.getRuntime().exec(new String[]{"sh","-c", sh});
            } catch (Exception ignored) {}
        });
    }

    private void ledDisableTriggers() {
        // cần root trên nhiều máy: "su 0 sh -c ..."
        analyzerExecutor.execute(() -> {
            for (String t : LED_TRIGGERS) {
                try {
                    Runtime.getRuntime().exec(new String[]{"sh","-c",
                            "[ -f "+t+" ] && echo none > "+t});
                } catch (Exception ignored) {}
            }
        });
    }

    private void ledOffAll() {
        ledExec("echo 0 > "+LED_WHITE+"; echo 0 > "+LED_RED+"; echo 0 > "+LED_GREEN);
    }

    // Bật xanh 2s rồi tắt
    private void flashGreen2s() {
        ledDisableTriggers();
        analyzerExecutor.execute(() -> {
            try {
                // tùy máy: một số cần mở trắng để nhìn rõ ánh xanh
                Runtime.getRuntime().exec(new String[]{"sh","-c",
                        "echo 100 > "+LED_WHITE+"; echo 0 > "+LED_RED+"; echo 1 > "+LED_GREEN});
            } catch (Exception ignored) {}
        });
        mainHandler.postDelayed(this::ledOffAll, 2000);
    }

    // Bật đỏ 2s rồi tắt
    private void flashRed2s() {
        ledDisableTriggers();
        analyzerExecutor.execute(() -> {
            try {
                Runtime.getRuntime().exec(new String[]{"sh","-c",
                        "echo 100 > "+LED_WHITE+"; echo 1 > "+LED_RED+"; echo 0 > "+LED_GREEN});
            } catch (Exception ignored) {}
        });
        mainHandler.postDelayed(this::ledOffAll, 2000);
    }
    private void exitIdle() {
        if (!isIdle) return;
        isIdle = false;

        runOnUiThread(() -> {
            try {
                previewView.animate().alpha(1f).setDuration(150).start();
                graphicOverlay.setWillNotDraw(false);
                graphicOverlay.invalidate();

                idleOverlay.animate().alpha(0f).setDuration(150)
                        .withEndAction(() -> idleOverlay.setVisibility(View.GONE))
                        .start();

                clockHandler.removeCallbacks(clockTick);
            } catch (Exception ignore) {}
        });
    }

    @Override
    public void onAverageLuminance(double luminance) {
        // Hysteresis + chỉ switch khi THỰC SỰ đổi camera
        if (currentCameraId.equals("0")) { // FRONT
            if (luminance < LOW_LIGHT_THRESHOLD && canSwitchFrontToBack) {
                lowLightCount++;
                if (lowLightCount >= LOW_LIGHT_FRAME_LIMIT) {
                    lowLightCount = 0;
                    canSwitchFrontToBack = false;
                    canSwitchBackToFront = true;
                    Log.d("Switch Camera", "0→1 (dark)");
                    switchCameraIfNeeded("1");
                }
            } else {
                lowLightCount = 0;
                if (!canSwitchFrontToBack && luminance > HIGH_LIGHT_THRESHOLD) {
                    canSwitchFrontToBack = true;
                }
            }
        } else { // BACK ("1")
            if (luminance > HIGH_LIGHT_THRESHOLD && canSwitchBackToFront) {
                lowLightCount++;
                if (lowLightCount >= LOW_LIGHT_FRAME_LIMIT) {
                    lowLightCount = 0;
                    canSwitchBackToFront = false;
                    canSwitchFrontToBack = true;
                    Log.d("Switch Camera", "1→0 (bright)");
                    switchCameraIfNeeded("0");
                }
            } else {
                lowLightCount = 0;
                if (!canSwitchBackToFront && luminance < LOW_LIGHT_THRESHOLD) {
                    canSwitchBackToFront = true;
                }
            }
        }
    }

    private void switchCameraIfNeeded(String newId) {
        if (newId.equals(currentCameraId)) return;
        long now = System.currentTimeMillis();
        if (now - lastSwitchTs < SWITCH_COOLDOWN_MS) return; // cooldown
        lastSwitchTs = now;
        startCamera(newId);
    }

    @Override
    public void onFaceAreaLarge(boolean isLarge) {
        runOnUiThread(() -> {
            if (isLarge) {
                if (isCountingDownToIdle) {
                    idleHandler.removeCallbacks(goIdleRunnable);
                    isCountingDownToIdle = false;
                }
                if (isIdle) exitIdle();
            } else {
                if (!isCountingDownToIdle && !isIdle) {
                    idleHandler.postDelayed(goIdleRunnable, IDLE_DELAY_MS);
                    isCountingDownToIdle = true;
                }
            }
        });
    }

    private void uploadImageToApi(File dummy) {
        // Giữ để tương thích nếu code khác còn gọi, nhưng KHÔNG dùng nữa.
        // Đường đi mới: uploadImageBytesToApi(...)
        showToastOnMainThread("Deprecated path used");
    }

    private void uploadImageBytesToApi(byte[] jpegBytes, String filename) {
        analyzerExecutor.execute(() -> {
            try {
                String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date());

                RequestBody requestBodyPrimary = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("image_file", filename,
                                RequestBody.create(jpegBytes, MediaType.parse("image/jpeg")))
                        .build();

                Request requestPrimary = new Request.Builder()
                        .url("http://10.13.32.51:5001/recognize-anti-spoofing")
                        .addHeader("X-API-Key", "vg_login_app")
                        .addHeader("X-Time", currentTime)
                        .post(requestBodyPrimary)
                        .build();

                OkHttpClient clientWithTimeout = httpClient.newBuilder()
                        .connectTimeout(3, TimeUnit.SECONDS)
                        .readTimeout(4, TimeUnit.SECONDS)
                        .writeTimeout(4, TimeUnit.SECONDS)
                        .callTimeout(6, TimeUnit.SECONDS)
                        .build();

                long startTime = System.currentTimeMillis();
                try (Response resp = clientWithTimeout.newCall(requestPrimary).execute()) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    String responseBody = resp.body() != null ? resp.body().string() : "";
                    Log.e("Response-Primary", responseBody);
                    Log.e("API-TIME", "Primary API time: " + elapsed + " ms");

                    if (!resp.isSuccessful()) {
                        if (resp.code() == 400) {
                            handleRecognitionFail();
                        } else {
                            showToastOnMainThread("API error: " + resp.code());
                        }
                        setTakingPhoto(false);
                        return;
                    }
                    handleApiJson(responseBody);
                    setTakingPhoto(false);
                } catch (Exception ex) {
                    Log.e("PrimaryAPI", "Primary API failed, fallback: " + ex.getMessage());
                    // Fallback
                    RequestBody requestBodyFallback = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("env_token", "8d59d8d588f84fc0a24291b8c36b6206")
                            .addFormDataPart("image_file", filename,
                                    RequestBody.create(jpegBytes, MediaType.parse("image/jpeg")))
                            .build();
                    Request requestFallback = new Request.Builder()
                            .url("http://10.1.16.23:8001/api/x/fr/env/face_search")
                            .post(requestBodyFallback)
                            .build();
                    long t1 = System.currentTimeMillis();
                    try (Response resp2 = httpClient.newCall(requestFallback).execute()) {
                        long elapsed2 = System.currentTimeMillis() - t1;
                        String responseBody2 = resp2.body() != null ? resp2.body().string() : "";
                        Log.e("Response-Fallback", responseBody2);
                        Log.e("API-TIME", "Fallback API time: " + elapsed2 + " ms");

                        if (!resp2.isSuccessful()) {
                            if (resp2.code() == 400) handleRecognitionFail();
                            else showToastOnMainThread("API error: " + resp2.code());
                            setTakingPhoto(false);
                            return;
                        }
                        handleApiJson(responseBody2);
                        setTakingPhoto(false);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                showToastOnMainThread("Error: " + e.getMessage());
                Log.e("FaceAPI_Error", e.getMessage(), e);
                setTakingPhoto(false);
            }
        });
    }

    private void handleApiJson(String responseBody) throws Exception {
        JSONObject jsonObject = new JSONObject(responseBody);
        if (jsonObject.optBoolean("is_fake", false)) {
            handleRecognitionFail();
            return;
        }

        if (jsonObject.optInt("is_recognized", 0) == 1) {
            String name = jsonObject.optString("name");
            String cardId = jsonObject.optString("id_string");
            double similarityVal = jsonObject.optDouble("similarity", 0) * 100;

            if (similarityVal <= 55) {
                handleRecognitionFail();
                return;
            }

            String similarity = String.format(Locale.US, "%.2f%%", similarityVal);
            User activeUser = new User(name, cardId, similarity);

            runOnUiThread(() -> {

                alertTextView.setText("Facial recognition successful");
                sp.playSoundBeep(sp.soundSuccess);
                flashGreen2s();   // ✅ xanh 2s
                labelName.setVisibility(View.VISIBLE);
                labelCardId.setVisibility(View.VISIBLE);
                labelSimilarity.setVisibility(View.VISIBLE);
                nameTextView.setVisibility(View.VISIBLE);
                cardIDTextView.setVisibility(View.VISIBLE);
                similarityTextView.setVisibility(View.VISIBLE);

                labelName.setText("Name:");
                nameTextView.setText(activeUser.getName());
                labelCardId.setText("Card ID:");
                cardIDTextView.setText(activeUser.getCardId());
                labelSimilarity.setText("Similarity:");
                similarityTextView.setText(activeUser.getSimilarity());

                userInfoPanel.setVisibility(View.VISIBLE);
                userInfoPanel.postDelayed(() -> userInfoPanel.setVisibility(View.GONE), 1500);
                nextAllowedTs = System.currentTimeMillis() + SUCCESS_COOLDOWN_MS;

                // Bật relay ngay, tắt sau 3s (không dùng sleep)
                turnRelayOnThenOff();
            });
        } else {
            runOnUiThread(() -> alertTextView.setText("Face not recognized"));
            handleRecognitionFail();
        }
    }

    private void turnRelayOnThenOff() {
        // Bật
        analyzerExecutor.execute(() -> {
            try {
                Runtime.getRuntime().exec(new String[]{"sh", "-c",
                        "H=$(cat /sys/class/leds/relay_ctl/max_brightness); echo $H > /sys/class/leds/relay_ctl/brightness"});
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        // Tắt sau 3s (không block)
        mainHandler.postDelayed(() -> analyzerExecutor.execute(() -> {
            try {
                Runtime.getRuntime().exec(new String[]{"sh", "-c",
                        "echo 0 > /sys/class/leds/relay_ctl/brightness"});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }), 3000);
    }

    private void handleRecognitionFail() {
        runOnUiThread(() -> {
            userInfoPanel.setVisibility(View.VISIBLE);
            alertTextView.setVisibility(View.VISIBLE);
            sp.playSoundBeep(sp.soundFailed);
            flashRed2s();     // ✅ đỏ 2s

            alertTextView.setText("Facial recognition failed");

            labelName.setVisibility(View.GONE);
            nameTextView.setVisibility(View.GONE);
            labelCardId.setVisibility(View.GONE);
            cardIDTextView.setVisibility(View.GONE);
            labelSimilarity.setVisibility(View.GONE);
            similarityTextView.setVisibility(View.GONE);

            loadingContainer.setVisibility(View.GONE);

            alertTextView.postDelayed(() -> alertTextView.setText(""), 2000);
            long now = System.currentTimeMillis();
            if (now < nextAllowedTs) {
                // đã có cooldown từ trước thì giữ
            } else {
                nextAllowedTs = now + FAILURE_COOLDOWN_MS;
            }
        });
    }

    private void showToastOnMainThread(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
    }

    private void startCamera(String cameraId) {
        // Guard: nếu đang đúng camera thì bỏ qua
        if (cameraId.equals(currentCameraId) && cameraProvider != null) {
            return;
        }
        currentCameraId = cameraId;
        isTakingPhoto = false;
        faceStraightStartTime = 0;

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                // Preview
                Preview.Builder pBuilder = new Preview.Builder();
                Preview preview = pBuilder.build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // ImageAnalysis
                ImageAnalysis.Builder aBuilder = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);
                ImageAnalysis imageAnalysis = aBuilder.build();
                imageAnalysis.setAnalyzer(analyzerExecutor, new FaceAnalyzer(this, graphicOverlay, this));

                int rotation;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    rotation = previewView.getDisplay() != null ? previewView.getDisplay().getRotation() : SurfaceRotationCompat.getDefaultRotation();
                } else {
                    Display d = getWindowManager().getDefaultDisplay();
                    rotation = d.getRotation();
                }

                // ImageCapture: chụp JPEG vào RAM, ưu tiên tốc độ
                ImageCapture.Builder icBuilder = new ImageCapture.Builder()
                        .setTargetRotation(rotation)
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setBufferFormat(ImageFormat.JPEG);

                try {
                    // Gợi ý JPEG_QUALITY để giảm size từ nguồn
                    Camera2Interop.Extender<ImageCapture> cExt = new Camera2Interop.Extender<>(icBuilder);
                    cExt.setCaptureRequestOption(CaptureRequest.JPEG_QUALITY, (byte) 80); // 60–85 tuỳ băng thông
                } catch (Exception ignore) {}

                imageCapture = icBuilder.build();

                // Chọn camera theo ID
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .addCameraFilter(cameraInfos -> {
                            for (CameraInfo info : cameraInfos) {
                                Camera2CameraInfo camera2Info = Camera2CameraInfo.from(info);
                                if (camera2Info.getCameraId().equals(cameraId)) {
                                    return Collections.singletonList(info);
                                }
                            }
                            return Collections.emptyList();
                        })
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        isTakingPhoto = false;
        faceStraightStartTime = 0;
    }

    @Override
    public void onFaceLookingStraight() {
        runOnUiThread(() -> {
            if (isTakingPhoto) {
                Log.d(TAG_FR, "Skip: isTakingPhoto=true");
                return;
            }

            long now = System.currentTimeMillis();

            // 1) Chưa tới giờ cho phép
            if (now < nextAllowedTs) {
                long remain = nextAllowedTs - now;
                Log.d(TAG_FR, "Blocked by cooldown. remain=" + remain + "ms, nextAllowedTs=" + nextAllowedTs);
                return;
            }

            // 2) Chưa thấy mặt rời khung kể từ lần thử trước
            if (requireFaceExit) {
                Log.d(TAG_FR, "Blocked by requireFaceExit=true (must see face exit event)");
                return;
            }

            if (faceStraightStartTime == 0) {
                faceStraightStartTime = now;
                Log.d(TAG_FR, "Face straight started at " + faceStraightStartTime);
            } else {
                long elapsed = now - faceStraightStartTime;
                Log.d(TAG_FR, "Face straight elapsed=" + elapsed + "ms");
                if (elapsed >= 500) { // ngưỡng của bạn
                    isTakingPhoto = true;
                    faceStraightStartTime = 0;

                    // Đặt trạng thái ngay lúc BẮT ĐẦU thử
                    requireFaceExit = true; // phải rời khung mới thử lại
                    nextAllowedTs = now + FAILURE_COOLDOWN_MS; // ít nhất đợi 1 nhịp (nếu success sẽ tăng thêm)

                    long attemptId = ++attemptSeq;
                    Log.d(TAG_FR, "BEGIN attempt#" + attemptId +
                            " set requireFaceExit=true, pre-cooldown=" + FAILURE_COOLDOWN_MS + "ms");

                    takePhoto();
                }
            }
        });
    }



    @Override
    public void onFaceNotLookingStraight() {
        runOnUiThread(() -> {
            faceStraightStartTime = 0;
            requireFaceExit = false; // đã rời khung -> bỏ yêu cầu
        });
    }

    private void takePhoto() {
        if (imageCapture == null) {
            setTakingPhoto(false);
            return;
        }

        imageCapture.takePicture(analyzerExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                try {
                    byte[] jpeg = imageToJpegBytes(image);
                    byte[] smallJpeg = downscaleJpeg(jpeg, 500, 80); // targetWidth=500px, quality=80
                    uploadImageBytesToApi(smallJpeg, "IMG_" + System.currentTimeMillis() + ".jpg");
                } catch (Exception e) {
                    e.printStackTrace();
                    setTakingPhoto(false);
                } finally {
                    image.close();
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                exception.printStackTrace();
                setTakingPhoto(false);
            }
        });
    }

    private byte[] imageToJpegBytes(ImageProxy image) {
        // Khi setBufferFormat=JPEG, plane[0] chứa trọn JPEG stream
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        if (planes == null || planes.length == 0) {
            // fallback (hiếm): trả về rỗng để fail an toàn
            return new byte[0];
        }
        ByteBuffer buf = planes[0].getBuffer();
        byte[] out = new byte[buf.remaining()];
        buf.get(out);
        return out;
    }

    private byte[] downscaleJpeg(byte[] jpeg, int targetW, int quality) throws Exception {
        // 1) đo bounds
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, bounds);
        int w = bounds.outWidth;

        // 2) tính inSampleSize theo bội số 2
        int inSample = 1;
        while ((w / inSample) > targetW * 1.2) inSample <<= 1;

        // 3) decode subsample
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = inSample;
        opts.inPreferredConfig = Bitmap.Config.RGB_565; // nhẹ RAM/CPU
        Bitmap bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, opts);
        if (bmp == null) throw new RuntimeException("Decode failed");

        // 4) scale tinh chỉnh nếu còn > targetW
        int finalW = bmp.getWidth();
        int finalH = bmp.getHeight();
        if (finalW > targetW) {
            finalH = (int) ((float) targetW / finalW * finalH);
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, targetW, finalH, true);
            if (scaled != bmp) bmp.recycle();
            bmp = scaled;
        }

        // 5) nén lại
        ByteArrayOutputStream baos = new ByteArrayOutputStream(64 * 1024);
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        bmp.recycle();
        return baos.toByteArray();
    }

    private void setTakingPhoto(boolean taking) {
        runOnUiThread(() -> isTakingPhoto = taking);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera("0");
        }
    }

    public boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                return capabilities != null &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (analyzerExecutor != null) {
            analyzerExecutor.shutdown();
        }
        clockHandler.removeCallbacks(clockTick);
        if (sp != null) sp.release();
    }

    private static class SurfaceRotationCompat {
        static int getDefaultRotation() {
            // fallback nếu không truy cập được display (hiếm)
            return 0;
        }
    }
}
