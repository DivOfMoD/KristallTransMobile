package by.kristalltrans.kristalltransmobile;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import de.hdodenhof.circleimageview.CircleImageView;

public class UpdateProfileActivity extends AppCompatActivity implements
        View.OnClickListener {

    static final int GALLERY_REQUEST = 1;
    private static final int REQUEST_READ_EXTERNAL_STORAGE_PERMISSION = 2;

    private EditText mNameField;
    private EditText mLastNameField;
    private EditText mMiddleNameField;
    private CircleImageView mUserPhoto;

    private FirebaseAuth mAuth;

    private FirebaseUser mUser;

    private FirebaseAuth.AuthStateListener mAuthListener;

    private StorageReference mStorageRef;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_updateprofile);

        mNameField = (EditText) findViewById(R.id.field_name);
        mLastNameField = (EditText) findViewById(R.id.field_lastname);
        mMiddleNameField = (EditText) findViewById(R.id.field_middlename);
        mUserPhoto = (CircleImageView) findViewById(R.id.userPhoto);

        mUserPhoto.setOnClickListener(this);
        findViewById(R.id.save_button).setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                mUser = firebaseAuth.getCurrentUser();
                if (mUser == null)
                    startActivity(new Intent(UpdateProfileActivity.this, EmailPasswordActivity.class));
                else {
                    if (mUser.getPhotoUrl() != null) {
                        Glide.with(UpdateProfileActivity.this)
                                .load(mUser.getPhotoUrl())
                                .into(mUserPhoto);
                    } else
                        mUserPhoto.setImageDrawable(ContextCompat
                                .getDrawable(UpdateProfileActivity.this,
                                        R.drawable.profile));
                }
            }
        };

        mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void updateUserProfile(String name, String lastname, String middlename) {
        if (!validateForm()) {
            return;
        }

        findViewById(R.id.name_lastname_fields).setVisibility(View.GONE);
        findViewById(R.id.save_button).setVisibility(View.GONE);
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        findViewById(R.id.userPhoto).setClickable(false);

        mUser = FirebaseAuth.getInstance().getCurrentUser();

        UserProfileChangeRequest profileUpdates;

        if (TextUtils.isEmpty(middlename)) {
            profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name + " " + lastname)
                    .build();
        } else {
            profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(lastname + " " + name + " " + middlename)
                    .build();
        }


        mUser.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(UpdateProfileActivity.this, "Данные профиля успешно обновлены.\nПожалуйста, авторизуйтесь еще раз.",
                                    Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                            finish();
                        } else {
                            Toast.makeText(UpdateProfileActivity.this, "Ошибка обновления данных профиля!\nПопробуйте еще раз. ",
                                    Toast.LENGTH_SHORT).show();
                        }

                        findViewById(R.id.progressBar).setVisibility(View.GONE);
                        findViewById(R.id.name_lastname_fields).setVisibility(View.VISIBLE);
                        findViewById(R.id.save_button).setVisibility(View.VISIBLE);
                        findViewById(R.id.userPhoto).setClickable(true);
                    }
                });
    }

    private void userAnswer() {
        AlertDialog.Builder builder = new AlertDialog.Builder(UpdateProfileActivity.this);
        builder.setTitle("Внимание!")
                .setMessage("Для сохраниния изменений потребуется выход из аккаунта!\nСохранить изменения?")

                .setIcon(R.drawable.warning)
                .setCancelable(true)
                .setPositiveButton("Да",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                updateUserProfile(mNameField.getText().toString(), mLastNameField.getText().toString(), mMiddleNameField.getText().toString());
                                dialog.cancel();
                            }
                        })
                .setNegativeButton("Нет",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private boolean validateForm() {
        boolean valid = true;

        String name = mNameField.getText().toString();
        if (TextUtils.isEmpty(name)) {
            mNameField.setError("Введите имя.");
            valid = false;
        } else {
            mNameField.setError(null);
        }

        String lastname = mLastNameField.getText().toString();
        if (TextUtils.isEmpty(lastname)) {
            mLastNameField.setError("Введите фамилию.");
            valid = false;
        } else {
            mLastNameField.setError(null);
        }

        return valid;
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.save_button:
                userAnswer();
                break;
            case R.id.userPhoto:
                if (ContextCompat.checkSelfPermission(UpdateProfileActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(UpdateProfileActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_READ_EXTERNAL_STORAGE_PERMISSION);
                    break;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(UpdateProfileActivity.this);
                builder.setTitle("Внимание!")
                        .setMessage("Для обновления фотографии профиля потребуется выход из аккаунта!\nОбновить фотографию?")

                        .setIcon(R.drawable.warning)
                        .setCancelable(true)
                        .setPositiveButton("Да",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                                        photoPickerIntent.setType("image/*");
                                        startActivityForResult(photoPickerIntent, GALLERY_REQUEST);
                                        dialog.cancel();
                                    }
                                })
                        .setNegativeButton("Нет",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                AlertDialog alert = builder.create();
                alert.show();
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case GALLERY_REQUEST:
                if (resultCode == RESULT_OK) {
                    findViewById(R.id.name_lastname_fields).setVisibility(View.GONE);
                    findViewById(R.id.save_button).setVisibility(View.GONE);
                    findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                    findViewById(R.id.userPhoto).setClickable(false);
                    Uri selectedImage = imageReturnedIntent.getData();
                    mStorageRef = FirebaseStorage.getInstance().getReference();
                    mUser = FirebaseAuth.getInstance().getCurrentUser();

                    StorageReference riversRef = mStorageRef.child("UsersPhotos/" + mUser.getEmail() + ".jpg");

                    riversRef.putFile(selectedImage)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    // Get a URL to the uploaded content
                                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                    Toast.makeText(UpdateProfileActivity.this, "Фотография профиля успешно загружена.",
                                            Toast.LENGTH_SHORT).show();

                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                            .setPhotoUri(downloadUrl)
                                            .build();

                                    mUser.updateProfile(profileUpdates)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(UpdateProfileActivity.this, "Фотография профиля успешно обновлена.\nПожалуйста, авторизуйтесь еще раз.",
                                                                Toast.LENGTH_SHORT).show();
                                                        mAuth.signOut();
                                                        finish();
                                                    } else {
                                                        Toast.makeText(UpdateProfileActivity.this, "Ошибка обновления фотографии профиля!\nПопробуйте еще раз. ",
                                                                Toast.LENGTH_SHORT).show();
                                                    }

                                                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                                                    findViewById(R.id.name_lastname_fields).setVisibility(View.VISIBLE);
                                                    findViewById(R.id.save_button).setVisibility(View.VISIBLE);
                                                    findViewById(R.id.userPhoto).setClickable(true);
                                                }
                                            });

                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle unsuccessful uploads
                                    // ...
                                    Toast.makeText(UpdateProfileActivity.this, "Ошибка обновления фотографии профиля!\n" +
                                                    "Попробуйте еще раз.",
                                            Toast.LENGTH_SHORT).show();

                                }
                            });
                }
                break;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE_PERMISSION && grantResults.length == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onClick(mUserPhoto);
            } else {
                Toast.makeText(UpdateProfileActivity.this, "Для изменения фотографиии профиля требуется разрешение на доступ к фото на Вашем устройстве.",
                        Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}