package by.kristalltrans.kristalltransmobile;

import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener, Camera.PictureCallback, Camera.PreviewCallback, Camera.AutoFocusCallback {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private StorageReference mStorageRef;

    Camera camera;
    SurfaceView preview;
    SurfaceHolder surfaceHolder;
    HolderCallback holderCallback;
    ProgressBar progressBar;

    final int CAMERA_ID = 0;
    final boolean FULL_SCREEN = false;

    boolean isButtonBackWork = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null)
                    startActivity(new Intent(CameraActivity.this, EmailPasswordActivity.class));
            }
        };

        preview = (SurfaceView) findViewById(R.id.surfaceView);

        surfaceHolder = preview.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        holderCallback = new HolderCallback();
        surfaceHolder.addCallback(holderCallback);

        findViewById(R.id.picture).setOnClickListener(this);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

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

    @Override
    protected void onResume() {
        super.onResume();
        camera = Camera.open(CAMERA_ID);
        Camera.Parameters params = camera.getParameters();
        params.setFlashMode("auto");
        params.setJpegQuality(100);
        params.setRotation(90);
        List<Camera.Size> sizeList = params.getSupportedPictureSizes();
        Camera.Size size1 = sizeList.get(0);
        Camera.Size size2 = sizeList.get(sizeList.size() - 1);
        if (size1.height > size2.height) {
            params.setPictureSize(size1.width, size1.height);
        } else {
            params.setPictureSize(size2.width, size2.height);
        }
        //params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        camera.setParameters(params);
        setPreviewSize(FULL_SCREEN);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (camera != null)
            camera.release();
        camera = null;
    }

    class HolderCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            camera.stopPreview();
            setCameraDisplayOrientation(CAMERA_ID);
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }

    }

    @Override
    public void onClick(View v) {
        if (v == findViewById(R.id.picture)) {
            //camera.takePicture(null, null, null, this);
            camera.autoFocus(this);
        }
    }

    @Override
    public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera) {
        findViewById(R.id.picture).setVisibility(View.GONE);
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);

        new SaveInBackground().execute(paramArrayOfByte);
        camera.startPreview();
    }

    class SaveInBackground extends AsyncTask<byte[], String, String> {
        @Override
        protected String doInBackground(byte[]... arrayOfByte) {
            try {
                FileOutputStream os =
                        new FileOutputStream(new File(getFilesDir(), "pic.jpg"));
                os.write(arrayOfByte[0]);
                os.close();
                isButtonBackWork = false;
                if (Internet.hasConnection(CameraActivity.this)) {
                    showToast("Загрузка фотографии началась.");
                    mStorageRef = FirebaseStorage.getInstance().getReference();

                    Uri file = Uri.fromFile(new File(getFilesDir(), "pic.jpg"));
                    final String user;
                    if (FirebaseAuth.getInstance().getCurrentUser().getDisplayName() == null)
                        user = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                    else user = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
                    final ArrayList<String> date = new ArrayList<>();
                    final ArrayList<Uri> downloadUrl = new ArrayList<>();
                    date.clear();
                    date.clear();
                    date.add(new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(new Date()));
                    StorageReference riversRef = mStorageRef.child("Documents/"
                            + user
                            + "/" + new SimpleDateFormat("yyyy.MM.dd").format(new Date()) + "/"
                            + date.get(0) + "_" + user + ".jpg");

                    riversRef.putFile(file)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    // Get a URL to the uploaded content
                                    downloadUrl.add(taskSnapshot.getDownloadUrl());
                                    Document document = new
                                            Document(user, date.get(0), downloadUrl.get(0).toString());
                                    DatabaseReference mSimpleFirechatDatabaseReference = FirebaseDatabase.getInstance().getReference();
                                    mSimpleFirechatDatabaseReference.child("documents")
                                            .push().setValue(document);
                                    notification(user);
                                    isButtonBackWork = true;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            findViewById(R.id.progressBar).setVisibility(View.GONE);
                                            findViewById(R.id.picture).setVisibility(View.VISIBLE);
                                            Toast.makeText(CameraActivity.this, "Фотография успешно отправлена.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            findViewById(R.id.progressBar).setVisibility(View.GONE);
                                            findViewById(R.id.picture).setVisibility(View.VISIBLE);
                                            Toast.makeText(CameraActivity.this, "Во время отправки фотогравфии произошла ошибка!\n" +
                                                    "Попробуйте еще раз.", Toast.LENGTH_SHORT).show();

                                        }
                                    });
                                    isButtonBackWork = true;
                                }
                            });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            findViewById(R.id.progressBar).setVisibility(View.GONE);
                            findViewById(R.id.picture).setVisibility(View.VISIBLE);
                            Toast.makeText(CameraActivity.this, "Подключение к интернету отсутствует!", Toast.LENGTH_SHORT).show();
                        }
                    });
                    isButtonBackWork = true;
                    //showToast("Подключение к интернету отсутствует!");
                }
            } catch (Exception e) {
                isButtonBackWork = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.progressBar).setVisibility(View.GONE);
                        findViewById(R.id.picture).setVisibility(View.VISIBLE);
                        Toast.makeText(CameraActivity.this, "Во время сохранения фотогравфии произошла ошибка!\nПопробуйте еще раз.", Toast.LENGTH_SHORT).show();

                    }
                });
            }
            return (null);
        }
    }

    @Override
    public void onAutoFocus(boolean paramBoolean, Camera paramCamera) {
        if (paramBoolean) {
            // если удалось сфокусироваться, делаем снимок
            paramCamera.takePicture(null, null, null, this);
            paramCamera.cancelAutoFocus();
        }
    }

    @Override
    public void onPreviewFrame(byte[] paramArrayOfByte, Camera paramCamera) {
        // здесь можно обрабатывать изображение, показываемое в preview
    }

    void setPreviewSize(boolean fullScreen) {

        // получаем размеры экрана
        Display display = getWindowManager().getDefaultDisplay();
        boolean widthIsMax = display.getWidth() > display.getHeight();

        // определяем размеры превью камеры
        Camera.Size size = camera.getParameters().getPreviewSize();

        RectF rectDisplay = new RectF();
        RectF rectPreview = new RectF();

        // RectF экрана, соотвествует размерам экрана
        rectDisplay.set(0, 0, display.getWidth(), display.getHeight());

        // RectF первью
        if (widthIsMax) {
            // превью в горизонтальной ориентации
            rectPreview.set(0, 0, size.width, size.height);
        } else {
            // превью в вертикальной ориентации
            rectPreview.set(0, 0, size.height, size.width);
        }

        Matrix matrix = new Matrix();
        // подготовка матрицы преобразования
        if (!fullScreen) {
            // если превью будет "втиснут" в экран (второй вариант из урока)
            matrix.setRectToRect(rectPreview, rectDisplay,
                    Matrix.ScaleToFit.START);
        } else {
            // если экран будет "втиснут" в превью (третий вариант из урока)
            matrix.setRectToRect(rectDisplay, rectPreview,
                    Matrix.ScaleToFit.START);
            matrix.invert(matrix);
        }
        // преобразование
        matrix.mapRect(rectPreview);

        // установка размеров surface из получившегося преобразования
        preview.getLayoutParams().height = (int) (rectPreview.bottom);
        preview.getLayoutParams().width = (int) (rectPreview.right);
    }

    void setCameraDisplayOrientation(int cameraId) {
        // определяем насколько повернут экран от нормального положения
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result = 0;

        // получаем инфо по камере cameraId
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        // задняя камера
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            result = ((360 - degrees) + info.orientation);
        } else
            // передняя камера
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = ((360 - degrees) - info.orientation);
                result += 360;
            }
        result = result % 360;
        camera.setDisplayOrientation(result);
    }

    private void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CameraActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void notification(final String user) {

        final ArrayList<String> adminTokens = new ArrayList<>();
        final ArrayList<String> arrayList = new ArrayList<>();
        arrayList.clear();
        arrayList.add("1");

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("adminTokens");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                if (arrayList.size() > 0) {
                    HashMap<String, String> value = (HashMap<String, String>) dataSnapshot.getValue();
                    adminTokens.clear();
                    for (int i = 0; i < value.size(); i++)
                        adminTokens.add(value.values().toArray()[i].toString());

                    MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                    OkHttpClient client = new OkHttpClient();

                    String keyFromConsole = "AAAAK4hHGz0:APA91bGXJ3-cCVUdF9YpuScopTOG3AYIvG3SUyLVy2QBj7GKIEj8ZQWbg4jx8NTSfJQMpDSgQUu1B16QvkUMpuPeVGPJZqzcyVzznJJ0GOlgW6GHzfjTxIjbZqGS-QGvlffbJa8ASZwY82Shxakn3O7vGjfWMMKCIg";//тут ключ который можно взять из консоли (Настройки-настройки проекта-CLOUD MESSAGING-ключ сервера)

                    for (int i = 0; i < value.size(); i++) {

                        String json = "{ \"notification\": { \"text\": \"" + user + " прислал документ\", \"sound\": \"notification_sound\"}, \"to\" : \"" + adminTokens.get(i) + "\"}";

                        RequestBody body = RequestBody.create(JSON, json);
                        Request request = new Request.Builder()
                                .url("https://fcm.googleapis.com/fcm/send")
                                .addHeader("Authorization", "key=" + keyFromConsole)
                                .addHeader("ContentType", "application/json")
                                .post(body)
                                .build();
                        client.newCall(request).enqueue(new Callback() {

                            @Override
                            public void onFailure(Call call, IOException e) {
                                e.printStackTrace();
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                System.out.println(response.body().string());
                            }
                        });
                    }
                    arrayList.clear();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
            }
        });

    }

    @Override
    public void onBackPressed() {
        if (isButtonBackWork) {
            super.onBackPressed();
        } else {
            Toast.makeText(CameraActivity.this, "Дождитесь окончания загрузки!", Toast.LENGTH_SHORT).show();
        }
    }

}