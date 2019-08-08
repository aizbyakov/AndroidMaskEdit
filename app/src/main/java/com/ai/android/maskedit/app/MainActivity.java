package com.ai.android.maskedit.app;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import com.ai.android.maskedit.MaskEdit;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        createUi();
    }

    private void createUi() {
        MaskEdit maskEdit1 = findViewById(R.id.maskEdit1);
        maskEdit1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, String.format("beforeTextChanged: s = %s, start = %d, count = % d, after = %d", s, start, count, after));
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, String.format("onTextChanged: s = %s, start = %d, count = % d", s, start, count));
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, String.format("afterTextChanged: s = %s", s));
            }
        });
    }
}
