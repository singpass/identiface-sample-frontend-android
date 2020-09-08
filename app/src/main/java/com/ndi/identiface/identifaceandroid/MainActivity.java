package com.ndi.identiface.identifaceandroid;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import ndi.fbs.android.sdk.NDILogin;

public class MainActivity extends AppCompatActivity {

//  ========== APP VARIABLES SETUP ==========

    // Error Messages
    private boolean didCallAPIWithNoErrors = false;
    private String errorMessage = "";

    // SingPass biometrics database streaming URL
    // URL currently set to: STAGING
    final String STREAMING_URL = "https://stg-bio-stream.singpass.gov.sg";

    // Identiface QuickStart backend API URLs and secret pass
    final String QUICKSTART_BASE = "https://www.identiface.live/api/";
    final String GET_SESSION_TOKEN_URL = QUICKSTART_BASE + "face/verify/token";
    final String VALIDATE_RESULT_URL = QUICKSTART_BASE + "face/verify/validate";
    final String QUICKSTART_PASS = "ndi-api";

    // API Request parameters
    final String serviceID = "SingPass";
    private String nric = "";
    private String sessionToken = "";
    private boolean hasSuccessfullyFaceVerified = false;

    private Intent loginIntent;

    // RequestQueue manages worker threads for running
    // network operations; reading from and writing to
    // cache, and parsing responses.

    // Initialise a RequestQueue
    private RequestQueue requestQueue;

    // Volley takes care of dispatching the
    // response to the Main Thread

    // UI variables
    private Button actionButton;
    private TextView labelText;
    private EditText editText;
    private ProgressBar progressBar;

//  ========== APP VARIABLES SETUP ==========

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NDILogin.registerListener();

        actionButton = findViewById(R.id.actionButton);
        labelText = findViewById(R.id.mainTextLabel);
        editText = findViewById(R.id.nricFieldInput);
        progressBar = findViewById(R.id.progressBar);
        loginIntent = new Intent(this, LoggedIn.class);

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if ((keyEvent != null && (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (i == EditorInfo.IME_ACTION_DONE)) {
                    //do what you want on the press of 'done'
                    loginWithIdentiface(textView);
                }
                return false;
            }
        });

        requestQueue = Volley.newRequestQueue(this);

        // for quick debugging
        editText.setText("G2957839M");
    }

    public void actionButtonPressed(View view) {
        if (sessionToken == "") {
            actionButton.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
            loginWithIdentiface(view);
        } else if (didCallAPIWithNoErrors) {
            labelText.setText("Verifying... please wait");
            actionButton.setVisibility(View.INVISIBLE);
            launchSDK(view);
        } else {
            resetSession();
        }
    }

    public void loginWithIdentiface(View view) {
        labelText.setText("Verifying your identity, please wait...");
        editText.setEnabled(false);
        actionButton.setEnabled(false);

        // Retrieve the NRIC from inputField in UI
        nric = editText.getText().toString();

        // Call getSessionToken API
        getSessionToken(view);
        return;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NDILogin.unregisterListener();
    }

