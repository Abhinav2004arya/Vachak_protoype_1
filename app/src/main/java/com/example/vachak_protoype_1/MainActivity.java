package com.example.vachak_protoype_1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Locale;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MainActivity extends AppCompatActivity {


    private static final int SPEECH_REQUEST_CODE = 100;
    private static final int CALL_PERMISSION_REQUEST_CODE = 101;
    private static final int CONTACTS_PERMISSION_REQUEST_CODE = 102;
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
        private static final String API_KEY = "6fb002068ad87c6f5cb975c8c49a0ecd";

    private TextToSpeech textToSpeech;
    private TextView responseText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton voiceButton = findViewById(R.id.voiceButton);
        responseText = findViewById(R.id.responseText);

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
            }
        });

        // Request necessary permissions
        requestPermissions();

        // Set button click listener for voice recognition
        voiceButton.setOnClickListener(v -> startVoiceRecognition());
    }

    private void requestPermissions() {
        // Request Contacts and Call permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.CALL_PHONE
            }, CONTACTS_PERMISSION_REQUEST_CODE);
        }
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String query = results.get(0);
                processCommand(query);
            }
        }
    }

    private void processCommand(String query) {
//        query = query.toLowerCase();

        if (query.toLowerCase().contains("call")) {
            String contactName = extractContactName(query);
            if (contactName != null) {
                makePhoneCall(contactName);
            } else {
                respond("I couldn't identify the contact name.");
            }
        }else if (query.toLowerCase().contains("weather")) {
            String[] words = query.split(" ");
            String city = "delhi"; // Default city

            // Look for a city name in the query
            for (String word : words) {
                if (Character.isUpperCase(word.charAt(0)) && word.length() > 2) {
                    city = word;
                    break;
                }
            }

            fetchWeather(city);
        }
        else {
            respond("I didn't understand.");
        }



    }


    private void fetchWeather(String city) {
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
                    respond(weatherInfo);
                } else {
                    respond("Failed to fetch weather data.");
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                respond("Error: " + t.getMessage());
            }
        });
    }


    private String extractContactName(String query) {
        String[] words = query.split(" ");
        for (int i = 0; i < words.length; i++) {
            if (words[i].equalsIgnoreCase("call") && i + 1 < words.length) {
                return words[i + 1]; // Returns the name after "call"
            }
        }
        return null;
    }

    private void makePhoneCall(String contactName) {
        String phoneNumber = getPhoneNumber(contactName);
        if (phoneNumber != null) {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(callIntent);
            } else {
                respond("Call permission is not granted.");
            }
        } else {
            respond("Contact not found.");
        }
    }

    private String getPhoneNumber(String name) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, CONTACTS_PERMISSION_REQUEST_CODE);
            return null;
        }

        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };

        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
        String[] selectionArgs = { "%" + name + "%" }; // Search for any match

        Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, null);

        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

            String contactName = cursor.getString(nameIndex);
            String phoneNumber = cursor.getString(numberIndex);

            cursor.close();
            Log.d("ContactFound", "Name: " + contactName + ", Number: " + phoneNumber);
            return phoneNumber; // Return the first found number
        }

        if (cursor != null) cursor.close();
        return null; // No contact found
    }

    private void respond(String message) {
        responseText.setText(message);
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}




//package com.example.vachak_protoype_1;
//
//
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.speech.RecognizerIntent;
//import android.speech.tts.TextToSpeech;
//
//import android.view.View;
//import android.widget.Button;
//import android.widget.ImageButton;
//import android.widget.TextView;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//
//import java.util.ArrayList;
//import java.util.Locale;
//
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//import retrofit2.Retrofit;
//import retrofit2.converter.gson.GsonConverterFactory;
//
//public class MainActivity extends AppCompatActivity {
//    private static final int SPEECH_REQUEST_CODE = 100;
//    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
//    private static final String API_KEY = "6fb002068ad87c6f5cb975c8c49a0ecd";
//
//    private TextToSpeech textToSpeech;
//    private TextView responseText;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        ImageButton voiceButton = findViewById(R.id.voiceButton);
//        responseText = findViewById(R.id.responseText);
//
//        // Initialize Text-to-Speech
//        textToSpeech = new TextToSpeech(this, status -> {
//            if (status == TextToSpeech.SUCCESS) {
//                textToSpeech.setLanguage(Locale.US);
//            }
//        });
//
//        // Set button click listener for voice recognition
//        voiceButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startVoiceRecognition();
//            }
//        });
//    }
//
//    private void startVoiceRecognition() {
//        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
//        startActivityForResult(intent, SPEECH_REQUEST_CODE);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
//            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//            if (results != null && !results.isEmpty()) {
//                String query = results.get(0);
//                processCommand(query);
//            }
//        }
//    }
//
//
//
//    private void processCommand(String query) {
//        if (query.toLowerCase().contains("weather")) {
//            String[] words = query.split(" ");
//            String city = "delhi"; // Default city
//
//            // Look for a city name in the query
//            for (String word : words) {
//                if (Character.isUpperCase(word.charAt(0)) && word.length() > 2) {
//                    city = word;
//                    break;
//                }
//            }
//
//            fetchWeather(city);
//        } else {
//            respond("I didn't understand. Try asking for the weather in a specific city.");
//        }
//    }
//
//
//    private void fetchWeather(String city) {
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl(BASE_URL)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//
//        WeatherAPI api = retrofit.create(WeatherAPI.class);
//        Call<WeatherResponse> call = api.getWeather(city, API_KEY, "metric");
//
//        call.enqueue(new Callback<WeatherResponse>() {
//            @Override
//            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    String weatherInfo = "Weather in " + city + ": " +
//                            response.body().weather[0].description + ", " +
//                            response.body().main.temp + "°C";
//                    respond(weatherInfo);
//                } else {
//                    respond("Failed to fetch weather data.");
//                }
//            }
//
//            @Override
//            public void onFailure(Call<WeatherResponse> call, Throwable t) {
//                respond("Error: " + t.getMessage());
//            }
//        });
//    }
//
//    private void respond(String message) {
//        responseText.setText(message);
//        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
//    }
//
//    @Override
//    protected void onDestroy() {
//        if (textToSpeech != null) {
//            textToSpeech.stop();
//            textToSpeech.shutdown();
//        }
//        super.onDestroy();
//    }
//}