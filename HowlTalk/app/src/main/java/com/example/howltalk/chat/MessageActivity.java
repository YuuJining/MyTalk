package com.example.howltalk.chat;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import com.example.howltalk.R;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

public class MessageActivity extends AppCompatActivity {
    private FirebaseRemoteConfig firebaseRemoteConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        firebaseRemoteConfig = firebaseRemoteConfig.getInstance();

        String splash_background = firebaseRemoteConfig.getString(getString(R.string.rc_color));
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor(splash_background));
        }
    }
}