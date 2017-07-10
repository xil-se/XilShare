package se.xil.instashare;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "InstaShareSettings";
    private static final String SETTINGS_URL = "SETTINGS_URL";
    private static final String SETTINGS_SECRET= "SETTINGS_SECRET";
    private Button settingsSaveButton;
    private TextView settingsURLText;
    private TextView settingsSecretText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupViews();
        setupClickListener();
        loadSettings();
    }

    private void setupViews() {
        this.settingsSaveButton = (Button) findViewById(R.id.settingsSave);
        this.settingsURLText = (TextView) findViewById(R.id.settingsURL);
        this.settingsSecretText = (TextView) findViewById(R.id.settingsSecret);
    }

    private void setupClickListener() {
        settingsSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveSettings();
            }
        });
    }

    private void saveSettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SETTINGS_URL, this.settingsURLText.getText().toString());
        editor.putString(SETTINGS_SECRET, this.settingsSecretText.getText().toString());
        editor.commit();
    }

    private void loadSettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        this.settingsURLText.setText(settings.getString(SETTINGS_URL, ""));
        this.settingsSecretText.setText(settings.getString(SETTINGS_SECRET, ""));
    }

}
