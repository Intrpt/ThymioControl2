package com.example.thymiocontrol2.control;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;

public class Log {
    private static LinkedList<String> logfile;
    private static int threshold = 50;

    private Log() { }

    public static void insert(String str) {
        cLogfile();



        logfile.add("["+ DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Calendar.getInstance().getTime())+"] "+str);
        if(logfile.size() > threshold) logfile.removeFirst();
    }

    private static void cLogfile() {
        if(logfile == null) logfile = new LinkedList<>();
    }

    public static LinkedList<String> get() {
        cLogfile();
        return logfile;
    }

}
