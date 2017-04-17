package tango.rajantechie.us.scorpiontango;

/**
 * Created by rajan on 4/14/2017.
 */

public class CommandsLobot {

public static final String FORWARD = "1";
public static final String REVERSE = "2";
public static final String LEFTTURN = "3";
public static final String RIGHTTURN = "4";
public static final String ACTION_FOR_16 = "5";
public static final String ACTION_REV_16 = "6";
public static final String ACTION_FOR_17 = "7";
public static final String ACTION_REV_18 = "8";
public static final String ACTION_FOR_2 = "9";
public static final String ACTION_FOR_STEER = "10";

public static final String STOP = "PL0";
public static final String SPACE = " ";
public static final String FIRST = "PL0 SQ1 SM100 ";
public static final String SEQ = "SQ";
public static final String DEF_SPD = "SM100";
public static final String SPD = "SM";


public static String getNewActionGroup(String action) {
    return STOP + SPACE + SEQ + action +SPACE+ DEF_SPD;
}

public static String getNewActionGroup(String action, String speed) {
    return STOP + SPACE + SEQ +SPACE+ action + speed;
}

}

