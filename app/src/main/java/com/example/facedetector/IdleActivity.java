package com.example.facedetector;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class IdleActivity extends AppCompatActivity implements FaceAnalyzer.FaceDetectionListener {

    private PreviewView previewView;
    private FaceGraphicOverlay graphicOverlay;
    private ProcessCameraProvider cameraProvider;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isReturningToMain = false;
    private TextView txtClock;
    String cameraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idle);
        txtClock =findViewById(R.id.txtClock);
        previewView = findViewById(R.id.previewView);
        graphicOverlay = findViewById(R.id.graphicOverlay);
        graphicOverlay.setCameraFacing(true); // camera trước
        cameraId = getIntent().getStringExtra("camera_id");
        if (cameraId == null) {
            cameraId = "0";
        }
        handler.post(updateTimeRunnable);
        startCamera(cameraId);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateTimeRunnable);
    }
    private final Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            String currentTime = sdf.format(new Date());
            txtClock.setText(currentTime);
            handler.postDelayed(this, 1000); // cập nhật mỗi giây
        }
    };
//    private void startCamera() {
//        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
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
//
//                imageAnalysis.setAnalyzer(
//                        ContextCompat.getMainExecutor(this),
//                        new FaceAnalyzer(this, null, this) // truyền đủ 3 tham số
//                );
//
//
//                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
//
//                cameraProvider.unbindAll();
//                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
//
//            } catch (ExecutionException | InterruptedException e) {
//                e.printStackTrace();
//            }
//        }, ContextCompat.getMainExecutor(this));
//    }
private void startCamera(String cameraId) {
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

    cameraProviderFuture.addListener(() -> {
        try {
            cameraProvider = cameraProviderFuture.get();

            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

            imageAnalysis.setAnalyzer(
                    ContextCompat.getMainExecutor(this),
                    new FaceAnalyzer(this, graphicOverlay, this)
            );

            // Chọn camera dựa theo cameraId được truyền vào
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
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }, ContextCompat.getMainExecutor(this));
}

    @Override
    public void onAverageLuminance(double luminance) {
        // Xử lý độ sáng trung bình ở đây
    }
    @Override
    public void onFaceAreaLarge(boolean isLarge) {
//        Log.d("IdleActivity", "onFaceAreaLarge called with isLarge = " + isLarge);
//        showToastOnMainThread("IDLE onFaceAreaLarge called");
        runOnUiThread(() -> {
            if (isLarge && !isReturningToMain) {
                isReturningToMain = true;
                runOnUiThread(() -> {
                    Intent intent = new Intent(IdleActivity.this, MainActivity.class);
                    intent.putExtra("camera_id", cameraId);
                    startActivity(intent);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), 500);

                });
            }
        });

    }

    private void showToastOnMainThread(String message) {
        runOnUiThread(() -> Toast.makeText(IdleActivity.this, message, Toast.LENGTH_LONG).show());
    }
    @Override
    public void onFaceLookingStraight() {
        // Không cần xử lý
    }

    @Override
    public void onFaceNotLookingStraight() {
        // Không cần xử lý
    }
}
