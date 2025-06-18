package com.example.facedetector;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.facedetector.adapter.MealPagerAdapter;
import com.example.facedetector.model.MealDay;
import com.example.facedetector.model.MealOption;
import com.example.facedetector.model.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MealRegisterActivity extends AppCompatActivity {
    MaterialToolbar toolbar;
    Button btnSubmit;
    List<MealDay> mealDays;
    private List<MealDay> getWeeklyMeals() {
        List<MealDay> days = new ArrayList<>();

        days.add(new MealDay("Monday", Arrays.asList(
                new MealOption("Grilled Chicken",R.drawable.chickenfry),
                new MealOption("Fried Rice",R.drawable.ricefry),
                new MealOption("Vegetable Soup",R.drawable.canhrau),
                new MealOption("Tofu Stir Fry",R.drawable.tofustirfry),

                new MealOption("Pork Chop",R.drawable.porkchop),
                new MealOption("Noodles",R.drawable.noodle),
                new MealOption("Egg Salad",R.drawable.eggsalad),
                new MealOption("Beef Stew",R.drawable.beefstew)
        )));

        days.add(new MealDay("Tuesday", Arrays.asList(
                new MealOption("Pork Chop",R.drawable.porkchop),
                new MealOption("Noodles",R.drawable.noodle),
                new MealOption("Egg Salad",R.drawable.eggsalad),
                new MealOption("Beef Stew",R.drawable.beefstew)
        )));

        days.add(new MealDay("Wednesday", Arrays.asList(
                new MealOption("Fish Curry",R.drawable.fishcurry),
                new MealOption("Rice & Beans",R.drawable.ricebean),
                new MealOption("Pumpkin Soup",R.drawable.pumpkinsoup),
                new MealOption("Chicken Salad",R.drawable.chickensalad)
        )));

        days.add(new MealDay("Thursday", Arrays.asList(
                new MealOption("Spaghetti",R.drawable.spaghetti),
                new MealOption("Com Tam",R.drawable.comtam),
                new MealOption("Beef Pho",R.drawable.phobo),
                new MealOption("Beef Noodle",R.drawable.bunbo)
        )));

        days.add(new MealDay("Friday", Arrays.asList(
                new MealOption("Fried Rice Flour Cake",R.drawable.botchien),
                new MealOption("Vietnamese Crab Noodle Soup",R.drawable.bunrieu),
                new MealOption("Vietnamese Crispy Chicken Rice",R.drawable.comgaxoimo),
                new MealOption("Vietnamese Westside Savory Pancake",R.drawable.banhxeo)
        )));

        days.add(new MealDay("Saturday", Arrays.asList(
                new MealOption("Grilled Pork with Vermicelli",R.drawable.buntrhitnuong),
                new MealOption("Braised Pork with Eggs",R.drawable.thitkhohotvit),
                new MealOption("Vietnamese Sour Soup",R.drawable.canhchua),
                new MealOption("Vietnamese Sandwich",R.drawable.banhmi)
        )));

        return days;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_meal_register);
        User user = (User) getIntent().getSerializableExtra("activeUser");

        toolbar = findViewById(R.id.topAppBar);
        toolbar.setTitle("Register Meal");
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(MealRegisterActivity.this, MenuActivity.class);
            intent.putExtra("activeUser", user);
            startActivity(intent);
            finish();
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mealDays = getWeeklyMeals();
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        MealPagerAdapter adapter = new MealPagerAdapter(this, mealDays);
        viewPager.setAdapter(adapter);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(mealDays.get(position).getDay())
        ).attach();
        btnSubmit = findViewById(R.id.btnSubmit);
//        btnSubmit.setOnClickListener(v -> {
//            Map<String, String> selections = adapter.getSelectedMeals();
//            StringBuilder summary = new StringBuilder("Your selected meals:\n");
//            for (MealDay day : mealDays) {
//                String meal = selections.getOrDefault(day.getDay(), "No selection");
//                summary.append(day.getDay()).append(": ").append(meal).append("\n");
//            }
//
//            new androidx.appcompat.app.AlertDialog.Builder(this)
//                    .setTitle("Meal Summary")
//                    .setMessage(summary.toString())
//                    .setPositiveButton("OK", null)
//                    .show();
//        });
        btnSubmit.setOnClickListener(v -> {
            Map<String, String> selections = adapter.getSelectedMeals();

            // Inflate dialog layout chứa container
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_meal_list, null);
            LinearLayout container = dialogView.findViewById(R.id.containerMeals);

            List<MealDay> mealDays = getWeeklyMeals();

            for (MealDay day : mealDays) {
                String mealName = selections.getOrDefault(day.getDay(), "You haven't selected any items for this day");

                // Inflate item view cho từng món
                View itemView = getLayoutInflater().inflate(R.layout.item_meal_summary, container, false);

                ImageView imageViewMeal = itemView.findViewById(R.id.imageViewMeal);
                TextView textViewDay = itemView.findViewById(R.id.textViewDay);
                TextView textViewMeal = itemView.findViewById(R.id.textViewMeal);

                textViewDay.setText(day.getDay());
                textViewMeal.setText(mealName);

                // Tìm ảnh tương ứng với tên món trong danh sách ngày đó
                int imageRes = R.drawable.logo; // mặc định
                if (!mealName.equals("You haven't selected any items for this day")) {
                    for (MealOption option : day.getOptions()) {
                        if (option.getName().equals(mealName)) {
                            imageRes = option.getImageResId();
                            break;
                        }
                    }
                }
                imageViewMeal.setImageResource(imageRes);

                container.addView(itemView);
            }

            // Hiện dialog danh sách món đã đăng ký đầu tiên
            TextView customTitle = new TextView(this);
            customTitle.setText("List of registered meal");
            customTitle.setTextSize(30); // Cỡ chữ to hơn
            customTitle.setTypeface(null, Typeface.BOLD); // In đậm
            customTitle.setPadding(32, 24, 32, 24); // Padding cho đẹp
            customTitle.setTextColor(Color.BLACK); // Màu chữ
            customTitle.setGravity(Gravity.CENTER); // Căn giữa nếu muốn
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setCustomTitle(customTitle)
                    .setView(dialogView)
                    .setPositiveButton("OK", (dialog, which) -> {
                        // Khi OK thì hiện dialog xác nhận đăng ký
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Confirm registration")
                                .setMessage("Are you sure you want to register the selected items?")
                                .setPositiveButton("Agree", (confirmDialog, confirmWhich) -> {
                                    Intent intent = new Intent(MealRegisterActivity.this, MenuActivity.class);
                                    intent.putExtra("activeUser", user); // Truyền lại user
                                    startActivity(intent);
                                    finish();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    })
                    .show();
        });




    }

}