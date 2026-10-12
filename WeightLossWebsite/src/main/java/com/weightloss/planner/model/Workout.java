package com.weightloss.planner.model;

import java.util.List;

public record Workout(String name, String focus, String details, String equipment,
                      List<Exercise> exercises, String warmup, String cooldown, int totalMinutes) { }
