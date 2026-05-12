import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;

// Main entry point of the application
import java.util.List;

public class Recipe {

    private String name;
    private Cuisine cuisine;
    private RecipeCategory category;
    private List<String> ingredients;
    private int prepTime;
    private int calories;
    private String instructions;
    private List<String> tags;
    private String link;
    private String imagePath;
    private boolean favorite;
    private int rating;

    public Recipe(String name,
                  Cuisine cuisine,
                  List<String> ingredients,
                  int prepTime,
                  int calories,
                  String instructions,
                  List<String> tags,
                  String link,
                  String imagePath) {

        this.name = name;
        this.cuisine = cuisine;
        this.category = RecipeCategory.MAIN_COURSE;
        this.ingredients = ingredients;
        this.prepTime = prepTime;
        this.calories = calories;
        this.instructions = instructions;
        this.tags = tags;
        this.link = link;
        this.imagePath = imagePath;
        this.favorite = false;
        this.rating = 0;
    }

    // ===== GETTERS =====

    public String getName() {
        return name;
    }

    public Cuisine getCuisine() {
        return cuisine;
    }

    public RecipeCategory getCategory() {
        return category;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public int getPrepTime() {
        return prepTime;
    }

    public int getCalories() {
        return calories;
    }

    public String getInstructions() {
        return instructions;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getLink() {
        return link;
    }

    public String getImagePath() {
        return imagePath;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public int getRating() {
        return rating;
    }

    // ===== SETTERS =====

    public void setCategory(RecipeCategory category) {
        this.category = category;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    // ===== UPDATE =====

    public void update(String name,
                       Cuisine cuisine,
                       List<String> ingredients,
                       int prepTime,
                       int calories,
                       String instructions,
                       List<String> tags,
                       String link,
                       String imagePath,
                       boolean favorite,
                       int rating) {

        this.name = name;
        this.cuisine = cuisine;
        this.ingredients = ingredients;
        this.prepTime = prepTime;
        this.calories = calories;
        this.instructions = instructions;
        this.tags = tags;
        this.link = link;
        this.imagePath = imagePath;
        this.favorite = favorite;
        this.rating = rating;
    }
    public String getFullDetails() {

        return "Name: " + name + "\n" +
                "Cuisine: " + cuisine + "\n" +
                "Category: " + category + "\n\n" +

                "Ingredients:\n" +
                String.join(", ", ingredients) + "\n\n" +

                "Preparation Time: " + prepTime + " mins\n" +
                "Calories: " + calories + " kcal\n\n" +

                "Tags: " + String.join(", ", tags) + "\n" +
                "Rating: " + rating + "/5\n" +

                (favorite ? "Marked as Favorite\n" : "") +

                "Link: " + link + "\n" +
                "Image: " + imagePath + "\n\n" +

                "Instructions:\n" + instructions;
    }
    @Override
    public String toString() {
        return name;
    }
}