package com.example.kursov.controllers;

import android.content.ActivityNotFoundException;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kursov.R;
import com.example.kursov.models.User;
import com.example.kursov.tools.Constants;
import com.example.kursov.tools.Tag;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;

public class RegisterActivity extends AppCompatActivity {
    private TextView email;
    private TextView nickname;
    private TextView password;
    private TextView confirmPassword;
    private ImageView profilePicture;
    private ProgressBar progressBar;
    private AlertDialog alertDialog;
    private Uri imageUri;
    private final FirebaseDatabase db = FirebaseDatabase.getInstance();
    private final DatabaseReference databaseReference = db.getReference("Users");
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final StorageReference storageReference = FirebaseStorage.getInstance().getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Button registerBtn = findViewById(R.id.processRegister);
        email = findViewById(R.id.userEmail);
        nickname = findViewById(R.id.userNickname);
        password = findViewById(R.id.userPassword);
        confirmPassword = findViewById(R.id.confirmPassword);
        progressBar = findViewById(R.id.progressBarRegister);
        profilePicture = findViewById(R.id.profilePictureRegister);

        registerBtn.setOnClickListener(view -> {
            if (email.getText().toString().isEmpty()) {
                email.setError("Email required!");
                email.requestFocus();
                return;
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email.getText().toString()).matches()) {
                email.setError("Enter a valid email!");
                email.requestFocus();
                return;
            } else if (nickname.getText().toString().isEmpty()) {
                nickname.setError("Nickname required!");
                nickname.requestFocus();
                return;
            } else if (nickname.getText().toString().length() <= 5) {
                nickname.setError("Nickname must be at least 6 symbols");
                nickname.requestFocus();
                return;
            } else if (password.getText().toString().isEmpty()) {
                password.setError("Password required!");
                password.requestFocus();
                return;
            } else if (password.getText().toString().length() <= 5) {
                password.setError("Password must be at least 6 characters!");
                password.requestFocus();
                return;
            } else if (confirmPassword.getText().toString().isEmpty()) {
                confirmPassword.setError("Confirm password required!");
                confirmPassword.requestFocus();
                return;
            } else if (!password.getText().toString().equals(confirmPassword.getText().toString())) {
                confirmPassword.setError("Passwords don't match!");
                confirmPassword.requestFocus();
                return;
            }

            hideKeyboard(view);

            progressBar.setVisibility(View.VISIBLE);

            checkEmailAvailability();
        });

        profilePicture.setOnClickListener(view -> showOptionsDialog());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

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
                throw new NullPointerException();
            }

            if (imageUri != null) {
                profilePicture.setImageURI(imageUri);
            }

            alertDialog.dismiss();
        }
    }

    private void uploadImage() {
        StorageReference strRef = storageReference.child(email.getText().toString().trim());
        strRef.putFile(imageUri).addOnCompleteListener(task -> {
            if (task.isComplete()) {
                Toast.makeText(RegisterActivity.this, "Failed to upload image", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void hideKeyboard(View v) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
    }

    private void checkEmailAvailability() {
        databaseReference.orderByChild("email").equalTo(email.getText().toString().trim()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    checkNicknameAvailability();
                } else {
                    email.setError("Email already in use!");
                    email.requestFocus();
                    progressBar.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(Tag.FIREBASE_ON_CANCELLED, error.getDetails() + "| " + error.getMessage());
            }
        });
    }

    private void checkNicknameAvailability() {
        databaseReference.orderByChild("nickname").equalTo(nickname.getText().toString().trim()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    registerUser(email.getText().toString().trim(), nickname.getText().toString().trim(), password.getText().toString().trim());
                } else {
                    nickname.setError("Nickname already in use!");
                    nickname.requestFocus();
                    progressBar.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(Tag.FIREBASE_ON_CANCELLED, error.getDetails() + " | " + error.getMessage());
            }
        });
    }

    private void registerUser(String email, String nickname, String password) {
        if (imageUri != null) {
            uploadImage();
        }

        User user = new User(email.trim()
                , password.trim()
                , nickname.trim()
                , 0);

        auth.createUserWithEmailAndPassword(user.getEmail(), user.getPassword()).addOnCompleteListener(authTask -> {
            if (authTask.isComplete()) {
                databaseReference.push().setValue(user).addOnCompleteListener(dbTask -> {
                    if (dbTask.isComplete()) {
                        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (fbUser != null) {
                            fbUser.sendEmailVerification();
                            Log.d(Tag.FIREBASE_CREATED_NEW_ACCOUNT, "Successfully created an account and a record in the database.");
                            progressBar.setVisibility(View.INVISIBLE);
                            showSuccessDialog();
                            auth.signOut();
                        }
                    } else {
                        Log.d(Tag.FIREBASE_FAILED_TO_CREATE_A_NEW_RECORD, "Failed to create a record in 'Users'.");
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                });
            } else {
                Log.d(Tag.FIREBASE_FAILED_TO_CREATE_A_NEW_ACCOUNT, "Failed to create a new Firebase account.");
                progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void showSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(RegisterActivity.this).inflate(
                R.layout.layout_success_dialog,
                findViewById(R.id.layoutDialogContainer)
        );
        builder.setView(view);
        builder.setCancelable(false);
        ((TextView) view.findViewById(R.id.textTitle)).setText(getResources().getString(R.string.dialog_success_title));
        ((TextView) view.findViewById(R.id.textMessage)).setText(getResources().getString(R.string.register_success_msg));
        ((Button) view.findViewById(R.id.buttonAction)).setText(getResources().getString(R.string.okay));
        ((ImageView) view.findViewById(R.id.imageIcon)).setImageResource(R.drawable.ic_success);

        alertDialog = builder.create();

        view.findViewById(R.id.buttonAction).setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        });

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();
    }

    private void showOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(RegisterActivity.this).inflate(
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
}
