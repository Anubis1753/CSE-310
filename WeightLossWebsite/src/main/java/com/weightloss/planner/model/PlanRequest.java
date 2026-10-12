package com.weightloss.planner.model;

public class PlanRequest {
    private int age;
    private double height;
    private double weight;
    private String activityLevel;
    private String fitnessLevel;
    private String goal;
    private int workoutDays;
    private String diet;
    private String cuisine;
    private String equipment;
    private String foodsToAvoid;

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }
    public String getFitnessLevel() { return fitnessLevel; }
    public void setFitnessLevel(String fitnessLevel) { this.fitnessLevel = fitnessLevel; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public int getWorkoutDays() { return workoutDays; }
    public void setWorkoutDays(int workoutDays) { this.workoutDays = workoutDays; }
    public String getDiet() { return diet; }
    public void setDiet(String diet) { this.diet = diet; }
    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }
    public String getEquipment() { return equipment; }
    public void setEquipment(String equipment) { this.equipment = equipment; }
    public String getFoodsToAvoid() { return foodsToAvoid; }
    public void setFoodsToAvoid(String foodsToAvoid) { this.foodsToAvoid = foodsToAvoid; }
}
