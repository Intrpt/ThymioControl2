package com.example.thymiocontrol2;

import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.thymiocontrol2.proto2pattern.IrCommand;


public class MainActivity extends AppCompatActivity {

    ConsumerIrManager manager;
    private EditText bitcount, data;
    public static int freq = 36000;
    private int multiply = 1000000/freq;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bitcount = findViewById(R.id.bitCount);
        data = findViewById(R.id.data);

        manager = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);

        buildRC5(1,1,1);   }





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