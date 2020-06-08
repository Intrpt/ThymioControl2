package com.example.thymiocontrol2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
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

import com.example.thymiocontrol2.control.Log;
import com.example.thymiocontrol2.control.Roboter;
import com.example.thymiocontrol2.proto2pattern.IrCommand;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;


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
    private View colorPickerView;


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
        colorPickerView = findViewById(R.id.colorPickerView);

        roboter = Roboter.getRoboter();
        slowDownTimer = new Handler();
        manager = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.mainPage:
                        break;
                    case R.id.logPage:
                        Intent intent = new Intent(MainActivity.this,LogActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        break;
                    case R.id.reset:
                        //RESET
                    default: Toast.makeText(MainActivity.this,"FAILED",Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });



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




        //*******************************************END ONCREATE*********************************************************************
    }

    @Override
    protected void onStart() {
        UpdateButton();
        if(slowDownTimer == null && enableSlowDownTimer) slowDownTimer = new Handler();

        final Runnable slowDownProcess = new Runnable() {
            public void run() {
                SlowDown();
                slowDownTimer.postDelayed(this, 1000);
            }
        };

        if(enableSlowDownTimer) slowDownTimer.postDelayed(slowDownProcess, 1000);
        UpdateColorButton();
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }



    public void OpenColorPickerView(View v) {
        if(roboter.getColormode() == Roboter.ROBOTER_COLOR_MODE_MANUAL) {
            try {
                new ColorPickerDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                        .setTitle("Select Color")
                        .setPreferenceName("Colorpicker")
                        .setPositiveButton("OK", new ColorEnvelopeListener() {
                            @Override
                            public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                                int[] argb = envelope.getArgb();
                                roboter.setColor(argb[1], argb[2], argb[3]);
                                UpdateRoboterColor();
                                UpdateColorButton();
                            }
                        })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .attachAlphaSlideBar(false)
                        .attachBrightnessSlideBar(false)
                        .show();
            } catch (Exception e) {
                Toast.makeText(this,"FAILED",Toast.LENGTH_LONG).show();
                Log.insert(e.toString());
            }
        }
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

    public void UpdateColorButton() {
        if(roboter.getColormode() == Roboter.ROBOTER_COLOR_MODE_AUTO) {
            colorPickerView.setVisibility(View.GONE);
        } else if(roboter.getColormode() == Roboter.ROBOTER_COLOR_MODE_MANUAL){
            colorPickerView.setVisibility(View.VISIBLE);
            colorPickerView.setBackgroundColor(Color.rgb(roboter.getColorR(),roboter.getColorG(),roboter.getColorB()));
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
            SendIR(buildRC5(0, 2, roboter.accelerate(Roboter.ROBOTER_ACCELERATE)));
        }
    }

    public void SlowDown() {
        if(roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL && roboter.getSpeed() > 1) {
            SendIR(buildRC5(0, 2, roboter.accelerate(Roboter.ROBOTER_SLOW_DOWN)));
        }
    }


    public void Backwards() {
        if(roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL) {
            if(roboter.getSpeed() > Roboter.ROBOTER_ACCELERATE*(-1)) {
                SendIR(buildRC5(0, 2, roboter.accelerate(Roboter.ROBOTER_ACCELERATE*(-1))));
            } else if(roboter.getSpeed() > 0) {
                SendIR(buildRC5(0, 2, 0));
                roboter.accelerate(roboter.getSpeed()*(-1));
            } else {
                SendIR(buildRC5(0, 2, roboter.accelerate(Roboter.ROBOTER_ACCELERATE*(-1))));
            }
        }
    }

    public void ChangeRoboterColorMode(View v) {
        try {
            if(v == findViewById(R.id.ColorModeAuto)) {
                roboter.setColormode(Roboter.ROBOTER_COLOR_MODE_AUTO);
            } else if(v == findViewById(R.id.ColorModeManual)){
                roboter.setColormode(Roboter.ROBOTER_COLOR_MODE_MANUAL);
            } else {
                Toast.makeText(this,"FAILED",Toast.LENGTH_LONG).show();
                Log.insert("FAILED because of clicked view == "+v.toString());
                return;
            }
            SendIR(buildRC5(0,1,roboter.getColormode()));
            UpdateColorButton();
        } catch (Exception e) {
            Toast.makeText(this,"FAILED",Toast.LENGTH_LONG).show();
            Log.insert(e.toString());
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
                Log.insert("FAILED because of clicked view == "+v.toString());
                return;
            }
            SendIR(buildRC5(0,1,roboter.getStatus()));
            UpdateButton();
        } catch (Exception e) {
            Toast.makeText(this,"FAILED",Toast.LENGTH_LONG).show();
            Log.insert(e.toString());
        }
    }

    public void UpdateSpeedView() {
        if (roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL)
            speedView.setText(String.valueOf(roboter.getSpeed()));
        else if (roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_AUTO)
            speedView.setText("AUTO");
    }

    public void UpdateRoboterColor() {
        if(roboter.getColormode() == Roboter.ROBOTER_COLOR_MODE_AUTO) {
            SendIR(buildRC5(0,6,Roboter.ROBOTER_COLOR_MODE_AUTO));
        } else if(roboter.getColormode() == Roboter.ROBOTER_COLOR_MODE_MANUAL) {
            SendIR(buildRC5(0,6,Roboter.ROBOTER_COLOR_MODE_MANUAL));
            SendIR(buildRC5(0,7,roboter.getColorR()));
            SendIR(buildRC5(0,8,roboter.getColorG()));
            SendIR(buildRC5(0,7,roboter.getColorB()));
        } else {
            Toast.makeText(this, "FAILED", Toast.LENGTH_SHORT).show();
            Log.insert("FAILED because of Robotor Colormode == "+roboter.getColormode());
        }
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
        Log.insert("tBit:"+toggleBit+" SysAddr:"+systemadr+" Command:"+command);
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

    public void Reset(View v) {
        try {
            roboter.setStatus(Roboter.ROBOTER_DRIVE_MODE_MANUAL);
            roboter.setColormode(Roboter.ROBOTER_COLOR_MODE_AUTO);
            SendIR(buildRC5(0,32,32));
            UpdateColorButton();
            UpdateButton();
        } catch( Exception e) {
            Toast.makeText(MainActivity.this,"FAILED",Toast.LENGTH_LONG).show();
            Log.insert(e.toString());
        }
    }
}