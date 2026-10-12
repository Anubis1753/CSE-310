# Personalized Meal and Workout Planner

## Project overview

This project will be a Java web application that creates personalized weekly
meal and workout plans based on a user's information, preferences, available
equipment, and fitness goals.

The first release will generate a plan, display a preview, and export the
finished plan as a downloadable PDF. It will not require an account or login.
Plans will be temporary and will not be saved permanently in the first
release.

The application is intended to provide general health and fitness guidance. It
is not a substitute for medical advice from a qualified professional.

## Instructions for Build and Use

### Planned build process

The application will be built as a Java web application. The planned
development stack is:

- Java
- Spring Boot or a similar Java web framework
- A database for meals, recipes, exercises, and nutrition data
- A PDF-generation library
- A browser-based user interface

The exact framework, database, and PDF library will be selected during
implementation.

### Planned user workflow

1. Open the web application.
2. Enter the requested profile and fitness information.
3. Select a goal, dietary preferences, and the desired number of workout days.
4. Generate a weekly meal and workout plan.
5. Review the plan in the browser.
6. Replace individual meals or exercises if desired.
7. Download the completed plan as a PDF.

The first release will not require an account or login. Generated plans will be
temporary and will not be saved permanently.

## Development Environment

Development will require:

- A Java Development Kit (JDK)
- An IDE such as IntelliJ IDEA or Visual Studio Code
- A build tool such as Maven or Gradle
- A database suitable for local development
- A modern web browser

Specific version requirements will be documented once the implementation stack
has been selected.

## Supported goals

The plan should adapt to the user's selected goal:

- Lose weight
- Maintain weight
- Gain muscle

Meal recommendations should reflect the selected nutrition goal, while workout
recommendations should reflect the selected fitness goal. The design should
also allow additional goals to be added later.

## First-version user inputs

The plan-generation form should collect and validate:

- Age
- Height
- Current weight
- Activity level
- Fitness level
- Primary goal
- Number of workout days per week
- Dietary restrictions and allergies
- Preferred foods and foods to avoid
- Preferred meal types or cuisines
- Available workout equipment
- Workout preferences

The interface should use separate dropdown menus where appropriate, including
goal, activity level, fitness level, workout frequency, diet, cuisine, and
equipment. Multi-select controls may be used for restrictions, preferences,
foods to avoid, and available equipment.

## Meal-plan features

Each generated weekly plan should include:

- Meals for every day of the week
- Meal categories such as breakfast, lunch, dinner, and snacks
- Suggested portions
- Estimated calories and relevant nutrition information
- Recipes and preparation instructions
- A weekly grocery list
- Filters for dietary restrictions, cuisine, meal type, and calorie range
- The ability to replace an individual meal without regenerating the entire
  plan

## Workout-plan features

The workout schedule should be generated for the number of days selected by the
user. Each workout should include:

- Workout focus based on the user's goal
- Exercises appropriate for the user's fitness level
- Equipment requirements
- Sets, repetitions, duration, and rest periods where applicable
- Warm-up and cool-down guidance
- The ability to replace an individual exercise

Examples of goal-specific behavior include weight-loss-oriented workouts for a
weight-loss goal and resistance-training plans that support muscle gain for a
muscle-building goal.

## Plan-generation approach

The recommended approach is a curated meal and exercise database combined with
rules-based recommendation logic. This should provide a more reliable and
consistent experience than randomly generating plans.

The generator should:

1. Validate the user's inputs.
2. Estimate appropriate nutrition targets from the provided profile and goal.
3. Select meals that match the nutrition targets and user preferences.
4. Select workouts that match the goal, schedule, fitness level, and equipment.
5. Assemble the weekly plan.
6. Allow individual meals or exercises to be replaced.
7. Render the final plan as a downloadable PDF.

## Planned architecture

The Java backend should be organized into separate responsibilities so future
account features can be added without redesigning the plan generator:

- **Web interface** — collects user inputs, displays a plan preview, and
  provides download controls.
- **Input and validation layer** — validates the profile and plan settings.
- **Plan-generation layer** — creates and updates weekly meal and workout
  plans.
- **Meal and recipe data layer** — stores meals, recipes, nutrition data, and
  filtering attributes.
- **Exercise data layer** — stores exercises, muscle groups, difficulty,
  equipment, and workout attributes.
- **PDF-generation layer** — converts the completed plan into a downloadable
  PDF.
- **Future account layer** — reserved for authentication, saved plans,
  preferences, and progress history.

The application should use a database-backed data model for meals, recipes,
exercises, and plan-generation rules. This keeps the content maintainable and
allows the database to grow without rewriting the generator.

## First-release workflow

1. The user opens the web application.
2. The user completes the profile and plan-preference form.
3. The application validates the submitted information.
4. The application generates a weekly meal and workout plan.
5. The user reviews the plan in the browser.
6. The user replaces any unwanted meal or exercise.
7. The user downloads the completed plan as a PDF.

## Future work

The first release will not persist user data. Future versions may add:

- [ ] User accounts and login
- [ ] Saved plans and plan history
- [ ] Saved user preferences
- [ ] Progress and weight tracking
- [ ] Plan adjustments based on progress
- [ ] Mobile-friendly enhancements or a mobile application

These additions should build on the existing plan-generation services rather
than changing their core responsibilities.

## Current status

Project planning and requirements definition. Implementation has not started.
