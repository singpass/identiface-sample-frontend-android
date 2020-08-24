package com.ndi.identiface.identifaceandroid;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;

import ndi.fbs.android.sdk.NDILogin;

public class MainActivity extends AppCompatActivity {

    String nric;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NDILogin.registerListener();

        EditText editText = findViewById(R.id.nricFieldInput);
        nric = editText.getText().toString();
    }

    @Override
    protected void onDestroy() {
//        NDILogin.unregisterListener();
        super.onDestroy();
    }
}