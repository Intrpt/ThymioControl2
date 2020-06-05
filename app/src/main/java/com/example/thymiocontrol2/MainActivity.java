package com.example.thymiocontrol2;

import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.thymiocontrol2.proto2pattern.IrCommand;
import com.google.android.material.bottomnavigation.BottomNavigationView;


public class MainActivity extends AppCompatActivity {

    public static int freq = 36000;
    private int multiply = 1000000/freq;

    ConsumerIrManager manager;

    private EditText bitcount, data;
    private BottomNavigationView bottomNavigationView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        Button buttonUp = findViewById(R.id.ButtonUp);
        Button buttonDown = findViewById(R.id.ButtonDown);
        Button buttonLeft = findViewById(R.id.ButtonLeft);
        Button buttonRight = findViewById(R.id.ButtonRight);

        manager = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);

        buildRC5(1,1,1);


        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return false;
            }
        });
    }





    public void SendIR(View v) {
        if (manager.hasIrEmitter()) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    manager.transmit(freq,buildRC5(0,1,1));

                }
            });
            t.start();
        }
        else Toast.makeText(this, "KEIN TRANSMITTER", Toast.LENGTH_LONG).show();
    }

    public int[] buildRC5(int toggleBit, int systemadr, int command) {
        long rc5 = 0;
        rc5 = rc5 | (toggleBit<<11);
        rc5 = rc5 | (systemadr<<6);
        rc5 = rc5 |command;
        IrCommand rc5Command=  IrCommand.RC5.buildRC5(12,rc5);
        int[] pattern = rc5Command.pattern;
        for (int d = 0; d < pattern.length; d++) {
            pattern[d] = pattern[d] * multiply;
        }
        return pattern;
    }
}