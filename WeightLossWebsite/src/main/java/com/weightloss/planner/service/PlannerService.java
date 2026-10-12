package com.weightloss.planner.service;

import com.weightloss.planner.model.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class PlannerService {
    private static final List<String> DAYS = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
    private static final List<Meal> MEALS = List.of(
            new Meal("Greek yogurt berry bowl", "Breakfast", 360, "Greek yogurt, berries, oats, and chia seeds", "Vegetarian", "Any", "yogurt, berries, oats, chia"),
            new Meal("Veggie egg scramble", "Breakfast", 390, "Eggs, spinach, peppers, mushrooms, and whole-grain toast", "Vegetarian", "Any", "eggs, spinach, peppers, mushrooms, toast"),
            new Meal("Overnight oats with banana", "Breakfast", 350, "Oats, milk, banana, cinnamon, and pumpkin seeds", "Vegetarian", "Any", "oats, milk, banana, pumpkin seeds"),
            new Meal("Tofu breakfast stir-fry", "Breakfast", 380, "Tofu, peppers, spinach, and brown rice", "Vegan", "Asian-inspired", "tofu, peppers, spinach, rice"),
            new Meal("Chicken quinoa power bowl", "Lunch", 520, "Grilled chicken, quinoa, greens, and vegetables", "Omnivore", "Mediterranean", "chicken, quinoa, greens, vegetables"),
            new Meal("Turkey avocado wrap", "Lunch", 480, "Whole-grain wrap with turkey, avocado, lettuce, and tomato", "Omnivore", "Any", "turkey, avocado, wheat, lettuce, tomato"),
            new Meal("Chickpea Greek salad", "Lunch", 430, "Chickpeas, cucumber, tomato, olives, greens, and feta", "Vegetarian", "Mediterranean", "chickpeas, cucumber, tomato, olives, feta"),
            new Meal("Black bean burrito bowl", "Lunch", 510, "Black beans, brown rice, corn, salsa, and avocado", "Vegan", "Mexican", "black beans, rice, corn, salsa, avocado"),
            new Meal("Salmon and roasted vegetables", "Dinner", 610, "Baked salmon, sweet potato, and seasonal vegetables", "Omnivore", "Mediterranean", "salmon, sweet potato, vegetables"),
            new Meal("Lentil vegetable curry", "Dinner", 560, "Lentils, vegetables, brown rice, and mild curry spices", "Vegan", "Asian-inspired", "lentils, vegetables, rice"),
            new Meal("Chicken fajita plate", "Dinner", 590, "Chicken, peppers, onions, black beans, and corn tortillas", "Omnivore", "Mexican", "chicken, peppers, onions, beans, corn"),
            new Meal("Vegetable pasta primavera", "Dinner", 540, "Whole-grain pasta with zucchini, tomato, spinach, and parmesan", "Vegetarian", "Mediterranean", "wheat, zucchini, tomato, spinach, parmesan"),
            new Meal("Apple with peanut butter", "Snack", 210, "One apple with a tablespoon of peanut butter", "Vegetarian", "Any", "apple, peanuts"),
            new Meal("Cottage cheese and pineapple", "Snack", 190, "Cottage cheese with pineapple chunks", "Vegetarian", "Any", "dairy, pineapple"),
            new Meal("Carrots with hummus", "Snack", 160, "Fresh carrots with hummus", "Vegan", "Mediterranean", "carrots, chickpeas, sesame"),
            new Meal("Berry protein smoothie", "Snack", 230, "Milk or soy milk, berries, banana, and protein powder", "Vegetarian", "Any", "milk, soy, berries, banana")
    );

    public WeeklyPlan generate(PlanRequest request) {
        validate(request);
        int target = "Maintain weight".equals(request.getGoal()) ? 2200 : "Gain muscle".equals(request.getGoal()) ? 2500 : 1800;
        Random random = new Random(ThreadLocalRandom.current().nextLong());
        List<Meal> available = filteredMeals(request, random);
        Set<String> usedMeals = new HashSet<>();

        List<DayPlan> days = DAYS.stream().map(day -> {
            int index = DAYS.indexOf(day);
            List<Meal> meals = List.of(
                    chooseMeal(available, "Breakfast", usedMeals, random),
                    chooseMeal(available, "Lunch", usedMeals, random),
                    chooseMeal(available, "Dinner", usedMeals, random),
                    chooseMeal(available, "Snack", usedMeals, random));
            Workout workout = index < request.getWorkoutDays() ? buildWorkout(index, request, random) : recoveryWorkout();
            return new DayPlan(day, meals, workout);
        }).toList();
        return new WeeklyPlan(request, target, days);
    }

    private List<Meal> filteredMeals(PlanRequest request, Random random) {
        String diet = Optional.ofNullable(request.getDiet()).orElse("Omnivore");
        String cuisine = Optional.ofNullable(request.getCuisine()).orElse("Any");
        String avoid = Optional.ofNullable(request.getFoodsToAvoid()).orElse("").toLowerCase(Locale.ROOT);
        List<Meal> filtered = MEALS.stream()
                .filter(meal -> dietMatches(meal, diet))
                .filter(meal -> "Any".equals(cuisine) || meal.cuisine().equals(cuisine) || meal.cuisine().equals("Any"))
                .filter(meal -> Arrays.stream(avoid.split(","))
                        .map(String::trim).filter(item -> !item.isBlank())
                        .noneMatch(item -> meal.ingredients().toLowerCase(Locale.ROOT).contains(item)))
                .collect(Collectors.toCollection(ArrayList::new));
        if (filtered.size() < 4) {
            filtered = MEALS.stream().filter(meal -> dietMatches(meal, diet)).filter(meal ->
                    Arrays.stream(avoid.split(",")).map(String::trim).filter(item -> !item.isBlank())
                            .noneMatch(item -> meal.ingredients().toLowerCase(Locale.ROOT).contains(item)))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        for (String category : List.of("Breakfast", "Lunch", "Dinner", "Snack")) {
            if (filtered.stream().noneMatch(meal -> meal.category().equals(category))) {
                MEALS.stream().filter(meal -> meal.category().equals(category) && dietMatches(meal, diet))
                        .filter(meal -> Arrays.stream(avoid.split(",")).map(String::trim).filter(item -> !item.isBlank())
                                .noneMatch(item -> meal.ingredients().toLowerCase(Locale.ROOT).contains(item)))
                        .findFirst().ifPresent(filtered::add);
            }
        }
        Collections.shuffle(filtered, random);
        return filtered;
    }

    private boolean dietMatches(Meal meal, String diet) {
        return "Omnivore".equals(diet) || meal.diet().equals(diet) || "Vegetarian".equals(diet) && "Vegan".equals(meal.diet()) || "Mediterranean".equals(diet) && meal.cuisine().equals("Mediterranean");
    }

    private Meal chooseMeal(List<Meal> available, String category, Set<String> used, Random random) {
        List<Meal> choices = available.stream().filter(meal -> meal.category().equals(category) && !used.contains(meal.name())).toList();
        if (choices.isEmpty()) choices = available.stream().filter(meal -> meal.category().equals(category)).toList();
        if (choices.isEmpty()) choices = MEALS.stream().filter(meal -> meal.category().equals(category)).toList();
        Meal selected = choices.get(random.nextInt(choices.size()));
        used.add(selected.name());
        return selected;
    }

    private Workout buildWorkout(int dayIndex, PlanRequest request, Random random) {
        String equipment = Optional.ofNullable(request.getEquipment()).orElse("None");
        boolean beginner = "Beginner".equals(request.getFitnessLevel());
        if (dayIndex % 2 == 0) {
            String squatMachine = equipment.equals("Full gym") ? "Leg press machine" : equipment.equals("Home basics") ? "Dumbbells" : "No machine - bodyweight";
            String rowMachine = equipment.equals("Full gym") ? "Seated cable row" : equipment.equals("Home basics") ? "Resistance band or dumbbells" : "No machine - bodyweight row substitute";
            List<Exercise> exercises = new ArrayList<>(List.of(
                    new Exercise("Squat", squatMachine, 3, beginner ? "8-10 reps" : "10-12 reps", 60, "Controlled descent; keep knees aligned with toes"),
                    new Exercise("Push-up or chest press", equipment.equals("Full gym") ? "Chest press machine" : "No machine required", 3, beginner ? "6-10 reps" : "8-12 reps", 60, "Use an easier variation if needed"),
                    new Exercise("Row", rowMachine, 3, "10-12 reps", 60, "Pull toward the ribs and squeeze the shoulder blades"),
                    new Exercise("Plank", "Exercise mat", 3, beginner ? "20-30 seconds" : "30-45 seconds", 45, "Keep hips level and brace the core")
            ));
            Collections.shuffle(exercises, random);
            return new Workout("Strength and conditioning", request.getGoal(), "3 sets per exercise with 45-60 seconds rest", equipment, exercises,
                    "5 minutes of brisk walking and dynamic leg/arm movements", "5 minutes of easy walking and full-body stretching", 40);
        }
        String cardioMachine = equipment.equals("Full gym") ? "Treadmill, bike, or elliptical" : equipment.equals("Home basics") ? "Stationary bike or jump rope" : "No machine - brisk outdoor walk";
        List<Exercise> exercises = new ArrayList<>(List.of(
                new Exercise("Moderate cardio", cardioMachine, 1, "20 minutes", 0, "Maintain a pace where conversation is possible"),
                new Exercise("Reverse lunge", "No machine required", 3, "10 reps per leg", 45, "Hold a support for balance if needed"),
                new Exercise("Mountain climber", "Exercise mat", 3, "20 total reps", 45, "Move slowly enough to keep the hips stable"),
                new Exercise("Dead bug", "Exercise mat", 3, "8 reps per side", 45, "Keep the lower back gently against the mat")
        ));
        Collections.shuffle(exercises, random);
        return new Workout("Cardio and mobility", request.getGoal(), "Cardio interval followed by 3 rounds of mobility and core work", equipment, exercises,
                "5 minutes of easy cardio and dynamic stretches", "5 minutes of walking and gentle stretching", 35);
    }

    private Workout recoveryWorkout() {
        List<Exercise> exercises = List.of(new Exercise("Easy walk", "No machine required", 1, "20-30 minutes", 0, "Comfortable pace"),
                new Exercise("Full-body stretching", "Exercise mat", 1, "8-10 minutes", 0, "Hold each stretch gently; do not bounce"));
        return new Workout("Recovery day", "Rest and recovery", "Light movement to support recovery", "None", exercises,
                "2 minutes of easy walking", "Optional relaxed breathing", 30);
    }

    public WeeklyPlan replaceMeal(WeeklyPlan plan, int dayIndex, int mealIndex) {
        if (dayIndex < 0 || dayIndex >= plan.days().size() || mealIndex < 0 || mealIndex >= plan.days().get(dayIndex).meals().size()) return plan;
        DayPlan day = plan.days().get(dayIndex);
        Meal current = day.meals().get(mealIndex);
        Meal replacement = MEALS.stream().filter(meal -> !meal.name().equals(current.name())).findFirst().orElse(current);
        ArrayList<Meal> meals = new ArrayList<>(day.meals());
        meals.set(mealIndex, replacement);
        ArrayList<DayPlan> days = new ArrayList<>(plan.days());
        days.set(dayIndex, new DayPlan(day.day(), meals, day.workout()));
        return new WeeklyPlan(plan.request(), plan.dailyCalorieTarget(), days);
    }

    private void validate(PlanRequest request) {
        if (request.getAge() < 13 || request.getAge() > 100) throw new IllegalArgumentException("Age must be between 13 and 100.");
        if (request.getHeight() <= 0 || request.getWeight() <= 0) throw new IllegalArgumentException("Height and weight must be positive.");
        if (request.getWorkoutDays() < 1 || request.getWorkoutDays() > 7) throw new IllegalArgumentException("Workout days must be between 1 and 7.");
    }
}
