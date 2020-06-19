package com.example.thymiocontrol2.control;


/**
 * Helper Class. Die dazu dient den aktuellen Status des Roboters zu speichern.
 */

public class Roboter {

    /*       STATICS           */
    public static int ROBOTER_DRIVE_MODE_AUTO = 1;
    public static int ROBOTER_DRIVE_MODE_MANUAL = 0;

    public static int ROBOTER_COLOR_MODE_AUTO = 1;
    public static int ROBOTER_COLOR_MODE_MANUAL = 2;


    public static int ROBOTER_ACCELERATE = 1;
    public static int ROBOTER_SLOW_DOWN = -2;
    static int maxSpeed = 63;   //Maximale Speed. Bitte auf 63 lassen. Sonst kommt es zu einem Overflow.


    private static Roboter roboter; //Singleton

    //  Dynanimische variablen
    private int speed = 0;
    private int status;
    private int colormode;
    private int[] rgb = {0,0,0};

    private Roboter() {
        status = ROBOTER_DRIVE_MODE_MANUAL;
        colormode = ROBOTER_COLOR_MODE_AUTO;
    }

    /**
     * Gibt das aktuelle roboter Objekt zurück.
     * Erstellt eins falls noch nicht geschehen (Singleton)
     */
    public static Roboter getRoboter() {
        if(roboter == null) roboter = new Roboter();
        return roboter;
    }

    /**
     * Ändert die Geschwindigkeit des roboter objekts.
     * untere Grenze: 0,    obere Grenze: 63
     * @param  dif  um welchen Wert das Fahrzeug beschleunigt (positiv int) oder bremst/rückwärts fährt (negativ int)
     * @return aktuell geschätzte einheitslose Geschwindigkeit.
     */
    public int accelerate(int dif) {
        speed += dif;
        if(speed > maxSpeed) speed = maxSpeed;
        else if (speed < maxSpeed*(-1)) speed = maxSpeed *(-1);
        return speed;
    }

    /**
     * Setzt den Fahrmodus auf MANUAL oder AUTO
     * throws Exception "Unknown Roboter Status" Wenn die Parameter unbekannt sind.
     * @param  status  setzt den Status auf MANUAL oder AUTO. Nutze dafür die statics ROBOTER_DRIVE_MODE_MANUEL oder ROBOTER_DRIVE_MODE_AUTO
     */
    public void setStatus(int status) throws Exception {
        if(status == ROBOTER_DRIVE_MODE_MANUAL || status == ROBOTER_DRIVE_MODE_AUTO) {
            this.status = status;
        } else throw new Exception("Unknown Roboter Status, Please use ROBOTER_DRIVE_MODE_MANUAL or ROBOTER_DRIVE_MODE_AUTO");
    }

    /**
     * Setzt den Farbmodus auf MANUAL oder AUTO
     * throws Exception "Unknown Roboter Colormode" Wenn die Parameter unbekannt sind.
     * @param  mode  setzt den Status auf MANUAL oder AUTO. Nutze dafür die statics ROBOTER_COLOR_MODE_AUTO oder ROBOTER_COLOR_MODE_MANUAL
     */
    public void setColormode(int mode) throws Exception {
        if(mode == ROBOTER_COLOR_MODE_AUTO || mode == ROBOTER_COLOR_MODE_MANUAL) {
            this.colormode = mode;
        } else throw new Exception("Unknown Roboter Colormode, Please use ROBOTER_COLOR_MODE_AUTO or ROBOTER_COLOR_MODE_MANUAL");
    }

    /**
     * @return Roboter Status. ROBOTER_COLOR_MODE_AUTO oder ROBOTER_COLOR_MODE_MANUAL
     */
    public int getStatus() {
        return this.status;
    }

    /**
     *
     * @return  Aktuell geschätzte Einheitslose Geschwindigkeit. Ist nicht mit der echten Geschwindigkeit synchronisiert.
     */
    public int getSpeed() {
        return this.speed;
    }

    /**
     *
     * @return  Roboter Color Mode. ROBOTER_COLOR_MODE_AUTO oder ROBOTER_COLOR_MODE_MANUAL
     */
    public int getColormode() {
        return this.colormode;
    }

    /**
     * Setzt die RGB Farbe des Roboter Objekts
     * @param r R (RGB)
     * @param g G (RGB)
     * @param b B (RGB)
     */
    public void setColor(int r, int g, int b) {
        rgb[0] = r;
        rgb[1] = g;
        rgb[2] = b;
    }

    /**
     *
     * @return  R von RGB
     */
    public int getColorR() {
        return rgb[0];
    }

    /**
     *
     * @return  G von RGB
     */
    public int getColorG() {
        return rgb[1];
    }

    /**
     *
     * @return  B von RGB
     */
    public int getColorB() {
        return rgb[2];
    }
}
