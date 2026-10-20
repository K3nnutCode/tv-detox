package com.ansub.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

/**
 * Weather for the opt-in Home Photos package only.
 * Fixed Bandar Seri Begawan coordinates: no device location, tracking SDK or analytics.
 * The only network request made by Home Photos is this HTTPS forecast request.
 * Weather data: Open-Meteo.com (CC BY 4.0), for personal non-commercial use.
 */
final class WeatherWidget extends LinearLayout {
    private static final String TAG = "HomePhotosWeather";
    private static final String ENDPOINT = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=4.9031&longitude=114.9398"
            + "&current=temperature_2m,weather_code"
            + "&timezone=Asia%2FBrunei";
    private static final long REFRESH_MS = 30 * 60 * 1000L;
    private static final String PREFS = "homePhotosWeather";

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final TextView degrees;
    private final TextView description;
    private final SharedPreferences cache;
    private boolean started;
    private int generation;

    WeatherWidget(Context context, float scale) {
        super(context);
        cache = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        setOrientation(VERTICAL);
        setGravity(Gravity.END);
        setPadding(size(26, scale), size(20, scale), size(26, scale), size(18, scale));
        setFocusable(false);
        setClickable(false);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x99000000);
        bg.setCornerRadius(size(28, scale));
        setBackground(bg);

        TextView city = line(context, "BANDAR SERI BEGAWAN", 19, 0xD1FFFFFF, scale);
        addView(city);
        degrees = line(context, "--°C", 60, Color.WHITE, scale);
        degrees.setGravity(Gravity.END);
        addView(degrees);
        description = line(context, "Weather unavailable", 24, 0xEEFFFFFF, scale);
        addView(description);
        TextView attribution = line(context, "Weather: Open-Meteo.com", 16, 0xB0FFFFFF, scale);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-2, -2);
        ap.topMargin = size(8, scale);
        addView(attribution, ap);
        showCached();
    }

    private static int size(int px, float scale) { return Math.max(1, Math.round(px * scale)); }

    private static TextView line(Context context, String value, int fontSize, int color, float scale) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextColor(color);
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, size(fontSize, scale));
        text.setGravity(Gravity.END);
        text.setSingleLine(true);
        text.setIncludeFontPadding(false);
        return text;
    }

    void start() {
        if (started) return;
        started = true;
        ++generation;
        showCached();
        fetchAsync(generation);
    }

    void stop() {
        started = false;
        ++generation; // In-flight results cannot update a hidden home screen.
        ui.removeCallbacksAndMessages(null);
    }

    private void showCached() {
        if (cache.contains("temp")) {
            degrees.setText(String.format(Locale.US, "%.0f°C", cache.getFloat("temp", 0f)));
            description.setText(label(cache.getInt("code", -1)) + " · saved");
        } else {
            degrees.setText("--°C");
            description.setText("Weather unavailable");
        }
    }

    private void fetchAsync(final int token) {
        new Thread(new Runnable() {
            @Override public void run() {
                double temp = Double.NaN;
                int weatherCode = -1;
                try {
                    HttpsURLConnection connection = (HttpsURLConnection) new URL(ENDPOINT).openConnection();
                    try {
                        connection.setRequestMethod("GET");
                        connection.setConnectTimeout(7000);
                        connection.setReadTimeout(7000);
                        connection.setUseCaches(false);
                        if (connection.getResponseCode() != 200) {
                            throw new java.io.IOException("HTTP " + connection.getResponseCode());
                        }
                        StringBuilder body = new StringBuilder();
                        char[] buffer = new char[2048];
                        try (InputStream input = connection.getInputStream();
                             Reader reader = new InputStreamReader(input, "UTF-8")) {
                            int length;
                            while ((length = reader.read(buffer)) >= 0) {
                                body.append(buffer, 0, length);
                                if (body.length() > 65536) throw new java.io.IOException("Response too large");
                            }
                        }
                        JSONObject current = new JSONObject(body.toString()).getJSONObject("current");
                        temp = current.getDouble("temperature_2m");
                        weatherCode = current.getInt("weather_code");
                        if (Double.isNaN(temp) || Double.isInfinite(temp) || temp < -100 || temp > 100) {
                            throw new java.io.IOException("Invalid temperature");
                        }
                    } finally {
                        connection.disconnect();
                    }
                } catch (Exception e) {
                    temp = Double.NaN;
                    Log.w(TAG, "Weather fetch failed (offline or provider unavailable)", e);
                }
                final double result = temp;
                final int code = weatherCode;
                ui.post(new Runnable() {
                    @Override public void run() {
                        if (!started || token != generation) return;
                        if (!Double.isNaN(result)) {
                            cache.edit().putFloat("temp", (float) result)
                                    .putInt("code", code).apply();
                            degrees.setText(String.format(Locale.US, "%.0f°C", result));
                            description.setText(label(code));
                        } else {
                            showCached();
                        }
                        ui.postDelayed(new Runnable() {
                            @Override public void run() { if (started && token == generation) fetchAsync(token); }
                        }, REFRESH_MS);
                    }
                });
            }
        }, "HomePhotosWeather").start();
    }

    private static String label(int code) {
        if (code == 0) return "Clear sky";
        if (code >= 1 && code <= 3) return "Partly cloudy";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 51 && code <= 57) return "Drizzle";
        if (code >= 61 && code <= 67) return "Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Rain showers";
        if (code >= 85 && code <= 86) return "Snow showers";
        if (code >= 95) return "Thunderstorms";
        return "Weather";
    }
}
