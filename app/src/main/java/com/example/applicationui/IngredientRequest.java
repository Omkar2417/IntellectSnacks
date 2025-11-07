package com.example.applicationui;

public class IngredientRequest {
    private String ingredients;

    public IngredientRequest(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }
}
