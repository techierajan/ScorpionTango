package tango.rajantechie.us.scorpiontango.firebase;

/**
 * Created by rajan on 4/21/2017.
 */

public class BotConfigs {


public BotConfigs(String lastCommand, int speed, boolean disable, long timeStamp) {
    this.lastCommand = lastCommand;
    this.speed = speed;
    this.disable = disable;
    this.timeStamp = timeStamp;
}

/*public long getTimeStamp() {
    return timeStamp;
}

public void setTimeStamp(long timeStamp) {
    this.timeStamp = timeStamp;
}

public BotConfigs(String lastCommand, int speed, boolean disable) {
    this.lastCommand = lastCommand;
    this.speed = speed;
    this.disable = disable;
    timeStamp = System.currentTimeMillis();
}*/

public boolean isDisable() {
    return disable;
}

public void setDisable(boolean disable) {
    this.disable = disable;
}

/*public int getSpeed() {
    return speed;
}

public void setSpeed(int speed) {
    this.speed = speed;
}

public String getLastCommand() {
    return lastCommand;
}

public void setLastCommand(String lastCommand) {
    this.lastCommand = lastCommand;
}*/

private boolean disable;
private int speed;
private String lastCommand;
private long timeStamp;

}
