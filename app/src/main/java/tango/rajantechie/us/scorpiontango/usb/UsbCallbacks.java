package tango.rajantechie.us.scorpiontango.usb;

/**
 * Created by rajan on 4/16/2017.
 */

public interface UsbCallbacks {

enum FAILURE {PORT_NOT_OPEN, PORT_NULL, PERMISSION_NOT_GRANTED}

void onSerialRecieved(byte[] bytes);

void onFail(FAILURE fail);

void onDeviceDetached();

void onSerialAttached();

}
