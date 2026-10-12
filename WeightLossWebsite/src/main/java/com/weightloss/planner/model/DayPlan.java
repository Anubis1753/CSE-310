package com.weightloss.planner.model;

import java.util.List;

public record DayPlan(String day, List<Meal> meals, Workout workout) { }
