package com.example.kursov.controllers;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.example.kursov.R;
import com.example.kursov.models.Information;
import com.example.kursov.tools.BackgroundMusicService;
import com.example.kursov.tools.Tag;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class InformationActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private TextView titleText;
    private TextView informationText;
    private ImageSlider imageSlider;
    private CardView imageSliderCardView;
    private final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Information");

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information);
        BackgroundMusicService.resumePlayer();

        ScrollView informationScrollView = findViewById(R.id.informationActivityScrollView);
        progressBar = findViewById(R.id.infoProgressbar);
        titleText = findViewById(R.id.informationTitle);
        informationText = findViewById(R.id.informationText);
        imageSlider = findViewById(R.id.imageSlider);
        imageSliderCardView = findViewById(R.id.imageSliderCardView);

        informationText.setMovementMethod(new ScrollingMovementMethod());
        getInformation(getIntent().getStringExtra("title"));

        informationScrollView.setOnTouchListener((v, event) -> {
            informationText.getParent().requestDisallowInterceptTouchEvent(false);
            return false;
        });

        informationText.setOnTouchListener((v, event) -> {
            informationText.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
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

    private void getInformation(String title) {
        databaseReference.orderByChild("title").equalTo(title).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Information information = null;

                for (DataSnapshot dataSnapshot: snapshot.getChildren()) {
                    information = dataSnapshot.getValue(Information.class);
                }

                if (information != null) {
                    titleText.setText(information.getTitle());
                    informationText.setText(information.getInformationText());

                    List<SlideModel> slideModels = new ArrayList<>();

                    for (String imageUrl : information.getImageUrls()) {
                        slideModels.add(new SlideModel(imageUrl, null, ScaleTypes.FIT));
                    }

                    imageSlider.setImageList(slideModels);

                    progressBar.setVisibility(View.GONE);
                    titleText.setVisibility(View.VISIBLE);
                    informationText.setVisibility(View.VISIBLE);
                    imageSliderCardView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(Tag.FIREBASE_ON_CANCELLED, error.getDetails() + " | " + error.getMessage());
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
