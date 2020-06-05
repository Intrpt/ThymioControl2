package com.example.thymiocontrol2.control;

public class Roboter {

    public static int ROBOTER_DRIVE_MODE_AUTO = 1;
    public static int ROBOTER_DRIVE_MODE_MANUAL = 0;

    private static int maxSpeed = 300;
    private int speed = 0;
    private static Roboter roboter;
    private int status = 0;

    private Roboter() {
        status = ROBOTER_DRIVE_MODE_MANUAL;
    }

    public static Roboter getRoboter() {
        if(roboter == null) roboter = new Roboter();
        return roboter;
    }

    public int accelerate(int dif) {
        speed += dif;
        return speed;
    }

    public void setStatus(int status) throws Exception {
        if(status == ROBOTER_DRIVE_MODE_MANUAL || status == ROBOTER_DRIVE_MODE_AUTO) {
            this.status = status;
        } else throw new Exception("Unknown Roboter Status, Please use ROBOTER_DRIVE_MODE_MANUAL or ROBOTER_DRIVE_MODE_AUTO");
    }

    public int getStatus() {
        return this.status;
    }

    public int getSpeed() {
        return this.speed;
    }
}
