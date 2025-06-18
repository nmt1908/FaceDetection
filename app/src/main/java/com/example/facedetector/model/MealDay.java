package com.example.facedetector.model;

import java.util.List;

public class MealDay {
    private final String day;
    private final List<MealOption> options;

    public MealDay(String day, List<MealOption> options) {
        this.day = day;
        this.options = options;
    }

    public String getDay() {
        return day;
    }

    public List<MealOption> getOptions() {
        return options;
    }
}
