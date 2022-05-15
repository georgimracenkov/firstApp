package com.example.kursov.controllers;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.kursov.R;
import com.example.kursov.models.User;
import com.example.kursov.tools.BackgroundMusicService;
import com.example.kursov.tools.Constants;
import com.example.kursov.tools.Tag;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;

public class AccountSettingsActivity extends AppCompatActivity {
    private EditText emailEditText;
    private EditText nicknameEditText;
    private EditText passwordEditText;
    private ProgressBar updateProgressBar;
    private ProgressBar picProgressBar;
    private ImageView profilePicture;
    private CardView profilePicCardView;
    private AlertDialog alertDialog;
    private Uri imageUri;
    private boolean profilePicChanged;
    private final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");
    private FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    private final StorageReference storageReference = FirebaseStorage.getInstance().getReference();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);
        BackgroundMusicService.resumePlayer();
        profilePicChanged = false;

        emailEditText = findViewById(R.id.updateEmail);
        nicknameEditText = findViewById(R.id.updateNickname);
        passwordEditText = findViewById(R.id.updatePassword);
        updateProgressBar = findViewById(R.id.accountSettingsProgressbar);
        picProgressBar = findViewById(R.id.profilePicProgressBar);
        profilePicture = findViewById(R.id.profilePicAccountSettings);
        profilePicCardView = findViewById(R.id.profilePicCardView);
        Button update = findViewById(R.id.updateAccBtn);
        Button delete = findViewById(R.id.deleteAccBtn);

        getProfilePic();

        update.setOnClickListener(view -> {
            hideKeyboard(view);

            if (profilePicChanged) {
                updateProfilePic(firebaseUser.getEmail());
            }

            String email = emailEditText.getText().toString().trim();
            String nickname = nicknameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (!email.isEmpty() || !nickname.isEmpty() || !password.isEmpty()) {
                if (!email.isEmpty()) {
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        emailEditText.setError("Enter a valid email!");
                        emailEditText.requestFocus();
                    } else {
                        updateProgressBar.setVisibility(View.VISIBLE);
                        checkEmailAvailability(email, nickname, password);
                    }
                } else {
                    updateProgressBar.setVisibility(View.VISIBLE);
                    checkEmailAvailability(email, nickname, password);
                }
            }
        });

        delete.setOnClickListener(view -> {
            hideKeyboard(view);
            showDeleteWarningMessage();
        });

        profilePicture.setOnClickListener(view -> showOptionsDialog());
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
        if (isBackgroundMusicServiceRunning()) {
            startService(new Intent(this, BackgroundMusicService.class));
        } else {
            BackgroundMusicService.resumePlayer();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        profilePicChanged = true;

        if (requestCode == Constants.CAMERA_REQUEST_CODE) {
            Bitmap image = (Bitmap) data.getExtras().get("data");

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            String path = MediaStore.Images.Media.insertImage(getContentResolver(), image, "Title", null);

            imageUri = Uri.parse(path);
            profilePicture.setImageURI(imageUri);
        }

        if (requestCode == Constants.GALLERY_REQUEST_CODE) {

            try {
                imageUri = data.getData();
            } catch (Exception e) {
                Log.d(Tag.NULL_POINTER, e.getMessage() + "|" + Arrays.toString(e.getStackTrace()));
            }

            if (imageUri != null) {
                profilePicture.setImageURI(imageUri);
            }
        }

        alertDialog.dismiss();
    }

    private void deleteUser() {
        if (firebaseUser != null) {
            databaseReference.orderByChild("email").equalTo(firebaseUser.getEmail()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = null;
                    String key = null;

                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        user = dataSnapshot.getValue(User.class);
                        key = dataSnapshot.getKey();
                    }

                    if (key != null) {
                        databaseReference.child(key).removeValue();
                    }

                    if (user != null) {
                        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), user.getPassword());
                        firebaseUser.reauthenticate(credential).addOnCompleteListener(t1 -> firebaseUser.delete().addOnCompleteListener(t2 -> {
                            if (t2.isSuccessful()) {
                                Log.d(Tag.FIREBASE_ACCOUNT_DELETED_SUCCESSFULLY, "User account deleted.");
                                showDeleteDialog();
                                updateProgressBar.setVisibility(View.INVISIBLE);
                            }
                        }));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.d(Tag.FIREBASE_ON_CANCELLED, error.getDetails() + " | " + error.getMessage());
                }
            });

        }
    }

    private void updateUser(User user) {
        databaseReference.orderByChild("email").equalTo(firebaseUser.getEmail()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User existingUser = null;
                String key = null;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    existingUser = dataSnapshot.getValue(User.class);
                    key = dataSnapshot.getKey();
                }

                if (existingUser != null) {
                    if (!isUserEqual(user, existingUser)) {
                        if (user.getEmail().isEmpty()) {
                            user.setEmail(existingUser.getEmail());
                        }
                        if (user.getNickname().isEmpty()) {
                            user.setNickname(existingUser.getNickname());
                        }
                        if (user.getPassword().isEmpty()) {
                            user.setPassword(existingUser.getPassword());
                        }

                        HashMap<String, Object> update = new HashMap<>();
                        update.put("email", user.getEmail());
                        update.put("nickname", user.getNickname());
                        update.put("password", user.getPassword());
                        update.put("score", existingUser.getScore());

                        if (key != null) {
                            databaseReference.child(key).updateChildren(update).addOnCompleteListener(task -> {
                                if (task.isComplete()) {
                                    updateFirebaseUser(user.getEmail(), user.getPassword(), !user.getEmail().equals(firebaseUser.getEmail()));

                                } else {
                                    Log.d(Tag.FIREBASE_ACCOUNT_UPDATE_IS_UNSUCCESSFUL, "An error occurred when trying to update the account");
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(Tag.FIREBASE_ON_CANCELLED, error.getDetails() + " | " + error.getMessage());
            }
        });
    }

    private boolean isUserEqual(User user, User existingUser) {
        return user.getEmail().equals(existingUser.getEmail())
                && user.getNickname().equals(existingUser.getNickname())
                && user.getPassword().equals(existingUser.getPassword());
    }

    private void updateFirebaseUser(String email, String password, boolean emailUpdate) {
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        firebaseUser.reauthenticate(credential).addOnCompleteListener(task -> firebaseUser.updateEmail(email).addOnCompleteListener(t1 -> firebaseUser.updatePassword(password).addOnCompleteListener(t2 -> {
            if (t2.isComplete()) {
                Log.d(Tag.FIREBASE_ACCOUNT_UPDATED_SUCCESSFULLY, "Account is successfully updated");
                if (emailUpdate) {
                    firebaseUser.sendEmailVerification().addOnCompleteListener(t3 -> FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).addOnCompleteListener(t4 -> {
                        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                        try {
                            firebaseUser.sendEmailVerification();
                            FirebaseAuth.getInstance().signOut();
                            showEmailUpdateDialog();
                            updateProgressBar.setVisibility(View.INVISIBLE);
                        } catch (Exception e) {
                            throw new NullPointerException("Firebase user is null");
                        }
                    }));
                } else {
                    showUpdateDialog();
                    updateProgressBar.setVisibility(View.INVISIBLE);
                }
            }
        })));
    }

    private void checkEmailAvailability(String email, String nickname, String password) {
        databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    checkNicknameAvailability(email, nickname, password);
                } else {
                    emailEditText.setError("Email already in use!");
                    emailEditText.requestFocus();
                    updateProgressBar.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(Tag.FIREBASE_ON_CANCELLED, error.getDetails() + " | " + error.getMessage());
            }
        });
    }

    private void checkNicknameAvailability(String email, String nickname, String password) {
        databaseReference.orderByChild("nickname").equalTo(nickname).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    User user = new User();
                    user.setEmail(email);
                    user.setNickname(nickname);
                    user.setPassword(password);
                    updateUser(user);
                } else {
                    nicknameEditText.setError("Nickname already in use!");
                    nicknameEditText.requestFocus();
                    updateProgressBar.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(Tag.FIREBASE_ON_CANCELLED, error.getDetails() + "| " + error.getMessage());
            }
        });
    }

    private void showEmailUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(AccountSettingsActivity.this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(AccountSettingsActivity.this).inflate(
                R.layout.layout_success_dialog,
                findViewById(R.id.layoutDialogContainer)
        );
        builder.setView(view);
        builder.setCancelable(false);
        ((TextView) view.findViewById(R.id.textTitle)).setText(R.string.update_success_title);
        ((TextView) view.findViewById(R.id.textMessage)).setText(R.string.dialog_update_email_text);
        ((ImageView) view.findViewById(R.id.imageIcon)).setImageResource(R.drawable.ic_success);
        Button okBtn = view.findViewById(R.id.buttonAction);
        okBtn.setText(R.string.okay);


        alertDialog = builder.create();

        okBtn.setOnClickListener(v -> startActivity(new Intent(AccountSettingsActivity.this, MainActivity.class)));

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();
    }

    private void showUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(AccountSettingsActivity.this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(AccountSettingsActivity.this).inflate(
                R.layout.layout_success_dialog,
                findViewById(R.id.layoutDialogContainer)
        );
        builder.setView(view);
        builder.setCancelable(false);
        ((TextView) view.findViewById(R.id.textTitle)).setText(R.string.update_success_title);
        ((TextView) view.findViewById(R.id.textMessage)).setText(R.string.dialog_update_success_text);
        ((ImageView) view.findViewById(R.id.imageIcon)).setImageResource(R.drawable.ic_success);
        Button okBtn = view.findViewById(R.id.buttonAction);
        okBtn.setText(R.string.okay);


        alertDialog = builder.create();

        okBtn.setOnClickListener(v -> alertDialog.dismiss());

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();
    }

    private void showDeleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(AccountSettingsActivity.this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(AccountSettingsActivity.this).inflate(
                R.layout.layout_success_dialog,
                findViewById(R.id.layoutDialogContainer)
        );
        builder.setView(view);
        builder.setCancelable(false);
        ((TextView) view.findViewById(R.id.textTitle)).setText(R.string.delete_account_success_title);
        ((TextView) view.findViewById(R.id.textMessage)).setText(R.string.delete_account_success_text);
        ((ImageView) view.findViewById(R.id.imageIcon)).setImageResource(R.drawable.ic_success);
        Button okBtn = view.findViewById(R.id.buttonAction);
        okBtn.setText(R.string.okay);


        alertDialog = builder.create();

        okBtn.setOnClickListener(v -> {
            startActivity(new Intent(AccountSettingsActivity.this, MainActivity.class));
            finish();
        });

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();
    }

    private void showDeleteWarningMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(AccountSettingsActivity.this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(AccountSettingsActivity.this).inflate(
                R.layout.layout_warning_dialog,
                findViewById(R.id.layoutDialogContainer)
        );
        builder.setView(view);
        builder.setCancelable(false);
        ((TextView) view.findViewById(R.id.textTitle)).setText(R.string.delete_account_success_title);
        ((TextView) view.findViewById(R.id.textMessage)).setText(R.string.delete_account_success_text);
        ((ImageView) view.findViewById(R.id.imageIcon)).setImageResource(R.drawable.ic_warning);
        Button noBtn = view.findViewById(R.id.buttonNo);
        Button yesBtn = view.findViewById(R.id.buttonYes);
        noBtn.setText(R.string.noBtn);
        yesBtn.setText(R.string.yesBtn);

        alertDialog = builder.create();

        noBtn.setOnClickListener(v -> alertDialog.dismiss());

        yesBtn.setOnClickListener(v -> {
            alertDialog.dismiss();
            deleteUser();
            updateProgressBar.setVisibility(View.VISIBLE);
        });

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();
    }

    private void showOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(AccountSettingsActivity.this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(AccountSettingsActivity.this).inflate(
                R.layout.layout_options_dialog,
                findViewById(R.id.layoutDialogContainer)
        );

        builder.setView(view);
        builder.setCancelable(false);
        ((TextView) view.findViewById(R.id.textTitle)).setText(R.string.options);
        TextView option1 = view.findViewById(R.id.option1);
        TextView option2 = view.findViewById(R.id.option2);
        Button cancelBtn = view.findViewById(R.id.buttonAction);
        cancelBtn.setText(R.string.cancel);

        alertDialog = builder.create();

        cancelBtn.setOnClickListener(v -> alertDialog.dismiss());

        option1.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            try {
                startActivityForResult(takePictureIntent, 1001);
            } catch (Exception e) {
                throw new ActivityNotFoundException();
            }
        });

        option2.setOnClickListener(v -> {
            Intent openGalleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            try {
                startActivityForResult(openGalleryIntent, 1000);
            } catch (Exception e) {
                throw new ActivityNotFoundException();
            }
        });

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();
    }

    private void getProfilePic() {
        databaseReference.orderByChild("email").equalTo(firebaseUser.getEmail()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = null;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    user = dataSnapshot.getValue(User.class);
                }

                try {
                    getPic(user.getEmail());
                } catch (Exception e) {
                    throw new NullPointerException();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(Tag.FIREBASE_ON_CANCELLED, error.getDetails() + " | " + error.getMessage());
            }
        });
    }

    private void getPic(String email) {
        storageReference.child(email).getDownloadUrl().addOnSuccessListener(uri -> {
            Picasso.get().load(uri).into(profilePicture);
            picProgressBar.setVisibility(View.GONE);
            profilePicCardView.setVisibility(View.VISIBLE);
        }).addOnFailureListener(exception -> {
            profilePicture.setImageResource(R.drawable.blank_profile_picture);
            picProgressBar.setVisibility(View.GONE);
            profilePicCardView.setVisibility(View.VISIBLE);
        });
    }

    private void updateProfilePic(String profilePicName) {
        StorageReference strRef = storageReference.child(profilePicName);
        strRef.getDownloadUrl().addOnSuccessListener(uri -> {
            if (imageUri != null) {
                if (uri != null) {
                    strRef.delete();
                }
                strRef.putFile(imageUri);
            }
        }).addOnFailureListener(exception -> strRef.putFile(imageUri));
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

    private void hideKeyboard(View v) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
    }
}
