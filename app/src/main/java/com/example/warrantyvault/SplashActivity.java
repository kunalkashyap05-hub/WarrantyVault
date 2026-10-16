package com.example.warrantyvault;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View splashRoot = findViewById(R.id.splashRoot);
        UiUtils.setupEdgeToEdge(this, splashRoot);

        View cardLogo = findViewById(R.id.cardLogo);
        View tvAppName = findViewById(R.id.tvAppName);
        View tvTagline = findViewById(R.id.tvTagline);

        UiUtils.animateEntrance(cardLogo, 100);
        UiUtils.animateEntrance(tvAppName, 200);
        UiUtils.animateEntrance(tvTagline, 300);

        // Delay for 1.5 seconds then check SharedPreferences session
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SharedPreferences preferences = getSharedPreferences("WarrantyVaultSession", MODE_PRIVATE);
            boolean loggedIn = preferences.getBoolean("loggedIn", false);
            long userId = preferences.getLong("userId", -1);

            if (loggedIn && userId != -1) {
                // User is logged in via SQLite session, go to Dashboard
                Intent intent = new Intent(SplashActivity.this, DashboardActivity.class);
                startActivity(intent);
            } else {
                // User not logged in, go to Login
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
            }
            finish();
        }, 1500);
    }
}
