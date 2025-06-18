package com.example.facedetector.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.example.facedetector.R;
import com.example.facedetector.model.MealOption;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MealDayFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MealDayFragment extends Fragment {

    private int selectedIndex = -1;
    private static final String ARG_MEALS = "meals";
    private static final String ARG_DAY = "day";

    private List<MealOption> meals;
    private String day;

    public String getDay() {
        return day;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
    }

    public static MealDayFragment newInstance(List<MealOption> meals, String day) {
        MealDayFragment fragment = new MealDayFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DAY, day);
        args.putSerializable(ARG_MEALS, new ArrayList<>(meals));
        fragment.setArguments(args);
        return fragment;
    }

    public String getSelectedMeal() {
        if (selectedIndex >= 0 && selectedIndex < meals.size()) {
            return meals.get(selectedIndex).getName();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            day = getArguments().getString(ARG_DAY);
            meals = (List<MealOption>) getArguments().getSerializable(ARG_MEALS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_meal_day, container, false);
        RadioGroup group = view.findViewById(R.id.mealRadioGroup);

        group.setOnCheckedChangeListener((radioGroup, checkedId) -> selectedIndex = checkedId);

        for (int i = 0; i < meals.size(); i++) {
            View itemView = inflater.inflate(R.layout.item_meal_option, group, false);

            RadioButton rb = itemView.findViewById(R.id.radioButton);
            ImageView iv = itemView.findViewById(R.id.imageView);
            TextView tv = itemView.findViewById(R.id.textViewMealName);

            rb.setId(i);
            tv.setText(meals.get(i).getName());
            iv.setImageResource(meals.get(i).getImageResId());

            itemView.setOnClickListener(v -> group.check(rb.getId()));

            group.addView(itemView);
        }

        if (selectedIndex >= 0 && selectedIndex < meals.size()) {
            group.check(selectedIndex);
        }

        return view;
    }
}