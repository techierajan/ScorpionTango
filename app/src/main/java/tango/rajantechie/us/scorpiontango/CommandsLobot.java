package tango.rajantechie.us.scorpiontango;

/**
 * Created by rajan on 4/14/2017.
 */

public class CommandsLobot {

private static final String FORWARD = "1";
private static final String REVERSE = "2";
private static final String LEFTTURN = "3";
private static final String RIGHTTURN = "4";
private static final String ACTION_FOR_16 = "5";
private static final String ACTION_REV_16 = "6";
private static final String ACTION_FOR_17 = "7";
private static final String ACTION_REV_18 = "8";
private static final String ACTION_FOR_2 = "9";
private static final String ACTION_FOR_STEER = "10";

public static final String STOP = "PL0";
public static final String SPACE = " ";
public static final String FIRST = "PL0 SQ1 SM100 ";
public static final String SEQ = "SQ";
public static final String DEF_SPD = "SM100";
public static final String SPD = "SM";


public static String getNewActionGroup(String action) {
    return STOP + SPACE + SEQ + action + DEF_SPD;
}

public static String getNewActionGroup(String action, String speed) {
    return STOP + SPACE + SEQ + action + speed;
}

}

