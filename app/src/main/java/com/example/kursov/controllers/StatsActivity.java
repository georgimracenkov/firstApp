package com.example.kursov.controllers;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kursov.R;
import com.example.kursov.models.User;
import com.example.kursov.tools.BackgroundMusicService;
import com.example.kursov.tools.Tag;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class StatsActivity extends AppCompatActivity {
    private ProgressBar hiScoreProgressBar;
    private TextView hiScoreTextView;
    private int score;
    private final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_stats);
        BackgroundMusicService.resumePlayer();

        hiScoreProgressBar = findViewById(R.id.statsHiScoreProgressBar);
        TextView scoreTextView = findViewById(R.id.statsScoreTextView);
        hiScoreTextView = findViewById(R.id.statsPersonalBest);
        score = getIntent().getExtras().getInt("currentSessionScore");
        Button continueBtn = findViewById(R.id.continueStatsButton);
        getHiScore();

        scoreTextView.setText(String.valueOf(score));
        continueBtn.setOnClickListener(view -> {
            startActivity(new Intent(StatsActivity.this, HomeActivity.class));
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(StatsActivity.this, HomeActivity.class));
        finish();
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

    private void getHiScore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = "";

        if (user != null) {
            email = user.getEmail();
        }

        databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = null;
                String key = null;
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    user = dataSnapshot.getValue(User.class);
                    key = dataSnapshot.getKey();
                }

                if (user != null) {
                    int hiScore = user.getScore();

                    if (score > hiScore) {
                        if (key != null) {
                            HashMap<String, Object> update = new HashMap<>();
                            update.put("score", score);
                            databaseReference.child(key).updateChildren(update);
                            hiScoreTextView.setText(String.valueOf(score));
                            hiScoreProgressBar.setVisibility(View.GONE);
                        }
                    } else {
                        hiScoreTextView.setText(String.valueOf(user.getScore()));
                        hiScoreProgressBar.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(Tag.FIREBASE_ON_CANCELLED, error.getDetails() + "| " + error.getMessage());
            }
        });
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

