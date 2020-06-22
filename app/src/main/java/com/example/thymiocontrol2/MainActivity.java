package com.example.thymiocontrol2;

import android.annotation.SuppressLint;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.thymiocontrol2.control.Log;
import com.example.thymiocontrol2.control.Roboter;
import com.example.thymiocontrol2.proto2pattern.IrCommand;
import com.example.thymiocontrol2.services.IRMessageManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;


/**
 *  Dieses Projekt verwendet folgende Libraries:
 *  (1) Skydove's ColorPickerPreference: https://github.com/skydoves/ColorPickerPreference
 *  (2) Teile aus Timnew's Android Infrared Library: https://github.com/timnew/AndroidInfrared
 */

public class MainActivity extends AppCompatActivity {

    public static int freq = 36000;     //Frequenz des IR Signals
    private int multiply = 1000000/freq;    //Multiplikator. Wird für die modulation des IR Signals verwendet

    boolean enableSlowDownTimer = false;    //"SlowdownTimer" ein Feature dass eine Motorbremse simuliert
    byte randomNumber = 0;  //eine Zufällige Nummer. Muss geschickt werden damit wiederholte IR Nachrichten unterschieden werden.


    BottomNavigationView bottomNavigationView;
    private Roboter roboter;
    private Button buttonUp, buttonDown, buttonLeft, buttonRight;
    private TextView speedView;
    private Handler slowDownTimer;
    private View colorPickerView;

    private boolean tempLeft = false, tempRight = false, tempBackwards = false, tempHeadway = false;    //Hilfsvariablen. Notwendig um die Flut an IR Nachrichten zu veringern.


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        //Buttons initialisieren
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        buttonUp = findViewById(R.id.ButtonUp);
        buttonDown = findViewById(R.id.ButtonDown);
        buttonLeft = findViewById(R.id.ButtonLeft);
        buttonRight = findViewById(R.id.ButtonRight);
        speedView = findViewById(R.id.speed);
        colorPickerView = findViewById(R.id.colorPickerView);



