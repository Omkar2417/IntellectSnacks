package com.example.applicationui;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface AdditiveApiService {
    @Headers("Content-Type: application/json")
    @POST("api/check_ingredients")
    Call<List<AdditiveInfo>> checkIngredients(@Body IngredientRequest request);
}
