package com.example.androidserver;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SimpleHttpServer simpleHttpServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WebConfiguration webConfiguration = new WebConfiguration();
        webConfiguration.setPort(8088);
        webConfiguration.setMaxParallelsl(50);
        simpleHttpServer = new SimpleHttpServer(webConfiguration);
        simpleHttpServer.startAsync();
    }

    @Override
    protected void onDestroy() {
        try {
            simpleHttpServer.stopAsync();
        } catch (IOException e) {
            Log.e(TAG, "onDestroy: "+e.toString() );
        }
        super.onDestroy();
    }
}