        ConsumerIrManager manager = (ConsumerIrManager)getSystemService(CONSUMER_IR_SERVICE);
        if(manager != null) {

            roboter = Roboter.getRoboter(); //initialisiert das Hilfsobjekt roboter.
            slowDownTimer = new Handler(); //siehe enableSlowDownTimer

            startService(new Intent(MainActivity.this,IRMessageManager.class));

            bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.mainPage:
                            break;
                        case R.id.logPage:
                            //Wechseln zur Logfile Activity
                            Intent intent = new Intent(MainActivity.this,LogActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                            break;
                        case R.id.reset:
                            //Thymio Reset
                            Reset();
                            break;
                        default: Toast.makeText(MainActivity.this,"FAILED",Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
            });



            buttonUp.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    tempHeadway = !tempHeadway;
                    if(tempHeadway) Headway(null);
                    return true;
                }
            });

            buttonRight.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    tempRight = !tempRight;
                    if(tempRight) TurnRight(null);
                    return false;
                }
            });

            buttonLeft.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    tempLeft = !tempLeft;
                    if(tempLeft) TurnLeft(null);
                    return false;
                }
            });

            buttonDown.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    tempBackwards = !tempBackwards;
                    if(tempBackwards) Backwards(null);
                    return false;
                }


            });
        } else {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Kritischer Fehler")
                    .setMessage("Dieses Gerät verfügt nicht über eine Infrarot Schnittstelle und darf diese App nicht verwenden.")
                    .show();
        }










        //*******************************************END ONCREATE*********************************************************************
    }

    @Override
    protected void onStart() {
        // Activity aktualisieren
        UpdateButton();
        UpdateColorButton();

        // ************************************************* SLOWDOWN TIMER ************************************************************
        // Ein Feature dass die Motorbremse eines Autos simuliert.
        if(slowDownTimer == null && enableSlowDownTimer) slowDownTimer = new Handler();

        final Runnable slowDownProcess = new Runnable() {
            public void run() {
                SlowDown();
                slowDownTimer.postDelayed(this, 1000);
            }
        };
        if(enableSlowDownTimer) slowDownTimer.postDelayed(slowDownProcess, 1000);
        //*********************************************************************************************************************************
        super.onStart();
    }

    @Override
    protected void onStop() {
        stopService(new Intent(MainActivity.this, IRMessageManager.class));
        super.onStop();
    }


    /**
     * Öffnet den Color Picker Dialog
     * Der Parameter View v kann null gesetzt werden. Er ist nur notwendig für die Signatur eines Button onClick headers
     * @param  v  View
     */
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
                                //Wir können nur zahlen bis einschließlich 31 verschicken.
                                //Deshalb müssen wir den Farb-integer (0-255) durch 8 divieren (0-31)
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

    /**
     * Updated die Steuerungs Buttons
     * Deaktiviert die Steuerungs Buttons im "DRIVE AUTONOMOUS" modus.
     * Aktiviert die Steuerungs Buttons im "DRIVE MANUAL" modus.
     */
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


    /**
     * Updated die Color Buttons.
     * Versteckt den Color Picker im "COLOR MODE AUTO" mode.
     * Aktiviert den Color Picker im "COLOR MODE MANUAL" mode.
     */
    public void UpdateColorButton() {
        if(roboter.getColormode() == Roboter.ROBOTER_COLOR_MODE_AUTO) {
            colorPickerView.setVisibility(View.GONE);
        } else if(roboter.getColormode() == Roboter.ROBOTER_COLOR_MODE_MANUAL){
            colorPickerView.setVisibility(View.VISIBLE);
            colorPickerView.setBackgroundColor(Color.rgb(roboter.getColorR(),roboter.getColorG(),roboter.getColorB()));
        }
    }

    /**
     * Lenkt den Roboter nach rechts
     * Der Parameter View v kann null gesetzt werden. Er ist nur notwendig für die Signatur eines Button onClick headers
     * @param  v  View
     */
    public void TurnRight(View v) {
        if(roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL) {
            if(randomNumber < 60) randomNumber++;
            else randomNumber = 0;
            SendIR(buildRC5(0, 4, randomNumber));
        }
    }

    /**
     * Lenkt den Roboter nach links
     * Der Parameter View v kann null gesetzt werden. Er ist nur notwendig für die Signatur eines Button onClick headers
     * @param  v  View
     */
    public void TurnLeft(View v) {
        if(roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL) {
            if(randomNumber < 60) randomNumber++;
            else randomNumber = 0;
            SendIR(buildRC5(0, 5, randomNumber));

        }
    }

    /**
     * Beschleunigt den Roboter
     * Der Parameter View v kann null gesetzt werden. Er ist nur notwendig für die Signatur eines Button onClick headers
     * @param  v  View
     */
    public void Headway(View v) {
        if(roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL) {
            SendIR(buildRC5(0, 2, roboter.accelerate(Roboter.ROBOTER_ACCELERATE)));
        }
    }

    /**
     * Motorbremse. Siehe SLOW DOWN Timer.
     */
    public void SlowDown() {
        if(roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL && roboter.getSpeed() > 1) {
            SendIR(buildRC5(0, 2, roboter.accelerate(Roboter.ROBOTER_SLOW_DOWN)));
        }
    }

    /**
     * Stopt den Roboter. (STOP Button)
     * Der Parameter View v kann null gesetzt werden. Er ist nur notwendig für die Signatur eines Button onClick headers
     * @param  v  View
     */

    public void Break(View v) {
            SendIR(buildRC5(0, 10, roboter.accelerate(roboter.getSpeed()*(-1))));
                try {
                     if(roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_AUTO){
                         //Wenn der Roboter im Farmodus "AUTO" ist stoppt er und wechselt auf "MANUAL"
                        roboter.setStatus(Roboter.ROBOTER_DRIVE_MODE_MANUAL);
                         SendIR(buildRC5(0,1,roboter.getStatus()));
                         UpdateButton();
                         ((MaterialButtonToggleGroup)findViewById(R.id.toggleButton)).check(R.id.DriveModeManual);
                    }
                } catch (Exception e) {
                    Toast.makeText(this,"FAILED",Toast.LENGTH_LONG).show();
                    Log.insert(e.toString());
                }
    }

    /**
     * Fährt den Roboter rückwärts oder bremst ihn
     * Der Parameter View v kann null gesetzt werden. Er ist nur notwendig für die Signatur eines Button onClick headers
     * @param  v  View
     */
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


    /**
     * Ändert den Farbmodus.
     * Wenn Der Farbmodus automatisch ist: wechselt auf MANUAL.
     * Wenn der Farmodus manual ist: wechselt auf AUTO.
     * Ruft die Fuktion "UpdateColorButton" und "SendIR" auf.
     * Der Parameter View v kann null gesetzt werden. Er ist nur notwendig für die Signatur eines Button onClick headers
     * @param  v  View
     */
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


    /**
     * Ändert den Fahrmodus.
     * Wenn Der Fahrmodus automatisch ist: wechselt auf MANUAL.
     * Wenn der Fahrmodus manual ist: wechselt auf AUTO.
     * Ruft die Fuktion "UpdateButton" und "SendIR" auf.
     * Der Parameter View v kann null gesetzt werden. Er ist nur notwendig für die Signatur eines Button onClick headers
     * @param  v  View
     */
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

    /**
     * Aktualisiert das TextView "SpeedView" mit dem aktuellen roboter speed
     */
    public void UpdateSpeedView() {
        if (roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_MANUAL)
            speedView.setText(String.valueOf(roboter.getSpeed()));
        else if (roboter.getStatus() == Roboter.ROBOTER_DRIVE_MODE_AUTO)
            speedView.setText("AUTO");
    }

    /**
     * Sendet ein IR Signal um die Farbe des Roboters zu ändern.
     * Wenn Der Fahrmodus automatisch ist: Sendet ein IR Signal "COLOR MODE AUTO"
     * Wenn der Fahrmodus manual ist: Sendet ein IR Signal "COLOR MODE MANUAL" gefolgt von den RGB Farben.
     */
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

    /**
     * Hilfsfunktion um ein IR Signal zu senden
     * sendet ein Intent an den IRMessageManager, der verschickt die IR Signale.
     * Ruft die Funktion "UpdateSpeedView" auf.
     * @param  pattern  Pattern das gesendet werden soll. Siehe buildRC5
     */
    public void SendIR(final int[] pattern) {
        UpdateSpeedView();
        Intent ircmd = new Intent(this, IRMessageManager.class);
        ircmd.putExtra(IRMessageManager.PARAM_IN_MSG, pattern);
        startService(ircmd);

    }

    /**
     * Erstellt ein pattern, das mit SendIR gesendet werden kan
     * Im Falle eines Overflows wird ein leeres pattern zurück gegeben und ein Fehler in die Log geschrieben.
     * Siehe Aufbau des RC5 Protokolls
     * @param  toggleBit  0 oder 1. Wird in diesem Sachverhalt nicht verwendet.
     * @param systemadr System Adresse (0-31). Siehe RC5 Protokoll.
     * @param command Command (0-63). Siehe RC5 Protokoll.
     */
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

    /**
     * Setzt den Thymio Roboter zurück auf einen 'leerlauf' Status
     */
    public void Reset() {
        try {
            Break(null);
            roboter.setStatus(Roboter.ROBOTER_DRIVE_MODE_MANUAL);
            roboter.setColormode(Roboter.ROBOTER_COLOR_MODE_AUTO);
            SendIR(buildRC5(0,31,31));
            //UpdateColorButton();
            //UpdateButton();
            buttonUp.setEnabled(false);
            buttonLeft.setEnabled(false);
            buttonRight.setEnabled(false);
            buttonDown.setEnabled(false);
            findViewById(R.id.stopButton).setEnabled(false);
            colorPickerView.setVisibility(View.GONE);
        } catch( Exception e) {
            Toast.makeText(MainActivity.this,"FAILED",Toast.LENGTH_LONG).show();
            Log.insert(e.toString());
        }
    }
}