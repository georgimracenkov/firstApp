package com.example.kursov.controllers;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.kursov.R;
import com.example.kursov.tools.BackgroundMusicService;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    public SettingsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View frView = inflater.inflate(R.layout.fragment_settings, container, false);
        TextView signOut = frView.findViewById(R.id.signOut);
        TextView accountSettings = frView.findViewById(R.id.accountSettings);
        ImageView mute = frView.findViewById(R.id.muteBtn);
        ImageView unmute = frView.findViewById(R.id.unmuteBtn);

        signOut.setOnClickListener(view -> showLogOutDialog(frView));
        accountSettings.setOnClickListener(view -> startActivity(new Intent(getActivity(), AccountSettingsActivity.class)));
        mute.setOnClickListener(view -> muteMusic(mute, unmute));
        unmute.setOnClickListener(view -> unmuteMusic(unmute, mute));

        return frView;
    }

    private void showLogOutDialog(View frView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity(), R.style.AlertDialogTheme);
        View view = LayoutInflater.from(getActivity()).inflate(
                R.layout.layout_warning_dialog,
                frView.findViewById(R.id.layoutDialogContainer)
        );
        builder.setView(view);
        builder.setCancelable(false);
        ((TextView) view.findViewById(R.id.textTitle)).setText(getResources().getString(R.string.dialog_warning_log_out_title));
        ((TextView) view.findViewById(R.id.textMessage)).setText(getResources().getString(R.string.dialog_warning_log_out_message));
        Button yesBtn = view.findViewById(R.id.buttonYes);
        yesBtn.setText(getResources().getString(R.string.yesBtn));
        Button noBtn = view.findViewById(R.id.buttonNo);
        noBtn.setText(R.string.noBtn);

        yesBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().stopService(new Intent(getActivity(), BackgroundMusicService.class));
        });

        AlertDialog alertDialog = builder.create();

        noBtn.setOnClickListener(v -> alertDialog.dismiss());

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();
    }

    private void muteMusic(ImageView mute, ImageView unmute) {
        BackgroundMusicService.mutePlayer();
        mute.setVisibility(View.GONE);
        unmute.setVisibility(View.VISIBLE);
    }

    private void unmuteMusic(ImageView unmute, ImageView mute) {
        BackgroundMusicService.unmutePlayer();
        unmute.setVisibility(View.GONE);
        mute.setVisibility(View.VISIBLE);
    }
}