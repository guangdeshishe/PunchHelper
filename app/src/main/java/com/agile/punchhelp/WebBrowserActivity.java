package com.agile.punchhelp;

import android.app.ActionBar;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class WebBrowserActivity extends AppCompatActivity {
    private PluginWebView mWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        mWebView = findViewById(R.id.mWebView);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {

        }
    }
}
