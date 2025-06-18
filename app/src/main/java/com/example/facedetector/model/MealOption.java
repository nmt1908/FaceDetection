package com.example.facedetector.model;

import java.util.List;

public class MealOption {
    private final String name;
    private final int imageResId;  // resource ID ảnh
    private List<MealOption> meals;
    public MealOption(String name, int imageResId) {
        this.name = name;
        this.imageResId = imageResId;
    }

    public String getName() {
        return name;
    }

    public int getImageResId() {
        return imageResId;
    }
    public List<MealOption> getMeals() {
        return meals;
    }
}

