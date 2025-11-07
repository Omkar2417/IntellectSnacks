package com.example.applicationui;
import java.util.Map;

public class AdditiveInfo {
    private String ingredient;
    private boolean matched;
    private String e_number;
    private Map<String, String> info;
    private String message;

    public String getIngredient() {
        return ingredient;
    }

    public boolean isMatched() {
        return matched;
    }

    public String getE_number() {
        return e_number;
    }

    public Map<String, String> getInfo() {
        return info;
    }

    public String getMessage() {
        return message;
    }
}
