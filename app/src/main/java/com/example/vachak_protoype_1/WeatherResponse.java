package com.example.vachak_protoype_1;



import com.google.gson.annotations.SerializedName;

public class WeatherResponse {
    @SerializedName("weather")
    public Weather[] weather;

    @SerializedName("main")
    public Main main;

    public static class Weather {
        @SerializedName("description")
        public String description;
    }

    public static class Main {
        @SerializedName("temp")
        public float temp;
    }
}