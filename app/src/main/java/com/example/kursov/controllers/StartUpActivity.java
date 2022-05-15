package com.example.kursov.controllers;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kursov.R;
import com.example.kursov.models.Question;
import com.example.kursov.tools.BackgroundMusicService;
import com.google.firebase.auth.FirebaseAuth;

public class StartUpActivity extends AppCompatActivity {
    private ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_up);
        progressBar = findViewById(R.id.startUpProgressBar);

//        new Questions();

        new Handler(getMainLooper()).postDelayed(() -> startActivity(isConnected()), 1000);
    }

    private boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = connectivityManager.getActiveNetwork();
        if (nw == null) return false;
        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);

        return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
    }

    private void startActivity(boolean isConnected) {
        if (isConnected) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) {
                startActivity(new Intent(StartUpActivity.this, MainActivity.class));
            } else {
                startActivity(new Intent(StartUpActivity.this, HomeActivity.class));
                startService(new Intent(this, BackgroundMusicService.class));
            }
            finish();
        } else {
            showNoInternetDialog();
        }
    }

    private void showNoInternetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(StartUpActivity.this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(StartUpActivity.this).inflate(
                R.layout.layout_error_dialog,
                findViewById(R.id.layoutDialogContainer)
        );

        builder.setView(view);
        builder.setCancelable(false);
        ((TextView) view.findViewById(R.id.textTitle)).setText(getResources().getString(R.string.dialog_error_title));
        ((TextView) view.findViewById(R.id.textMessage)).setText(R.string.dialog_no_internet_commection_text);
        ((ImageView) view.findViewById(R.id.imageIcon)).setImageResource(R.drawable.ic_error);
        Button retryBtn = view.findViewById(R.id.buttonAction);
        retryBtn.setText(R.string.retry);


        progressBar.setVisibility(View.INVISIBLE);

        AlertDialog alertDialog = builder.create();

        retryBtn.setOnClickListener(v -> {
            new Handler(getMainLooper()).postDelayed(() -> startActivity(isConnected()), 300);
            progressBar.setVisibility(View.VISIBLE);
            alertDialog.dismiss();
        });

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();
    }
}
