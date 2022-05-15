package com.example.kursov.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.kursov.R;
import com.example.kursov.models.Question;
import com.example.kursov.models.User;
import com.example.kursov.tools.Tag;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class PlayFragment extends Fragment {
    private List<User> users;
    private final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");
    private TextView ranking;
    private TextView personalBestHiScoreTextView;
    private ProgressBar rankingProgressBar;

    public PlayFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getRankingData();
        getHiScore();
        View frView = inflater.inflate(R.layout.fragment_play, container, false);
        ranking = frView.findViewById(R.id.rankingTextView);
        rankingProgressBar = frView.findViewById(R.id.rankingProgressbar);
        personalBestHiScoreTextView = frView.findViewById(R.id.personalBestHiScore);

        users = new ArrayList<>();

        Button button = frView.findViewById(R.id.playBtn);
        button.setOnClickListener(view -> {
            startActivity(new Intent(getActivity(), GameActivity.class));
        });

        return frView;
    }

    private void getRankingData() {
        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    users.add(user);
                }

                StringBuilder rank = new StringBuilder();

                if (users.size() > 2) {
                    users.sort(Comparator.comparing(User::getScore).reversed());
                }

                for (int i = 0; i < users.size(); i++) {
                    if (i == 10) {
                        break;
                    }
                    rank.append(i + 1).append(": ").append(users.get(i).getNickname()).append("\n");
                }
                rankingProgressBar.setVisibility(View.INVISIBLE);
                ranking.setText(rank.toString());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(Tag.FIREBASE_ON_CHANGED, "Updated child: " + Objects.requireNonNull(snapshot.getValue(Question.class)));
                ranking.setText("");
                rankingProgressBar.setVisibility(View.VISIBLE);
                users.sort(Comparator.comparing(User::getScore).reversed());
                StringBuilder rank = new StringBuilder();

                if (users.size() > 2) {
                    users.sort(Comparator.comparing(User::getScore).reversed());
                }

                for (int i = 0; i < users.size(); i++) {
                    if (i == 10) {
                        break;
                    }
                    rank.append(i + 1).append(": ").append(users.get(i).getNickname()).append("\n");
                }
                rankingProgressBar.setVisibility(View.INVISIBLE);
                ranking.setText(rank.toString());
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
                Log.d(Tag.FIREBASE_ON_CANCELLED, error.getDetails() + "| " + error.getMessage());
            }
        });
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

                for (DataSnapshot dataSnapshot: snapshot.getChildren()) {
                    user = dataSnapshot.getValue(User.class);
                }

                if (user != null) {
                    personalBestHiScoreTextView.setText(String.valueOf(user.getScore()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(Tag.FIREBASE_ON_CANCELLED, error.getDetails() + " | " + error.getMessage());
            }
        });
    }
}