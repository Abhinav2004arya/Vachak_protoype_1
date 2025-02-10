package com.example.vachak_protoype_1;

import android.util.Log;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WeatherHelper {
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String API_KEY = "6fb002068ad87c6f5cb975c8c49a0ecd";
    private WeatherCallback callback;

    public interface WeatherCallback {
        void onWeatherResponse(String weatherInfo);
        void onFailure(String errorMessage);
    }

    public WeatherHelper(WeatherCallback callback) {
        this.callback = callback;
    }

    public void fetchWeather(String city) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherAPI api = retrofit.create(WeatherAPI.class);
        Call<WeatherResponse> call = api.getWeather(city, API_KEY, "metric");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String weatherInfo = "Weather in " + city + ": " +
                            response.body().weather[0].description + ", " +
                            response.body().main.temp + "°C";
                    callback.onWeatherResponse(weatherInfo);
                } else {
                    callback.onFailure("Failed to fetch weather data.");
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                callback.onFailure("Error: " + t.getMessage());
            }
        });
    }
}
