package com.example.thymiocontrol2.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.hardware.ConsumerIrManager;

import com.example.thymiocontrol2.MainActivity;


public class IRMessageManager extends IntentService {

    public static final String PARAM_IN_MSG = "pattern";

    private static ConsumerIrManager manager;

    public IRMessageManager() {
        super("IRMessageManager");
    }

    @Override
    public void onCreate() {
        manager = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            int[]pattern = intent.getIntArrayExtra(PARAM_IN_MSG);
            if(pattern != null && pattern.length > 0) {
                manager.transmit(MainActivity.freq,pattern);
            }
        }
    }

}
