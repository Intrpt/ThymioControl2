package com.example.thymiocontrol2;

import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.thymiocontrol2.control.Roboter;
import com.example.thymiocontrol2.proto2pattern.IrCommand;
import com.google.android.material.bottomnavigation.BottomNavigationView;


public class MainActivity extends AppCompatActivity {

    public static int freq = 36000;
    private int multiply = 1000000/freq;

    private boolean enableSlowDownTimer = true;

    ConsumerIrManager manager;

    private BottomNavigationView bottomNavigationView;
    private Roboter roboter;
    private Button buttonUp, buttonDown, buttonLeft, buttonRight;
    private TextView speedView;
    private Handler slowDownTimer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        buttonUp = findViewById(R.id.ButtonUp);
        buttonDown = findViewById(R.id.ButtonDown);
        buttonLeft = findViewById(R.id.ButtonLeft);
        buttonRight = findViewById(R.id.ButtonRight);
        speedView = findViewById(R.id.speed);

        roboter = Roboter.getRoboter();
        slowDownTimer = new Handler();
        manager = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);


        buttonUp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Headway();
                return false;
            }
        });

        buttonRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                TurnRight();
                return false;
            }
        });

        buttonLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                TurnLeft();
                return false;
            }
        });

        buttonDown.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Backwards();
                return false;
            }
        });


        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return false;
            }
        });


        //*******************************************END ONCREATE*********************************************************************
    }

    @Override
    protected void onStart() {
        UpdateButton();
        if(slowDownTimer == null) slowDownTimer = new Handler();

        final Runnable slowDownProcess = new Runnable() {
            public void run() {
                SlowDown();
                slowDownTimer.postDelayed(this, 1000);
            }
        };

        if(enableSlowDownTimer) slowDownTimer.postDelayed(slowDownProcess, 1000);
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void UpdateButton() {
        if(roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_AUTO) {
            buttonDown.setClickable(false);
            buttonLeft.setClickable(false);
            buttonRight.setClickable(false);
            buttonDown.setClickable(false);
        } if(roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL) {
            buttonDown.setClickable(true);
            buttonLeft.setClickable(true);
            buttonRight.setClickable(true);
            buttonDown.setClickable(true);
        }
    }

    public void TurnRight() {
        if(roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL) {
            SendIR(buildRC5(0, 4, 0));
        }
    }

    public void TurnLeft() {
        if(roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL) {
            SendIR(buildRC5(0, 5, 0));
        }
    }

    public void Headway() {
        if(roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL) {
            SendIR(buildRC5(0, 2, roboter.accelerate(50)));
        }
    }

    public void SlowDown() {
        if(roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL && roboter.getSpeed() > 1) {
            SendIR(buildRC5(0, 2, roboter.accelerate(-1)));
        }
    }

    public void Backwards() {
        if(roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL) {
            if(roboter.getSpeed() >= 100) {
                SendIR(buildRC5(0, 2, roboter.accelerate(-100)));
            } else if(roboter.getSpeed() > 0) {
                SendIR(buildRC5(0, 2, 0));
            } else {
                SendIR(buildRC5(0, 2, roboter.accelerate(-50)));
            }
        }
    }

    public void ChangeRoboterMode(View v){
        try {
            if(v == findViewById(R.id.DriveModeAuto)) {
                roboter.setStatus(Roboter.ROBOTER_DRIVE_MODE_AUTO);
            } else if(v == findViewById(R.id.DriveModeManual)){
                roboter.setStatus(Roboter.ROBOTER_DRIVE_MODE_MANUAL);
            } else {
                Toast.makeText(this,"FAILED",Toast.LENGTH_LONG).show();
                return;
            }
            SendIR(buildRC5(0,1,roboter.getStatus()));
            UpdateButton();
        } catch (Exception e) { }
    }

    public void UpdateSpeedView() {
        if (roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL)
            speedView.setText(roboter.getSpeed());
        else if (roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_AUTO)
            speedView.setText("AUTO");
    }


    public void SendIR(final int[] pattern) {
        UpdateSpeedView();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                manager.transmit(freq,pattern);
            }
        });
        t.start();
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