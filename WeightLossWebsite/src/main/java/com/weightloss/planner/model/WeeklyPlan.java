package com.weightloss.planner.model;

import java.util.List;

public record WeeklyPlan(PlanRequest request, int dailyCalorieTarget, List<DayPlan> days) { }
