package com.weightloss.planner.model;

public record Meal(String name, String category, int calories, String description,
                   String diet, String cuisine, String ingredients) { }
