package com.weightloss.planner.service;

import com.weightloss.planner.model.Meal;
import com.weightloss.planner.model.PlanRequest;
import com.weightloss.planner.model.WeeklyPlan;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;

class PlannerServiceTest {
    private final PlannerService plannerService = new PlannerService();

    @Test
    void fishAvoidanceAlsoExcludesSpecificFishMeals() {
        PlanRequest request = requestWithAvoidance("fish");

        WeeklyPlan plan = plannerService.generate(request);

        plan.days().forEach(day -> day.meals().forEach(meal -> {
            String mealText = mealText(meal);
            assertFalse(mealText.contains("salmon"), "Salmon should be excluded when fish is avoided");
            assertFalse(mealText.contains("tuna"), "Tuna should be excluded when fish is avoided");
            assertFalse(mealText.contains("cod"), "Cod should be excluded when fish is avoided");
        }));
    }

    @Test
    void replacingAMealKeepsAvoidanceRules() {
        PlanRequest request = requestWithAvoidance("fish");
        WeeklyPlan plan = plannerService.generate(request);

        WeeklyPlan replaced = plannerService.replaceMeal(plan, 0, 2);

        replaced.days().forEach(day -> day.meals().forEach(meal -> {
            String mealText = mealText(meal);
            assertFalse(mealText.contains("salmon"));
            assertFalse(mealText.contains("tuna"));
            assertFalse(mealText.contains("cod"));
        }));
    }

    @Test
    void naturalLanguageAndAliasesAreAppliedToMealFiltering() {
        PlanRequest request = requestWithAvoidance("I am allergic to peanuts and dairy");

        WeeklyPlan plan = plannerService.generate(request);

        plan.days().forEach(day -> day.meals().forEach(meal -> {
            String mealText = mealText(meal);
            assertFalse(mealText.contains("peanut"));
            assertFalse(mealText.contains("yogurt"));
            assertFalse(mealText.contains("cheese"));
            assertFalse(mealText.contains("milk"));
        }));
    }

    private PlanRequest requestWithAvoidance(String foodsToAvoid) {
        PlanRequest request = new PlanRequest();
        request.setAge(30);
        request.setHeight(175);
        request.setWeight(80);
        request.setActivityLevel("Moderately active");
        request.setFitnessLevel("Beginner");
        request.setGoal("Lose weight");
        request.setWorkoutDays(3);
        request.setDiet("Omnivore");
        request.setCuisine("Any");
        request.setEquipment("None");
        request.setFoodsToAvoid(foodsToAvoid);
        return request;
    }

    private String mealText(Meal meal) {
        return (meal.name() + " " + meal.description() + " " + meal.ingredients()).toLowerCase(Locale.ROOT);
    }
}
