package com.example.skipad;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    public static boolean isGrabRedEnvelopeEnabled;
    public static int redEnvelopeDelayTime;
    private CheckBox grabRedEnvelopeCheckBox;
    private SharedPreferences.Editor preferencesEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);  // Modern edge-to-edge approach
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText grabDelayEditText = findViewById(R.id.second_et);
        Button openAccessibilitySettingsButton = findViewById(R.id.open_btn);
        Button openAppDetailsSettingsButton = findViewById(R.id.open_alert_window_btn);
        grabRedEnvelopeCheckBox = findViewById(R.id.grab_cb);

        openAccessibilitySettingsButton.setOnClickListener(this);
        openAppDetailsSettingsButton.setOnClickListener(this);
        grabRedEnvelopeCheckBox.setOnCheckedChangeListener(this);

        SharedPreferences timeSettingsPreferences = getSharedPreferences("time_num", MODE_PRIVATE);
        preferencesEditor = timeSettingsPreferences.edit();

        // Set initial values
        int savedDelayTime = timeSettingsPreferences.getInt("delay_time", 500);
        grabDelayEditText.setText(String.valueOf(savedDelayTime));
        grabRedEnvelopeCheckBox.setChecked(timeSettingsPreferences.getBoolean("open_grab", true));

        // Apply initial settings
        redEnvelopeDelayTime = savedDelayTime;
        isGrabRedEnvelopeEnabled = grabRedEnvelopeCheckBox.isChecked();

        // Add TextWatcher for the EditText
        grabDelayEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String text = charSequence.toString();
                try {
                    int value = Integer.parseInt(text);
                    redEnvelopeDelayTime = value;
                    preferencesEditor.putInt("delay_time", value);
                    preferencesEditor.apply();
                } catch (NumberFormatException e) {
                    // Optionally reset to previous value or show a warning
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.open_btn) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } else if (view.getId() == R.id.open_alert_window_btn) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        isGrabRedEnvelopeEnabled = isChecked;
        grabRedEnvelopeCheckBox.setTextColor(isChecked ? Color.GREEN : Color.RED);

        // Save preference
        preferencesEditor.putBoolean("open_grab", isChecked);
        preferencesEditor.apply();
    }
}
