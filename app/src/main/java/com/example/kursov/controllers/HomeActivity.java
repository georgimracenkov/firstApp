package com.example.kursov.controllers;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kursov.R;
import com.example.kursov.tools.BackgroundMusicService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class HomeActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {
    private final PlayFragment playFragment = new PlayFragment();
    private final SettingsFragment settingsFragment = new SettingsFragment();
    private final LearnFragment learnFragment = new LearnFragment();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        bottomNavigationView.setOnItemSelectedListener(this);
        bottomNavigationView.setSelectedItemId(R.id.navigation_play);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.navigation_play:
                getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, playFragment).commit();
                return true;
            case R.id.navigation_settings:
                getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, settingsFragment).commit();
                return true;
            case R.id.navigation_learn:
                getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, learnFragment).commit();
                return true;
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        BackgroundMusicService.pausePlayer();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isBackgroundMusicServiceRunning()) {
            startService(new Intent(this, BackgroundMusicService.class));
        } else {
            BackgroundMusicService.resumePlayer();
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(HomeActivity.this).inflate(
                R.layout.layout_warning_dialog,
                findViewById(R.id.layoutDialogContainer)
        );
        builder.setView(view);
        ((TextView) view.findViewById(R.id.textTitle)).setText(R.string.exit_title);
        ((TextView) view.findViewById(R.id.textMessage)).setText(R.string.exit_text);
        Button yesBtn = view.findViewById(R.id.buttonYes);
        yesBtn.setText(getResources().getString(R.string.yesBtn));
        Button noBtn = view.findViewById(R.id.buttonNo);
        noBtn.setText(R.string.noBtn);

        yesBtn.setOnClickListener(v -> {
            finish();
            System.exit(0);
        });

        AlertDialog alertDialog = builder.create();

        noBtn.setOnClickListener(v -> alertDialog.dismiss());

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();
    }

    @SuppressWarnings("deprecation")
    private boolean isBackgroundMusicServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BackgroundMusicService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}

