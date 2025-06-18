package com.example.facedetector.adapter;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.facedetector.fragment.MealDayFragment;
import com.example.facedetector.model.MealDay;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MealPagerAdapter extends FragmentStateAdapter {
    private final List<MealDay> mealDays;
    private final Map<Integer, MealDayFragment> fragmentMap = new HashMap<>();
    public MealPagerAdapter(@NonNull FragmentActivity fragmentActivity, List<MealDay> mealDays) {
        super(fragmentActivity);
        this.mealDays = mealDays;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        MealDay day = mealDays.get(position);
        MealDayFragment fragment = MealDayFragment.newInstance(day.getOptions(), day.getDay());
        fragmentMap.put(position, fragment);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return mealDays.size();
    }
    public Map<String, String> getSelectedMeals() {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < mealDays.size(); i++) {
            MealDayFragment fragment = fragmentMap.get(i);
            if (fragment != null) {
                String selected = fragment.getSelectedMeal();
                if (selected != null) {
                    result.put(fragment.getDay(), selected);
                }
            }
        }
        return result;
    }

}
