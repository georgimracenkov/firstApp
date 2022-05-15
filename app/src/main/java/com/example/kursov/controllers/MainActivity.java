package com.example.kursov.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kursov.R;
import com.example.kursov.tools.BackgroundMusicService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button login = findViewById(R.id.login);
        final TextView register = findViewById(R.id.register);
        final TextView email = findViewById(R.id.email);
        final TextView password = findViewById(R.id.password);
        final ProgressBar progressBar = findViewById(R.id.progressBarMain);

        login.setOnClickListener(view -> {
            if (!Patterns.EMAIL_ADDRESS.matcher(email.getText().toString()).matches()) {
                email.setError("Enter a valid email!");
                email.requestFocus();
                return;
            } else if (email.getText().toString().isEmpty()) {
                email.setError("Email required!");
                email.requestFocus();
                return;
            } else if (password.getText().toString().length() <=5 ) {
                password.setError("Password must be at least 6 or more symbols!");
                password.requestFocus();
                return;
            } else if (password.getText().toString().isEmpty()) {
                password.setError("Password required!");
                password.requestFocus();
                return;
            }

            hideKeyboard(view);
            progressBar.setVisibility(View.VISIBLE);

            auth.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(task -> {
                if (task.isComplete()) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        if (!user.isEmailVerified()) {
                            Toast.makeText(MainActivity.this, "Please, verify your email!", Toast.LENGTH_LONG).show();
                        } else {
                            startActivity(new Intent(MainActivity.this, HomeActivity.class));
                            startService(new Intent(this, BackgroundMusicService.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Account with this email/password does not exist!", Toast.LENGTH_LONG).show();
                    }
                    progressBar.setVisibility(View.INVISIBLE);
                } else {
                    Toast.makeText(MainActivity.this, "Failed! " + task.getException(), Toast.LENGTH_LONG).show();
                }
            });
        });

        register.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, RegisterActivity.class)));
    }

    private void hideKeyboard(View v) {
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(v.getApplicationWindowToken(),0);
    }
}
