package tango.rajantechie.us.scorpiontango.firebase;

/**
 * Created by rajan on 4/21/2017.
 */

public class BotConfigs {


public BotConfigs() {
}

public BotConfigs(String lastcommand, int speed, boolean disable,long timeStamp) {
    this.lastcommand = lastcommand;
    this.speed = speed;
    this.disable = disable;
    this.timeStamp=timeStamp;
}

public long getTimeStamp() {
    return timeStamp;
}

public void setTimeStamp(long timeStamp) {
    this.timeStamp = timeStamp;
}

public BotConfigs(String lastcommand, int speed, boolean disable) {
    this.lastcommand = lastcommand;
    this.speed = speed;
    this.disable = disable;
    timeStamp = System.currentTimeMillis();
}

public boolean isDisable() {
    return disable;
}

public void setDisable(boolean disable) {
    this.disable = disable;
}

public int getSpeed() {
    return speed;
}

public void setSpeed(int speed) {
    this.speed = speed;
}

public String getLastcommand() {
    return lastcommand;
}

public void setLastcommand(String lastcommand) {
    this.lastcommand = lastcommand;
}

private boolean disable;
private int speed;
private String lastcommand;
private long timeStamp;

}
