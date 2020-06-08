package com.example.thymiocontrol2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.example.thymiocontrol2.MainActivity;
import com.example.thymiocontrol2.R;
import com.example.thymiocontrol2.control.Log;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

public class LogActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    ListView logList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        logList = findViewById(R.id.log);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.mainPage:
                        Intent intent = new Intent(LogActivity.this,MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        break;
                    case R.id.logPage:
                        break;
                }
                return false;
            }
        });

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, Log.get());
        logList.setAdapter(arrayAdapter);


    }
}