package com.example.apple.android_nfc_reader_cardio_braintree;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

public abstract class BaseActivity extends AppCompatActivity implements View.OnClickListener {

    public Toolbar toolbar;

    public void setupBaseToolbar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onClick(View v) {
    }
}
