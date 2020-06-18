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
import com.example.thymiocontrol2.services.IRMessageManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;


public class MainActivity extends AppCompatActivity {

    public static int freq = 36000;
    private int multiply = 1000000/freq;

    private boolean enableSlowDownTimer = false;
    byte randomNumber = 0;

    //ConsumerIrManager manager;

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
        //manager = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);

        startService(new Intent(MainActivity.this,IRMessageManager.class));

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
                        Reset();
                        break;
                    default: Toast.makeText(MainActivity.this,"FAILED",Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });



       /* buttonUp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Headway();
                return false;
            }
        });*/

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

       /* buttonDown.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Backwards();
                return false;
            }
        });*/




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
        stopService(new Intent(MainActivity.this, IRMessageManager.class));
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
                                argb[1] = argb[1]/8;
                                argb[2] = argb[2]/8;
                                argb[3] = argb[3]/8;
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
            buttonUp.setEnabled(false);
            buttonLeft.setEnabled(false);
            buttonRight.setEnabled(false);
            buttonDown.setEnabled(false);
            //findViewById(R.id.stopButton).setEnabled(false);
        } if(roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL) {
            buttonUp.setEnabled(true);
            buttonLeft.setEnabled(true);
            buttonRight.setEnabled(true);
            buttonDown.setEnabled(true);
            //findViewById(R.id.stopButton).setEnabled(true);
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
            if(randomNumber < 60) randomNumber++;
            else randomNumber = 0;
            SendIR(buildRC5(0, 4, randomNumber));
        }
    }

    public void TurnLeft() {
        if(roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL) {
            if(randomNumber < 60) randomNumber++;
            else randomNumber = 0;
            SendIR(buildRC5(0, 5, randomNumber));
        }
    }

    public void Headway(View v) {
        if(roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL) {
            SendIR(buildRC5(0, 2, roboter.accelerate(Roboter.ROBOTER_ACCELERATE)));
        }
    }

    public void SlowDown() {
        if(roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL && roboter.getSpeed() > 1) {
            SendIR(buildRC5(0, 2, roboter.accelerate(Roboter.ROBOTER_SLOW_DOWN)));
        }
    }

    public void Break(View v) {
        if(roboter.getSpeed() !=  0) {
            SendIR(buildRC5(0, 10, roboter.accelerate(roboter.getSpeed()*(-1))));
        }
    }


    public void Backwards(View v) {
        if(roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL) {
            if(roboter.getSpeed() > Roboter.ROBOTER_ACCELERATE*(-1)) {
                SendIR(buildRC5(0, 3, roboter.accelerate(Roboter.ROBOTER_ACCELERATE*(-1))));
            } else if(roboter.getSpeed() > 0) {
                SendIR(buildRC5(0, 3, 0));
                roboter.accelerate(roboter.getSpeed()*(-1));
            } else {
                SendIR(buildRC5(0, 3, roboter.accelerate(Roboter.ROBOTER_ACCELERATE*(-1))));
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
            SendIR(buildRC5(0,6,roboter.getColormode()));
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
            SendIR(buildRC5(0,9,roboter.getColorB()));
        } else {
            Toast.makeText(this, "FAILED", Toast.LENGTH_SHORT).show();
            Log.insert("FAILED because of Robotor Colormode == "+roboter.getColormode());
        }
    }

    public void SendIR(final int[] pattern) {
        UpdateSpeedView();
        Intent ircmd = new Intent(this, IRMessageManager.class);
        ircmd.putExtra(IRMessageManager.PARAM_IN_MSG, pattern);
        startService(ircmd);

    }
    public int[] buildRC5(int toggleBit, int systemadr, int command) {
        long rc5 = 0;
        if(command < 0) command = Math.abs(command);
        if (command > 63) {
            Log.insert("WARNING! Overflow: tBit:"+toggleBit+" SysAddr:"+systemadr+" Command:"+command);
            return new int[0];
        } else if (systemadr > 31) {
            Log.insert("WARNING! Overflow: tBit:"+toggleBit+" SysAddr:"+systemadr+" Command:"+command);
            return new int[0];
        } else {
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
    }

    public void Reset() {
        try {
            Break(null);
            roboter.setStatus(Roboter.ROBOTER_DRIVE_MODE_MANUAL);
            roboter.setColormode(Roboter.ROBOTER_COLOR_MODE_AUTO);
            SendIR(buildRC5(0,31,31));
            UpdateColorButton();
            UpdateButton();
        } catch( Exception e) {
            Toast.makeText(MainActivity.this,"FAILED",Toast.LENGTH_LONG).show();
            Log.insert(e.toString());
        }
    }
}