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
            new Meal("Avocado tomato toast", "Breakfast", 340, "Whole-grain toast with avocado, tomato, lemon, and hemp seeds", "Vegan", "Mediterranean", "wheat, toast, avocado, tomato, hemp seeds"),
            new Meal("Cottage cheese peach crunch", "Breakfast", 370, "Cottage cheese, peaches, cinnamon, and sliced almonds", "Vegetarian", "Any", "dairy, cottage cheese, peaches, almonds"),
            new Meal("Peanut butter banana oats", "Breakfast", 410, "Warm oats with banana, peanut butter, and cinnamon", "Vegetarian", "Any", "oats, banana, peanuts, peanut butter"),
            new Meal("Berry chia pudding", "Breakfast", 320, "Chia seeds, berries, and milk topped with pumpkin seeds", "Vegetarian", "Any", "chia, berries, milk, pumpkin seeds"),
            new Meal("Black bean breakfast tacos", "Breakfast", 400, "Black beans, avocado, tomato salsa, and corn tortillas", "Vegan", "Mexican", "black beans, avocado, tomato, corn tortillas"),
            new Meal("Apple cinnamon quinoa porridge", "Breakfast", 330, "Quinoa, apple, cinnamon, raisins, and pumpkin seeds", "Vegan", "Any", "quinoa, apple, raisins, pumpkin seeds"),
            new Meal("Spinach mushroom breakfast burrito", "Breakfast", 430, "Eggs, spinach, mushrooms, salsa, and a whole-grain tortilla", "Vegetarian", "Mexican", "eggs, spinach, mushrooms, wheat tortilla, salsa"),
            new Meal("Smoked salmon breakfast plate", "Breakfast", 390, "Smoked salmon, cucumber, tomato, avocado, and a whole-grain bagel", "Omnivore", "Mediterranean", "salmon, cucumber, tomato, avocado, wheat, bagel"),
            new Meal("Chicken quinoa power bowl", "Lunch", 520, "Grilled chicken, quinoa, greens, and vegetables", "Omnivore", "Mediterranean", "chicken, quinoa, greens, vegetables"),
            new Meal("Turkey avocado wrap", "Lunch", 480, "Whole-grain wrap with turkey, avocado, lettuce, and tomato", "Omnivore", "Any", "turkey, avocado, wheat, lettuce, tomato"),
            new Meal("Chickpea Greek salad", "Lunch", 430, "Chickpeas, cucumber, tomato, olives, greens, and feta", "Vegetarian", "Mediterranean", "chickpeas, cucumber, tomato, olives, feta"),
            new Meal("Black bean burrito bowl", "Lunch", 510, "Black beans, brown rice, corn, salsa, and avocado", "Vegan", "Mexican", "black beans, rice, corn, salsa, avocado"),
            new Meal("Tuna white bean salad", "Lunch", 490, "Tuna, white beans, greens, tomatoes, cucumber, and lemon", "Omnivore", "Mediterranean", "tuna, fish, white beans, greens, tomato, cucumber"),
            new Meal("Lentil tabbouleh bowl", "Lunch", 450, "Lentils, bulgur, parsley, cucumber, tomato, and lemon", "Vegan", "Mediterranean", "lentils, bulgur, wheat, parsley, cucumber, tomato"),
            new Meal("Caprese white bean toast", "Lunch", 460, "White beans, mozzarella, tomato, basil, and whole-grain toast", "Vegetarian", "Mediterranean", "white beans, dairy, mozzarella, tomato, wheat, toast"),
            new Meal("Tofu soba vegetable bowl", "Lunch", 500, "Tofu, soba noodles, edamame, carrots, cabbage, and sesame", "Vegan", "Asian-inspired", "tofu, soy, soba, wheat, edamame, carrots, sesame"),
            new Meal("Chicken pesto salad", "Lunch", 510, "Chicken, greens, tomatoes, cucumber, pesto, and parmesan", "Omnivore", "Mediterranean", "chicken, greens, tomato, cucumber, dairy, parmesan, pine nuts"),
            new Meal("Egg salad lettuce cups", "Lunch", 410, "Egg salad with celery, Greek yogurt, mustard, and lettuce cups", "Vegetarian", "Any", "eggs, dairy, yogurt, celery, lettuce"),
            new Meal("Roasted vegetable hummus pita", "Lunch", 440, "Roasted vegetables, hummus, greens, and whole-wheat pita", "Vegan", "Mediterranean", "vegetables, chickpeas, sesame, hummus, wheat, pita"),
            new Meal("Turkey sweet potato chili", "Lunch", 530, "Lean turkey, sweet potato, tomatoes, kidney beans, and spices", "Omnivore", "Mexican", "turkey, sweet potato, tomato, kidney beans"),
            new Meal("Salmon sushi rice bowl", "Lunch", 540, "Salmon, brown rice, cucumber, avocado, edamame, and nori", "Omnivore", "Asian-inspired", "salmon, fish, rice, cucumber, avocado, edamame, soy"),
            new Meal("Mediterranean chicken pita", "Lunch", 500, "Chicken, hummus, cucumber, tomato, greens, and whole-grain pita", "Omnivore", "Mediterranean", "chicken, chickpeas, sesame, hummus, cucumber, tomato, wheat, pita"),
            new Meal("Salmon and roasted vegetables", "Dinner", 610, "Baked salmon, sweet potato, and seasonal vegetables", "Omnivore", "Mediterranean", "salmon, sweet potato, vegetables"),
            new Meal("Lentil vegetable curry", "Dinner", 560, "Lentils, vegetables, brown rice, and mild curry spices", "Vegan", "Asian-inspired", "lentils, vegetables, rice"),
            new Meal("Chicken fajita plate", "Dinner", 590, "Chicken, peppers, onions, black beans, and corn tortillas", "Omnivore", "Mexican", "chicken, peppers, onions, beans, corn"),
            new Meal("Vegetable pasta primavera", "Dinner", 540, "Whole-grain pasta with zucchini, tomato, spinach, and parmesan", "Vegetarian", "Mediterranean", "wheat, zucchini, tomato, spinach, parmesan"),
            new Meal("Turkey meatballs with polenta", "Dinner", 570, "Turkey meatballs, creamy polenta, marinara, and roasted zucchini", "Omnivore", "Mediterranean", "turkey, corn, dairy, tomato, zucchini"),
            new Meal("Beef vegetable stir-fry", "Dinner", 600, "Lean beef, broccoli, peppers, carrots, brown rice, and ginger", "Omnivore", "Asian-inspired", "beef, rice, broccoli, peppers, carrots, soy"),
            new Meal("Shrimp quinoa skillet", "Dinner", 560, "Shrimp, quinoa, zucchini, tomatoes, spinach, and lemon", "Omnivore", "Mediterranean", "shrimp, shellfish, quinoa, zucchini, tomato, spinach"),
            new Meal("Stuffed bell peppers", "Dinner", 550, "Bell peppers filled with lean beef, brown rice, tomatoes, and herbs", "Omnivore", "Any", "beef, rice, bell peppers, tomato"),
            new Meal("Chickpea coconut curry", "Dinner", 575, "Chickpeas, cauliflower, spinach, coconut milk, and brown rice", "Vegan", "Asian-inspired", "chickpeas, cauliflower, spinach, coconut, rice"),
            new Meal("Tofu teriyaki bowl", "Dinner", 570, "Tofu, broccoli, carrots, brown rice, and a light teriyaki glaze", "Vegan", "Asian-inspired", "tofu, soy, broccoli, carrots, rice, teriyaki"),
            new Meal("Lemon herb chicken and potatoes", "Dinner", 580, "Roasted chicken, baby potatoes, green beans, and lemon herbs", "Omnivore", "Mediterranean", "chicken, potatoes, green beans"),
            new Meal("Black bean enchilada skillet", "Dinner", 540, "Black beans, corn tortillas, peppers, tomato salsa, and avocado", "Vegan", "Mexican", "black beans, corn, corn tortillas, peppers, tomato, avocado"),
            new Meal("Cod tomato olive stew", "Dinner", 530, "Cod, tomatoes, white beans, olives, spinach, and herbs", "Omnivore", "Mediterranean", "cod, fish, tomato, white beans, olives, spinach"),
            new Meal("Mushroom walnut lentil loaf", "Dinner", 520, "Lentils, mushrooms, walnuts, oats, carrots, and herbs", "Vegan", "Any", "lentils, mushrooms, walnuts, oats, carrots"),
            new Meal("Chicken soba noodle soup", "Dinner", 510, "Chicken, soba noodles, mushrooms, spinach, carrots, and broth", "Omnivore", "Asian-inspired", "chicken, soba, wheat, mushrooms, spinach, carrots"),
            new Meal("Apple with peanut butter", "Snack", 210, "One apple with a tablespoon of peanut butter", "Vegetarian", "Any", "apple, peanuts"),
            new Meal("Cottage cheese and pineapple", "Snack", 190, "Cottage cheese with pineapple chunks", "Vegetarian", "Any", "dairy, pineapple"),
            new Meal("Carrots with hummus", "Snack", 160, "Fresh carrots with hummus", "Vegan", "Mediterranean", "carrots, chickpeas, sesame"),
            new Meal("Berry protein smoothie", "Snack", 230, "Milk or soy milk, berries, banana, and protein powder", "Vegetarian", "Any", "milk, soy, berries, banana"),
            new Meal("Edamame sea salt cup", "Snack", 180, "Steamed edamame with a pinch of sea salt and lemon", "Vegan", "Asian-inspired", "soy, edamame"),
            new Meal("Crispy roasted chickpeas", "Snack", 170, "Oven-roasted chickpeas with paprika and garlic", "Vegan", "Mediterranean", "chickpeas"),
            new Meal("Hard-boiled eggs and grapes", "Snack", 180, "Two hard-boiled eggs with fresh grapes", "Vegetarian", "Any", "eggs, grapes"),
            new Meal("Banana oat energy bites", "Snack", 220, "Oats, banana, peanut butter, chia, and cinnamon", "Vegetarian", "Any", "oats, banana, peanuts, peanut butter, chia"),
            new Meal("Avocado rice cakes", "Snack", 190, "Brown rice cakes topped with avocado, tomato, and lime", "Vegan", "Any", "rice, avocado, tomato"),
            new Meal("Greek yogurt cucumber dip", "Snack", 150, "Greek yogurt, cucumber, dill, and whole-grain crackers", "Vegetarian", "Mediterranean", "dairy, yogurt, cucumber, wheat, crackers"),
            new Meal("Pear with sunflower seed butter", "Snack", 200, "Sliced pear with sunflower seed butter", "Vegan", "Any", "pear, sunflower seeds"),
            new Meal("Trail mix and orange", "Snack", 240, "A small trail mix portion with an orange", "Vegan", "Any", "nuts, almonds, walnuts, seeds, raisins, orange")
    );

    private static final Map<String, Set<String>> AVOIDANCE_ALIASES = createAvoidanceAliases();

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
        Set<String> avoidedFoods = parseAvoidedFoods(request.getFoodsToAvoid());
        List<Meal> dietSafe = MEALS.stream()
                .filter(meal -> dietMatches(meal, diet))
                .filter(meal -> !containsAvoidedFood(meal, avoidedFoods))
                .collect(Collectors.toCollection(ArrayList::new));
        List<Meal> filtered = dietSafe.stream()
                .filter(meal -> "Any".equals(cuisine) || meal.cuisine().equals(cuisine) || meal.cuisine().equals("Any"))
                .collect(Collectors.toCollection(ArrayList::new));

        // Cuisine is a preference, but dietary restrictions and avoidance rules are hard constraints.
        // If the preference leaves a category empty, keep every safe meal so the plan remains complete.
        if (hasMissingCategory(filtered)) filtered = dietSafe;
        if (filtered.isEmpty() || hasMissingCategory(filtered)) {
            throw new IllegalArgumentException("No meals match your diet and foods-to-avoid selections. Try removing one restriction or adding more meal options.");
        }
        Collections.shuffle(filtered, random);
        return filtered;
    }

    private boolean hasMissingCategory(List<Meal> meals) {
        return List.of("Breakfast", "Lunch", "Dinner", "Snack").stream()
                .anyMatch(category -> meals.stream().noneMatch(meal -> meal.category().equals(category)));
    }

    private Set<String> parseAvoidedFoods(String rawAvoidedFoods) {
        if (rawAvoidedFoods == null || rawAvoidedFoods.isBlank()) return Set.of();

        String normalized = normalize(rawAvoidedFoods);
        Set<String> avoided = new LinkedHashSet<>();
        AVOIDANCE_ALIASES.keySet().stream()
                .filter(alias -> containsPhrase(normalized, alias))
                .forEach(avoided::add);

        // Also support free-form entries such as "mushrooms" or "bell peppers".
        for (String part : normalized.split(",|;|\\band\\b|\\bor\\b")) {
            String cleaned = part.replaceAll("\\b(i|am|allergic|allergy|avoid|avoiding|please|do|not|no|to|the|food|foods)\\b", " ").trim();
            if (!cleaned.isBlank()) {
                avoided.add(cleaned);
                Arrays.stream(cleaned.split("\\s+"))
                        .filter(token -> token.length() > 1)
                        .forEach(avoided::add);
            }
        }
        return avoided;
    }

    private boolean containsAvoidedFood(Meal meal, Set<String> avoidedFoods) {
        if (avoidedFoods.isEmpty()) return false;
        String searchable = normalize(String.join(" ", meal.name(), meal.description(), meal.ingredients()));
        return avoidedFoods.stream()
                .flatMap(food -> AVOIDANCE_ALIASES.getOrDefault(food, Set.of(food)).stream())
                .anyMatch(alias -> containsPhrase(searchable, alias));
    }

    private boolean containsPhrase(String text, String phrase) {
        String paddedText = " " + normalize(text) + " ";
        String paddedPhrase = " " + normalize(phrase) + " ";
        return paddedText.contains(paddedPhrase);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }

    private static Map<String, Set<String>> createAvoidanceAliases() {
        Map<String, Set<String>> aliases = new HashMap<>();
        addAliases(aliases, Set.of("fish", "fishes"), "fish", "fishes", "salmon", "tuna", "cod", "tilapia", "trout", "sardine", "sardines", "anchovy", "anchovies", "mackerel");
        addAliases(aliases, Set.of("seafood"), "seafood", "fish", "salmon", "tuna", "cod", "tilapia", "trout", "shrimp", "prawns", "crab", "lobster", "shellfish");
        addAliases(aliases, Set.of("shellfish"), "shellfish", "shrimp", "prawn", "prawns", "crab", "lobster");
        addAliases(aliases, Set.of("dairy", "milk", "lactose"), "dairy", "milk", "yogurt", "cheese", "butter", "whey", "cream", "parmesan", "feta", "mozzarella", "cottage");
        addAliases(aliases, Set.of("egg", "eggs"), "egg", "eggs", "mayonnaise");
        addAliases(aliases, Set.of("gluten", "wheat"), "gluten", "wheat", "barley", "rye", "flour", "bread", "pasta", "pita", "tortilla", "bagel", "couscous", "soba", "toast", "crackers");
        addAliases(aliases, Set.of("nut", "nuts", "tree nut", "tree nuts"), "nut", "nuts", "peanut", "peanuts", "almond", "almonds", "cashew", "cashews", "walnut", "walnuts", "pecan", "pecans", "pistachio", "pistachios", "trail mix");
        addAliases(aliases, Set.of("peanut", "peanuts", "peanut butter"), "peanut", "peanuts", "peanut butter");
        addAliases(aliases, Set.of("soy", "soya"), "soy", "soya", "tofu", "edamame", "teriyaki", "miso");
        addAliases(aliases, Set.of("meat"), "meat", "beef", "steak", "chicken", "turkey", "pork", "ham", "bacon");
        addAliases(aliases, Set.of("chicken", "poultry"), "chicken", "poultry");
        addAliases(aliases, Set.of("turkey"), "turkey");
        addAliases(aliases, Set.of("pork"), "pork", "ham", "bacon");
        addAliases(aliases, Set.of("beef"), "beef", "steak");
        addAliases(aliases, Set.of("bean", "beans", "legumes", "legume"), "bean", "beans", "chickpea", "chickpeas", "lentil", "lentils", "hummus", "peas");
        addAliases(aliases, Set.of("sesame", "tahini"), "sesame", "tahini", "hummus");
        addAliases(aliases, Set.of("coconut"), "coconut");
        addAliases(aliases, Set.of("corn"), "corn");
        addAliases(aliases, Set.of("avocado"), "avocado");
        return Collections.unmodifiableMap(aliases);
    }

    private static void addAliases(Map<String, Set<String>> aliases, Set<String> names, String... matches) {
        Set<String> values = Set.of(matches);
        names.forEach(name -> aliases.put(name, values));
    }

    private boolean dietMatches(Meal meal, String diet) {
        return "Omnivore".equals(diet) || meal.diet().equals(diet) || "Vegetarian".equals(diet) && "Vegan".equals(meal.diet()) || "Mediterranean".equals(diet) && meal.cuisine().equals("Mediterranean");
    }

    private Meal chooseMeal(List<Meal> available, String category, Set<String> used, Random random) {
        List<Meal> choices = available.stream().filter(meal -> meal.category().equals(category) && !used.contains(meal.name())).toList();
        if (choices.isEmpty()) choices = available.stream().filter(meal -> meal.category().equals(category)).toList();
        if (choices.isEmpty()) throw new IllegalStateException("No safe " + category.toLowerCase(Locale.ROOT) + " meals are available for this plan.");
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
        List<Meal> safeMeals = filteredMeals(plan.request(), new Random(ThreadLocalRandom.current().nextLong()));
        Set<String> mealsAlreadyOnDay = day.meals().stream().map(Meal::name).collect(Collectors.toSet());
        Meal replacement = safeMeals.stream()
                .filter(meal -> meal.category().equals(current.category()))
                .filter(meal -> !mealsAlreadyOnDay.contains(meal.name()))
                .findFirst()
                .orElseGet(() -> safeMeals.stream()
                        .filter(meal -> meal.category().equals(current.category()) && !meal.name().equals(current.name()))
                        .findFirst().orElse(current));
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
