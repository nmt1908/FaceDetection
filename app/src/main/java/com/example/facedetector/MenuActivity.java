package com.example.facedetector;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.facedetector.model.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MenuActivity extends AppCompatActivity {
    Button btnSettings;
    User newUser;
    Button btnRegisterMeal, btnRegisterLeave, btnExit;
    private TextView txtGreeting,txtClock;
    String cameraId;
    private final Handler handler = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        newUser = (User) getIntent().getSerializableExtra("activeUser");
         cameraId = getIntent().getStringExtra("camera_id");
        if (cameraId == null) {
            cameraId = "0";
        }
        if (newUser == null) {
            Toast.makeText(this, "Please identify your face", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(MenuActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return; // Dừng không chạy tiếp
        }
        btnSettings = findViewById(R.id.btnSettings);
        if ("047409".equals(newUser.getCardId())) {
            btnSettings.setVisibility(View.VISIBLE);
        } else {
            btnSettings.setVisibility(View.GONE);
        }



        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, SettingActivity.class);
            startActivity(intent);
        });



        btnRegisterMeal = findViewById(R.id.btnRegisterMeal);
        btnRegisterLeave = findViewById(R.id.btnRegisterLeave);
        btnExit = findViewById(R.id.btnExit);
        txtGreeting =findViewById(R.id.txtGreeting);
        txtClock = findViewById(R.id.txtClock);
        handler.post(updateTimeRunnable);
        btnRegisterMeal.setOnClickListener(v -> {
//            startActivity(new Intent(...));
            Intent intent = new Intent(MenuActivity.this, MealRegisterActivity.class);
            intent.putExtra("activeUser", newUser);
            startActivity(intent);

        });

        btnRegisterLeave.setOnClickListener(v -> {
            Toast.makeText(this, "Register Leave clicked", Toast.LENGTH_SHORT).show();
        });

        btnExit.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, MainActivity.class);
            intent.putExtra("camera_id", cameraId);
            startActivity(intent);

            finish(); // kết thúc MenuActivity
        });


        // Lấy dữ liệu từ Intent

        txtGreeting.setText("Hello, "+newUser.getName());


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
}