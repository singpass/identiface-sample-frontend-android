package com.ndi.identiface.identifaceandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

public class LoggedIn extends AppCompatActivity {

    Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logged_in);

        logoutButton = findViewById(R.id.logoutButton);
    }

    public void logoutButtonPressed(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        return;
    }
}