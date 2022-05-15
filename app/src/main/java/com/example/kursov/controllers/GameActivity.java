package com.example.kursov.controllers;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.kursov.R;
import com.example.kursov.models.Answer;
import com.example.kursov.models.Question;
import com.example.kursov.tools.BackgroundMusicService;
import com.example.kursov.tools.Constants;
import com.example.kursov.tools.Tag;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GameActivity extends AppCompatActivity {
    private TextView scoreTextView;
    private TextView questionTextView;
    private TextView timerTextView;
    private Button answerOne;
    private Button answerTwo;
    private Button answerThree;
    private Button answerFour;
    private ProgressBar progressBar;
    private TextView progressCounter;
    private int score = 0;
    private int questionIndex = 0;
    private final List<Question> questions = new ArrayList<>();
    private Thread play;
    private CountDownTimer cdt;
    private final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Questions");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_loading_screen);
        progressBar = findViewById(R.id.progressBarLoadingScreen);
        progressCounter = findViewById(R.id.progressCounter);
        getAllQuestions();
        BackgroundMusicService.resumePlayer();

        play = new Thread(() -> {
            scoreTextView = findViewById(R.id.stageTextView);
            questionTextView = findViewById(R.id.questionTextView);
            timerTextView = findViewById(R.id.timerTextView);
            answerOne = findViewById(R.id.answerOne);
            answerTwo = findViewById(R.id.answerTwo);
            answerThree = findViewById(R.id.answerThree);
            answerFour = findViewById(R.id.answerFour);

            setFields(questions.get(questionIndex));

            answerOne.setOnClickListener(view -> submitAnswer(questions.get(questionIndex), 0, answerOne));

            answerTwo.setOnClickListener(view -> submitAnswer(questions.get(questionIndex), 1, answerTwo));

            answerThree.setOnClickListener(view -> submitAnswer(questions.get(questionIndex), 2, answerThree));

            answerFour.setOnClickListener(view -> submitAnswer(questions.get(questionIndex), 3, answerFour));
        });
    }

    @Override
    public void onBackPressed() {
        showStopDialog();
    }

    @Override
    public void onPause() {
        super.onPause();
        BackgroundMusicService.pausePlayer();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isBackgroundMusicServiceRunning()) {
            startService(new Intent(this, BackgroundMusicService.class));
        } else {
            BackgroundMusicService.resumePlayer();
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void submitAnswer(Question question, int answerIndex, Button button) {
        setButtonsToNotClickable();

        Answer answer = question.getAnswers().get(answerIndex);

        if (answer.isTrue()) {
            button.setBackground(getDrawable(R.drawable.button_answer_correct));
            score += 100;
        } else {
            button.setBackground(getDrawable(R.drawable.button_answer_wrong));
        }

        questionIndex++;

        new Handler(getMainLooper()).postDelayed(() -> {
            button.setBackground(getDrawable(R.drawable.answer_button));

            if (questionIndex + 1 == questions.size()) {
                score += 1000;
                startStatsActivity();
            } else {
                setFields(questions.get(questionIndex));
            }
        }, 175);
    }

    @SuppressLint("SetTextI18n")
    private void setFields(Question question) {
        scoreTextView.setText(getResources().getString(R.string.in_game_score) + " " + score);
        questionTextView.setText(question.getQuestion());
        answerOne.setText(question.getAnswers().get(0).getAnswer());
        answerTwo.setText(question.getAnswers().get(1).getAnswer());
        answerThree.setText(question.getAnswers().get(2).getAnswer());
        answerFour.setText(question.getAnswers().get(3).getAnswer());
        setButtonsToClickable();
    }

    public void startTimer() {
        cdt = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long l) {
                int seconds = (int) l / 1000;
                timerTextView.setText(String.valueOf(seconds));
                if (seconds == 10) {
                    timerTextView.setTextColor(getColor(R.color.colorError));
                }
            }

            @Override
            public void onFinish() {
                showTimeOutDialog();
                play.interrupt();
            }
        };

        cdt.start();
    }

    private void showTimeOutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(GameActivity.this).inflate(
                R.layout.layout_game_over_dialog,
                findViewById(R.id.layoutDialogContainer)
        );

        builder.setView(view);
        builder.setCancelable(false);
        ((TextView) view.findViewById(R.id.textTitle)).setText(getResources().getString(R.string.dialog_times_up_title));
        ((TextView) view.findViewById(R.id.textMessage)).setText(getResources().getString(R.string.game_over_msg));
        ((ImageView) view.findViewById(R.id.imageIcon)).setImageResource(R.drawable.ic_times_up);
        Button continueBtn = view.findViewById(R.id.buttonAction);
        continueBtn.setText(getResources().getString(R.string.continueBtn));
        continueBtn.setOnClickListener(v -> startStatsActivity());

        AlertDialog alertDialog = builder.create();

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();
    }

    private void showStopDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(GameActivity.this).inflate(
                R.layout.layout_warning_dialog,
                findViewById(R.id.layoutDialogContainer)
        );
        builder.setView(view);
        ((TextView) view.findViewById(R.id.textTitle)).setText(R.string.stop_dialog_stop);
        ((TextView) view.findViewById(R.id.textMessage)).setText(R.string.stop_dialog_text);
        Button yesBtn = view.findViewById(R.id.buttonYes);
        yesBtn.setText(getResources().getString(R.string.yesBtn));
        Button noBtn = view.findViewById(R.id.buttonNo);
        noBtn.setText(R.string.noBtn);

        yesBtn.setOnClickListener(v -> {
            cdt.cancel();
            finish();
            startActivity(new Intent(GameActivity.this, HomeActivity.class));
        });

        AlertDialog alertDialog = builder.create();

        noBtn.setOnClickListener(v -> alertDialog.dismiss());

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();
    }

    private void getAllQuestions() {
        databaseReference.addChildEventListener(new ChildEventListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Question question = snapshot.getValue(Question.class);
                if (question != null) {
                    questions.add(question);
                    double progress = getProgress(questions.size());
                    progressCounter.setText(String.format("%.1f%%", progress));
                    progressBar.setProgress((int) progress);
                }

                if (questions.size() == Constants.QUESTIONS_COUNT) {
                    setContentView(R.layout.activty_game);
                    startTimer();
                    play.start();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(Tag.FIREBASE_ON_CHANGED, "Updated child: " + Objects.requireNonNull(snapshot.getValue(Question.class)));
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                Log.d(Tag.FIREBASE_ON_DELETED, "Child removed: " + snapshot.getKey());
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(Tag.FIREBASE_ON_MOVED, "Current child name: " + snapshot.getKey() + " | " + "Previous child name: " + previousChildName);
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

    private double getProgress(int questionSize) {

        return (double) (100 * questionSize) / Constants.QUESTIONS_COUNT;
    }

    private void startStatsActivity() {
        Intent i = new Intent(GameActivity.this, StatsActivity.class);
        i.putExtra("currentSessionScore", score);
        cdt.cancel();
        play.interrupt();
        startActivity(i);
        finish();
    }

    private void setButtonsToNotClickable() {
        answerOne.setClickable(false);
        answerTwo.setClickable(false);
        answerThree.setClickable(false);
        answerFour.setClickable(false);
    }

    private void setButtonsToClickable() {
        answerOne.setClickable(true);
        answerTwo.setClickable(true);
        answerThree.setClickable(true);
        answerFour.setClickable(true);
    }
}
