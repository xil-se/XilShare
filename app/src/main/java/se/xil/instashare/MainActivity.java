package se.xil.instashare;

import android.Manifest;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "InstaShare";
    private static final int PERMISSION_REQUEST_CODE = 1;

    private Button settingsSaveButton;
    private TextView settingsURLText;
    private TextView settingsSecretText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Log.e(TAG, "App starting!");

        setupViews();
        setupClickListener();
        loadSettings();
        fixPermissions();
    }

    private void fixPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != 0) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    private void setupViews() {
        this.settingsSaveButton = (Button) findViewById(R.id.settingsSave);
        this.settingsURLText = (TextView) findViewById(R.id.settingsURL);
        this.settingsSecretText = (TextView) findViewById(R.id.settingsSecret);
    }

    private void setupClickListener() {
        this.settingsSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveSettings();
            }
        });
    }

    private void saveSettings() {
        SharedPreferences settings = getSharedPreferences(Settings.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(Settings.SETTINGS_URL, this.settingsURLText.getText().toString());
        editor.putString(Settings.SETTINGS_SECRET, this.settingsSecretText.getText().toString());
        editor.commit();

        Utils.showToast(MainActivity.this, "Settings saved");
    }

    private void loadSettings() {
        SharedPreferences settings = getSharedPreferences(Settings.PREFS_NAME, 0);
        this.settingsURLText.setText(settings.getString(Settings.SETTINGS_URL, ""));
        this.settingsSecretText.setText(settings.getString(Settings.SETTINGS_SECRET, ""));
    }

}
