package com.example.facedetector;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.appcompat.widget.Toolbar;

public class SettingActivity extends AppCompatActivity {

    private int faceAreaDetectionThreshold ;
    private int faceRecognitionThresholdArea ;

    private TextView tvFaceAreaDetectionThresholdValue;
    private TextView txtThreshold2;
    private TextView txtTimeWaiting;
    private int timeWaiting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Kích hoạt icon back trên toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // KHỞI TẠO VIEW TRƯỚC
        tvFaceAreaDetectionThresholdValue = findViewById(R.id.tvFaceAreaDetectionThresholdValue);
        txtThreshold2 = findViewById(R.id.txtThreshold2);
        txtTimeWaiting = findViewById(R.id.txtTimeWaiting);
        Button btnDecrease1 = findViewById(R.id.btnDecreaseFaceAreaDetectionThreshold);
        Button btnIncrease1 = findViewById(R.id.btnIncreaseFaceAreaDetectionThreshold);
        Button btnDecrease2 = findViewById(R.id.btnDecreaseFaceRecognitionThresholdArea);
        Button btnIncrease2 = findViewById(R.id.btnIncreaseFaceRecognitionThresholdArea);
        Button btnDecreaseTime = findViewById(R.id.btnDecreaseTimeWaiting); // 👈
        Button btnIncreaseTime = findViewById(R.id.btnIncreaseTimeWaiting);
        Button btnSave = findViewById(R.id.btnSave);

        // ĐỌC GIÁ TRỊ ĐÃ LƯU
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        faceAreaDetectionThreshold = prefs.getInt("detect_threshold", 12500);
        faceRecognitionThresholdArea = prefs.getInt("recognize_threshold", 27500);
        timeWaiting = prefs.getInt("time_waiting", 1000);

        // HIỂN THỊ GIÁ TRỊ ĐỌC ĐƯỢC LÊN UI
        tvFaceAreaDetectionThresholdValue.setText(String.valueOf(faceAreaDetectionThreshold));
        txtThreshold2.setText(String.valueOf(faceRecognitionThresholdArea));
        txtTimeWaiting.setText(String.valueOf(timeWaiting));

        // XỬ LÝ NÚT TĂNG GIẢM NGƯỠNG 1
        btnDecrease1.setOnClickListener(v -> {
            faceAreaDetectionThreshold = Math.max(7000, faceAreaDetectionThreshold - 500);
            tvFaceAreaDetectionThresholdValue.setText(String.valueOf(faceAreaDetectionThreshold));
        });

        btnIncrease1.setOnClickListener(v -> {
            faceAreaDetectionThreshold += 500;
            tvFaceAreaDetectionThresholdValue.setText(String.valueOf(faceAreaDetectionThreshold));
        });

        // XỬ LÝ NÚT TĂNG GIẢM NGƯỠNG 2
        btnDecrease2.setOnClickListener(v -> {
            faceRecognitionThresholdArea = Math.max(7000, faceRecognitionThresholdArea - 500);
            txtThreshold2.setText(String.valueOf(faceRecognitionThresholdArea));
        });

        btnIncrease2.setOnClickListener(v -> {
            faceRecognitionThresholdArea += 500;
            txtThreshold2.setText(String.valueOf(faceRecognitionThresholdArea));
        });
        btnDecreaseTime.setOnClickListener(v -> {
            timeWaiting = Math.max(2000, timeWaiting - 1000);
            txtTimeWaiting.setText(String.valueOf(timeWaiting));
        });

        btnIncreaseTime.setOnClickListener(v -> {
            timeWaiting += 1000;
            txtTimeWaiting.setText(String.valueOf(timeWaiting));
        });

        // XỬ LÝ NÚT LƯU
        btnSave.setOnClickListener(v -> {
            String message = "Ngưỡng diện tích phát hiện mặt: " + faceAreaDetectionThreshold +
                    "\nNgưỡng diện tích nhận diện mặt: " + faceRecognitionThresholdArea+
            "\nNgưỡng thời gian chờ: " + timeWaiting;

            getSharedPreferences("settings", MODE_PRIVATE)
                    .edit()
                    .putInt("detect_threshold", faceAreaDetectionThreshold)
                    .putInt("recognize_threshold", faceRecognitionThresholdArea)
                    .putInt("time_waiting", timeWaiting)
                    .apply();

            new AlertDialog.Builder(SettingActivity.this)
                    .setTitle("Giá trị đã lưu")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

}
