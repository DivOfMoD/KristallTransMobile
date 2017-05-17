package by.kristalltrans.kristalltransmobile;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        View.OnClickListener {

    public static final String APP_PREFERENCES = "by.kristalltrans.kristalltransmobile";
    public static final String APP_PREFERENCES_COUNTER = "region";
    private SharedPreferences mSettings;

    static final private int REQUEST_CODE_CHOOSE_COUNTRY = 0;
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    public Elements title;

    TextView tvCity, tvTempCurrent, tvTodayForecast;

    TextView tvUserName, tvUserEmail;
    CircleImageView ivUserPhoto;

    String city, tempCurrent;
    StringBuilder todayForecast;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        findViewById(R.id.llChat).setOnClickListener(this);
        findViewById(R.id.llCamera).setOnClickListener(this);
        findViewById(R.id.clock).setOnClickListener(this);
        findViewById(R.id.ivLogo).setOnClickListener(this);
        findViewById(R.id.llWeather).setOnClickListener(this);
        findViewById(R.id.llDialog).setOnClickListener(this);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setItemIconTintList(null);
        navigationView.setNavigationItemSelectedListener(this);

        final View header_nv = navigationView.inflateHeaderView(R.layout.nav_header_main);

        tvUserName = (TextView) header_nv.findViewById(R.id.tvUserName);
        tvUserEmail = (TextView) header_nv.findViewById(R.id.tvUserEmail);
        ivUserPhoto = (CircleImageView) header_nv.findViewById(R.id.ivUserPhoto);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    if (user.getDisplayName() != null)
                        tvUserName.setText(user.getDisplayName());
                    else {
                        tvUserName.setText("");
                        Toast.makeText(MainActivity.this, "Пожалуйста! Укажите Ваше Имя и Фамилию в настройках профиля!",
                                Toast.LENGTH_LONG).show();
                    }
                    tvUserEmail.setText(user.getEmail());

                    if (user.getPhotoUrl() != null) {
                        Glide.with(MainActivity.this)
                                .load(user.getPhotoUrl())
                                .into(ivUserPhoto);
                    } else
                        ivUserPhoto.setImageDrawable(ContextCompat
                                .getDrawable(MainActivity.this,
                                        R.drawable.profile));
                } else {
                    startActivity(new Intent(MainActivity.this, EmailPasswordActivity.class));
                }
                // ...
            }
        };

        tvCity = (TextView) findViewById(R.id.tvCity);
        tvTempCurrent = (TextView) findViewById(R.id.tvTempCurrent);
        tvTodayForecast = (TextView) findViewById(R.id.tvTodayForecast);

        new NewThread().execute();
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    }

    private static long back_pressed;

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (back_pressed + 2000 > System.currentTimeMillis())
                super.onBackPressed();
            else
                Toast.makeText(getBaseContext(), "Нажмите еще раз, чтобы выйти",
                        Toast.LENGTH_SHORT).show();
            back_pressed = System.currentTimeMillis();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_signOut:
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Выход из аккаунта")
                        .setMessage("Вы уверены?")
                        .setIcon(R.drawable.exit)
                        .setCancelable(true)
                        .setPositiveButton("Да",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        mAuth.signOut();
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
            case R.id.nav_updateProfile:
                startActivity(new Intent(MainActivity.this, UpdateProfileActivity.class));
                break;
            case R.id.nav_info:
                startActivity(new Intent(MainActivity.this, InfoActivity.class));
                break;
            case R.id.nav_inst:
                startActivity(new Intent(MainActivity.this, InstructionActivity.class));
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.llChat:
                startActivity(new Intent(this, ChatActivity.class));
                break;
            case R.id.llCamera:
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA},
                            REQUEST_CAMERA_PERMISSION);
                    break;
                }
                startActivity(new Intent(this, CameraActivity.class));
                break;
            case R.id.clock:
                startActivity(new Intent(this, CalendarActivity.class));
                break;
            case R.id.ivLogo:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://kristalltrans.by/")));
                break;
            case R.id.llWeather:
                startActivityForResult(new Intent(this, WeatherChooseCountryActivity.class), REQUEST_CODE_CHOOSE_COUNTRY);
                break;
            case R.id.llDialog:
                startActivity(new Intent(this, DialogActivity.class));
                break;
        }
    }

    public class NewThread extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... arg) {

            String region = "10274";

            if (mSettings.contains(APP_PREFERENCES_COUNTER))
                // Получаем число из настроек
                region = mSettings.getString(APP_PREFERENCES_COUNTER, "10274");


            Document doc;

            try {

                doc = Jsoup.connect("https://p.ya.ru/" + region).get();

                title = doc.select(".city.city_type_selectable.i-bem");
                city = title.text();
                title = doc.select(".temp-current.i-bem");
                tempCurrent = title.text();
                title = doc.select(".today-forecast");
                todayForecast = new StringBuilder(title.text());
                for (int i = 0; i < todayForecast.length(); i++) {
                    if (todayForecast.charAt(i) == ',') {
                        todayForecast.replace(i, i + 1, "\n");
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {

            if (city != null) {
                findViewById(R.id.progressBar).setVisibility(View.GONE);
                tvCity.setText("В " + city);
                tvTempCurrent.setText(tempCurrent + " °C");
                tvTodayForecast.setText(todayForecast);
            } else {
                if (Internet.hasConnection(MainActivity.this))
                    new NewThread().execute();
                else Toast.makeText(getBaseContext(), "Подключение к интернету отсутствует!",
                        Toast.LENGTH_SHORT).show();
            }
        }
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CHOOSE_COUNTRY) {
            if (resultCode == RESULT_OK) {
                tvCity.setText("");
                tvTempCurrent.setText("");
                tvTodayForecast.setText("");
                findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                new NewThread().execute();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.length == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onClick(findViewById(R.id.llCamera));
            } else {
                Toast.makeText(MainActivity.this, "Для работы камеры требуется разрешение на доступ к камере на Вашем устройстве.",
                        Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