//    =============== SDK METHODS ==================

    public void getSessionToken(View view) {
        // Retrieves a token from the getSessionToken URL
        // to be used by the Identiface SDK
        // returns true on successful retrieval
        // returns false on unsuccessful retrievals

        JSONObject reqJSON = new JSONObject();
        try {
            reqJSON.put("user_id", nric);
            reqJSON.put("service_id", serviceID);
            reqJSON.put("pw", QUICKSTART_PASS);
        } catch (JSONException e) {
            e.printStackTrace();
            didCallAPIWithNoErrors = false;
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                GET_SESSION_TOKEN_URL,
                reqJSON,
                new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {

                    String token = response.getString("token");
                    sessionToken = token;

                    Snackbar snackBar = Snackbar.make(view, "Success! Tap the button to begin facial verification.", 2000);
                    snackBar.show();

                    labelText.setText("Ready to scan your face!");
                    actionButton.setEnabled(true);
                    actionButton.setText("Scan my face");
                    actionButton.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.colorSuccess));
                    editText.setEnabled(false);

                    didCallAPIWithNoErrors = true;
                } catch (Exception e) {
                    Log.d("Response Error", response.toString());
                    didCallAPIWithNoErrors = false;
                    return;
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                int statusCode = error.networkResponse.statusCode;
                String msg = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                JSONObject jsonResp = new JSONObject();
                JSONObject jsonMsg = new JSONObject();
                String errorDesc = "";
                try {
                    jsonResp = new JSONObject(msg);
                    jsonMsg = new JSONObject(jsonResp.getString("message"));
                    errorDesc = jsonMsg.getString("error_description");
                } catch (Exception e) {
                    Log.d("Exception", "Line 157");
                    e.printStackTrace();
                }

                Log.d("Response Code Error", Integer.toString(statusCode));
                Log.d("Response Error Message", msg);
                errorMessage = "Error Status: " + Integer.toString(statusCode) +
                ", " + errorDesc;

                didCallAPIWithNoErrors = false;

                Snackbar errorSnackbar = Snackbar.make(view, errorMessage, 2000);
                errorSnackbar.show();

                labelText.setText("Incorrect NRIC/FIN number. Try again.");
                editText.setEnabled(true);
                actionButton.setEnabled(true);

                return;
            }
        });

        // set a new RequestQueue
        requestQueue.add(jsonObjectRequest);

        return;
    }

    public void launchSDK(View view) {
        progressBar.setVisibility(View.VISIBLE);
        NDILogin.Build(this)
        .streamingUrl(STREAMING_URL)
        .sessionToken(sessionToken)
        .progress(progressResponse -> {
            Log.d("PROGRESS",String.valueOf(progressResponse.getProgress()));
            progressBar.setProgress((int) progressResponse.getProgress(), true);
        })
        .launch(iproovResponse -> {
            if(iproovResponse.getReason() != null) {
                // CUSTOM UI Styles
                Snackbar snackbar = Snackbar.make(view, iproovResponse.getReason(), 5000);
                snackbar.show();

                // Set force pass matching
                if (nric.equals("G2957839M")) {
                    validateResult(view);
                }
                resetSession();
            } else if (iproovResponse.isSuccess()) {
                validateResult(view);
            }
        });
    }

    public void validateResult(View view) {
        // Validates the results returned by
        // the SDK. This is to prevent
        // man-in-the-middle attacks
        // by verifying the results
        // from the trusted source

        JSONObject reqJSON = new JSONObject();
        try {
            reqJSON.put("user_id", nric);
            reqJSON.put("service_id", serviceID);
            reqJSON.put("pw", QUICKSTART_PASS);
            reqJSON.put("token", sessionToken);
        } catch (JSONException e) {
            e.printStackTrace();
            didCallAPIWithNoErrors = false;
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.POST,
            VALIDATE_RESULT_URL,
            reqJSON,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        hasSuccessfullyFaceVerified = response.getBoolean("is_passed");

                        if (hasSuccessfullyFaceVerified) {
                            Snackbar snackbar = Snackbar.make(view, "Face verification successful!", 5000);
                            snackbar.show();

                            labelText.setText("Successful validation!");
                            actionButton.setText("SUCCESS");
                            actionButton.setEnabled(false);

                            startActivity(loginIntent);
                        } else {
                            Log.d("VALIDATE RESPONSE", response.toString());
                            String score = response.get("score").toString();
                            Snackbar snackbar = Snackbar.make(view, "Score: " + score + ". Face verification unsuccessful!", 5000);
                            snackbar.show();
                            resetSession();
                        }

                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d("ERROR", "error");
                        return;
                    }
                }
            }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                int statusCode = error.networkResponse.statusCode;
                String msg = new String(error.networkResponse.data, StandardCharsets.UTF_8);

                JSONObject jsonMsg = new JSONObject();
                String errorDesc = "";
                try {
                    jsonMsg = new JSONObject(msg);
                    errorDesc = jsonMsg.getString("error_description");
                } catch (Exception e) {
                    Log.d("Exception", "Line 281");
                    e.printStackTrace();
                }

                Log.d("Response Code Error", Integer.toString(statusCode));
                Log.d("Response Error Message", msg);
                errorMessage = "Error Status: " + Integer.toString(statusCode) +
                        ", " + errorDesc;

                didCallAPIWithNoErrors = false;

                Snackbar snackbar = Snackbar.make(view, "Face verification failed!" + errorMessage, 5000);
                snackbar.show();

                labelText.setText("Try again?");
                actionButton.setText("Verify my identity");
                editText.setEnabled(true);

                return;
            }
        }
        );

        requestQueue.add(jsonObjectRequest);

        return;
    }

    public void resetSession() {
        sessionToken = "";
        labelText.setText("Try again?");
        actionButton.setText("Verify my identity");
        actionButton.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.colorPrimary));
        progressBar.setVisibility(View.INVISIBLE);
        actionButton.setVisibility(View.VISIBLE);
        editText.setEnabled(true);
    }

//    =============== SDK METHODS ==================

}